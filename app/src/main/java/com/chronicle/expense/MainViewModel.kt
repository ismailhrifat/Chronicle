package com.chronicle.expense

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronicle.expense.data.Account
import com.chronicle.expense.data.AccountType
import com.chronicle.expense.data.Auth
import com.chronicle.expense.data.ACCOUNT_OTHER
import com.chronicle.expense.data.ACTION_INCREASE
import com.chronicle.expense.data.ACTION_INITIAL
import com.chronicle.expense.data.ACTION_REPAY
import com.chronicle.expense.data.DEBT_LENT
import com.chronicle.expense.data.Debt
import com.chronicle.expense.data.DebtLogic
import com.chronicle.expense.data.DebtRecord
import com.chronicle.expense.data.CATEGORY_OTHER
import com.chronicle.expense.data.CATEGORY_TRANSACTION_FEE
import com.chronicle.expense.data.Category
import com.chronicle.expense.data.Event
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.Repo
import com.chronicle.expense.data.Settings
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.notify.ReminderWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

sealed interface Sheet {
    data class Add(val tab: Int = 0) : Sheet
    data class EditTx(val tx: Transaction) : Sheet
    data class Transfer(val accountId: String) : Sheet
    data class EditBalance(val accountId: String) : Sheet
    data class EventExpense(val eventId: String? = null) : Sheet
    data object NewEvent : Sheet
    data class AddDebt(val tab: Int = 0) : Sheet
    data class AddDebtRecord(val debtId: String) : Sheet
    data class EditDebtRecord(val debtId: String, val index: Int) : Sheet
}

sealed interface DateRange {
    val label: String

    /** Start inclusive, end exclusive, epoch millis. */
    fun bounds(): Pair<Long?, Long?>

    data object AllTime : DateRange {
        override val label = "All time"
        override fun bounds() = null to null
    }

    data object Today : DateRange {
        override val label = "Today"
        override fun bounds() = LocalDate.now().let { it.millis() to it.plusDays(1).millis() }
    }

    data object ThisWeek : DateRange {
        override val label = "This week"
        override fun bounds(): Pair<Long?, Long?> {
            val start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
            return start.millis() to start.plusDays(7).millis()
        }
    }

    data object ThisMonth : DateRange {
        override val label = "This month"
        override fun bounds() = YearMonth.now().let { it.atDay(1).millis() to it.plusMonths(1).atDay(1).millis() }
    }

    data class Month(val month: YearMonth) : DateRange {
        override val label: String = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        override fun bounds() = month.atDay(1).millis() to month.plusMonths(1).atDay(1).millis()
    }

    data class Day(val date: LocalDate) : DateRange {
        override val label: String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        override fun bounds() = date.millis() to date.plusDays(1).millis()
    }

    data class Custom(val from: LocalDate, val to: LocalDate) : DateRange {
        override val label: String = DateTimeFormatter.ofPattern("d MMM").let { "${from.format(it)} – ${to.format(it)}" }
        override fun bounds() = from.millis() to to.plusDays(1).millis()
    }
}

fun LocalDate.millis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

data class TxFilter(
    val kinds: Set<TxKind> = emptySet(),
    val accountIds: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val range: DateRange = DateRange.AllTime,
    val min: Long? = null,
    val max: Long? = null,
    val query: String = "",
) {
    /** Number of active filters, not counting the search text. */
    val count: Int
        get() = listOf(
            kinds.isNotEmpty(), accountIds.isNotEmpty(), categories.isNotEmpty(),
            range != DateRange.AllTime, min != null || max != null,
        ).count { it }

    fun matches(tx: Transaction, accountName: (String) -> String): Boolean {
        if (kinds.isNotEmpty() && tx.kind !in kinds) return false
        if (accountIds.isNotEmpty() && tx.accountId !in accountIds && tx.toAccountId !in accountIds) return false
        if (categories.isNotEmpty() && tx.category !in categories) return false
        if (min != null && tx.amount < min) return false
        if (max != null && tx.amount > max) return false
        val q = query.trim()
        if (q.isNotEmpty()) {
            val haystack = "${tx.category} ${tx.description} ${accountName(tx.accountId)} ${Money.format(tx.amount, false)}"
            if (!haystack.contains(q, ignoreCase = true)) return false
        }
        return true
    }
}

data class TxListState(
    val items: List<Transaction> = emptyList(),
    val loading: Boolean = true,
    val hasMore: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val auth = FirebaseAuth.getInstance()
    private val settings = Settings(app)

    val user: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser)

    /** Debug builds only: in-memory sample data so screens can be reviewed without an account. */
    private val demo = MutableStateFlow(false)

    val signedIn: StateFlow<Boolean> = combine(user, demo) { u, d -> u != null || d }
        .stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser != null)

    private val repoFlow: StateFlow<Repo?> = combine(user.map { it?.uid }, demo) { uid, d -> if (d) "demo" else uid }
        .distinctUntilChanged()
        .map { uid -> if (uid == "demo") com.chronicle.expense.data.DemoRepo() else uid?.let { Repo(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, auth.currentUser?.uid?.let { Repo(it) })

    fun enableDemo() {
        demo.value = true
    }

    private val repo get() = repoFlow.value

    val themeMode: StateFlow<String> = settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    /** Null while the first snapshot is loading. */
    val accounts: StateFlow<List<Account>?> = repoFlow
        .flatMapLatest { r -> r?.accounts()?.map<List<Account>, List<Account>?> { list ->
                list.sortedWith(compareBy({ it.order ?: Long.MAX_VALUE }, { it.createdAt }))
            }?.onStart { emit(null) } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val categories: StateFlow<List<Category>> = repoFlow
        .flatMapLatest { r -> r?.categories() ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Everything since the first of the current month; feeds "Top spending". */
    val monthTransactions: StateFlow<List<Transaction>> = repoFlow
        .flatMapLatest { r -> r?.transactions(from = YearMonth.now().atDay(1).millis()) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = repoFlow
        .flatMapLatest { r -> r?.transactions(limit = 60) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val events: StateFlow<List<Event>> = repoFlow
        .flatMapLatest { r -> r?.events() ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** All event expenses, newest first. */
    val eventTransactions: StateFlow<List<Transaction>> = repoFlow
        .flatMapLatest { r -> r?.eventTransactions()?.map { list -> list.sortedByDescending { it.timestamp } } ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val debts: StateFlow<List<Debt>> = repoFlow
        .flatMapLatest { r -> r?.debts() ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 0 Lent, 1 Borrowed, 2 Archived. */
    var debtTab by mutableStateOf(0)

    val filter = MutableStateFlow(TxFilter())
    private val pageSize = MutableStateFlow(PAGE)

    val transactionList: StateFlow<TxListState> = combine(
        repoFlow, filter.map { it.range }.distinctUntilChanged(), pageSize,
    ) { r, range, limit -> Triple(r, range, limit) }
        .flatMapLatest { (r, range, limit) ->
            if (r == null) return@flatMapLatest flowOf(TxListState(loading = false))
            val (from, to) = range.bounds()
            combine(r.transactions(from, to, limit), filter, accounts) { raw, f, accs ->
                val names = accs.orEmpty().associate { it.id to it.name }
                TxListState(
                    items = raw.filter { f.matches(it) { id -> names[id] ?: "" } },
                    loading = false,
                    hasMore = raw.size >= limit,
                )
            }.onStart { emit(TxListState(loading = true)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxListState())

    var sheet by mutableStateOf<Sheet?>(null)
    var signingIn by mutableStateOf(false)
        private set
    var signInError by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            // Seed once per sign-in, after the first category snapshot arrives.
            repoFlow.flatMapLatest { r ->
                if (r == null) flowOf(null) else r.categories().map { r to it }
            }.filterNotNull().distinctUntilChanged { a, b -> a.first === b.first }.collect { (r, list) ->
                r.seedCategories(list, firstRun = list.isEmpty())
            }
        }
    }

    // ---- Auth & settings --------------------------------------------------------------

    fun signIn(activity: Activity) {
        if (signingIn) return
        signingIn = true
        signInError = null
        viewModelScope.launch {
            signInError = Auth.signInWithGoogle(activity)
            signingIn = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sheet = null
            filter.value = TxFilter()
            Auth.signOut(getApplication())
        }
    }

    fun setTheme(mode: String) {
        viewModelScope.launch { settings.setTheme(mode) }
    }

    // ---- Writes -----------------------------------------------------------------------

    private fun accountIds() = accounts.value.orEmpty().map { it.id }.toSet()

    fun saveTransaction(old: Transaction?, new: Transaction) {
        val r = repo ?: return
        if (new.kind != TxKind.TRANSFER && !new.kind.isDebt && new.eventId == null) r.ensureCategory(new.category, new.kind)
        r.saveTransaction(old, new, accountIds())
    }

    fun deleteTransaction(tx: Transaction) {
        repo?.deleteTransaction(tx, accountIds())
    }

    fun addAccount(name: String, type: AccountType, initialBalance: Long) {
        val count = accounts.value.orEmpty().size
        repo?.addAccount(name, type, initialBalance, color = count % 8)
    }

    fun reorderAccounts(ids: List<String>) {
        repo?.reorderAccounts(ids)
    }

    fun transfer(fromId: String, toId: String, amount: Long, note: String, fee: Long = 0) {
        val now = System.currentTimeMillis()
        saveTransaction(
            null,
            Transaction(
                kind = TxKind.TRANSFER, amount = amount, accountId = fromId, toAccountId = toId,
                category = "Transfer", description = note.trim(), timestamp = now,
            )
        )
        logTransferFee(fromId, toId, amount, fee, now)
    }

    /** Records the fee of a transfer as its own expense on the source account. */
    fun logTransferFee(fromId: String, toId: String?, amount: Long, fee: Long, timestamp: Long) {
        if (fee <= 0) return
        val names = accounts.value.orEmpty().associate { it.id to it.name }
        val from = names[fromId] ?: "Deleted account"
        val to = if (toId == null) ACCOUNT_OTHER else names[toId] ?: "Deleted account"
        saveTransaction(
            null,
            Transaction(
                kind = TxKind.EXPENSE, amount = fee, accountId = fromId,
                category = CATEGORY_TRANSACTION_FEE,
                description = "${Money.format(amount)} transfer from $from to $to",
                timestamp = timestamp + 1,
            )
        )
    }

    /** Logs the difference as an "Other" expense or income so history explains the change. */
    fun setBalance(account: Account, newBalance: Long) {
        val diff = newBalance - account.balance
        if (diff == 0L) return
        saveTransaction(
            null,
            Transaction(
                kind = if (diff < 0) TxKind.EXPENSE else TxKind.INCOME,
                amount = kotlin.math.abs(diff),
                accountId = account.id,
                category = CATEGORY_OTHER,
                description = "Balance adjustment (${Money.format(account.balance)} → ${Money.format(newBalance)})",
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    fun saveAccount(account: Account) {
        repo?.saveAccountSettings(account)
        if (account.isCard) ReminderWorker.runNow(getApplication())
    }

    fun deleteAccount(account: Account) {
        val r = repo ?: return
        val others = accountIds() - account.id
        viewModelScope.launch { r.deleteAccount(account, others) }
    }

    fun addCategory(name: String, kind: TxKind) {
        repo?.ensureCategory(name, kind)
    }

    fun deleteCategory(category: Category) {
        repo?.deleteCategory(category)
    }

    fun renameCategory(category: Category, newName: String) {
        val r = repo ?: return
        viewModelScope.launch { r.renameCategory(category, newName) }
    }

    fun addEvent(name: String) {
        repo?.addEvent(name)
    }

    fun setEventClosed(id: String, closed: Boolean) {
        repo?.setEventClosed(id, closed)
    }

    fun deleteEvent(id: String) {
        repo?.deleteEvent(id)
    }

    fun renameEvent(event: Event, newName: String) {
        val r = repo ?: return
        viewModelScope.launch { r.renameEvent(event, newName) }
    }

    // ---- Debts ------------------------------------------------------------------------

    /**
     * Mirrors a debt record into history so the account balance moves with it. Returns the
     * history entry id, or null when the record is not tied to a Chronicle account.
     */
    private fun postDebtTx(old: Transaction?, name: String, record: DebtRecord): String? {
        val r = repo ?: return null
        val accountId = record.accountId?.takeIf { it in accountIds() }
        val out = record.out
        if (accountId == null || out == null) {
            if (old != null) r.deleteTransaction(old, accountIds())
            return null
        }
        val tx = Transaction(
            kind = if (out) TxKind.DEBT_OUT else TxKind.DEBT_IN,
            amount = record.amount, accountId = accountId,
            category = when {
                record.action == ACTION_REPAY -> if (out) "Debt repaid" else "Debt collected"
                out -> "Lent"
                else -> "Borrowed"
            },
            description = "$name · ${record.description}",
            timestamp = debtTimestamp(record.date),
        )
        return r.saveTransaction(old, tx, accountIds())
    }

    /** The history entry a linked record created, rebuilt from the record itself. */
    private fun debtTxOf(record: DebtRecord): Transaction? {
        val id = record.txId ?: return null
        val accountId = record.accountId ?: return null
        val out = record.out ?: return null
        return Transaction(id = id, kind = if (out) TxKind.DEBT_OUT else TxKind.DEBT_IN, amount = record.amount, accountId = accountId)
    }

    private fun debtTimestamp(date: String): Long {
        val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return System.currentTimeMillis()
        return if (day == LocalDate.now()) System.currentTimeMillis()
        else day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun accountLabel(accountId: String?): String =
        accounts.value.orEmpty().firstOrNull { it.id == accountId }?.name ?: ACCOUNT_OTHER

    private fun debtFee(record: DebtRecord, name: String, verb: String, fee: Long) {
        val accountId = record.accountId ?: return
        if (fee <= 0 || record.out != true) return
        saveTransaction(
            null,
            Transaction(
                kind = TxKind.EXPENSE, amount = fee, accountId = accountId,
                category = CATEGORY_TRANSACTION_FEE,
                description = "${Money.format(record.amount)} $verb to $name",
                timestamp = debtTimestamp(record.date) + 1,
            )
        )
    }

    /**
     * The + button's Lent / Borrowed form. With [existing] the amount is added to that
     * person's record instead of creating a second one. [accountId] null means "Other".
     */
    fun addDebt(
        type: String, name: String, description: String, accountId: String?, amount: Long, date: String,
        fee: Long, existing: Debt?,
    ) {
        val r = repo ?: return
        val out = type == DEBT_LENT
        val action = when {
            existing == null -> ACTION_INITIAL
            existing.type == type -> ACTION_INCREASE
            else -> ACTION_REPAY
        }
        val person = existing?.name ?: name.trim()
        var record = DebtRecord(
            action = action, amount = amount,
            description = description.trim().ifBlank {
                when (action) { ACTION_INITIAL -> "Initial Debt"; ACTION_INCREASE -> "Additional Debt"; else -> "Repayment" }
            },
            account = accountLabel(accountId), date = date, timestamp = DebtLogic.nowIso(),
            accountId = accountId?.takeIf { it in accountIds() }, out = out,
        )
        record = record.copy(txId = postDebtTx(null, person, record))
        r.saveDebt(existing?.let { DebtLogic.append(it, record) } ?: DebtLogic.create(type, person, description.trim(), record))
        debtFee(record, person, "lend", fee)
    }

    /** "Update record" on a person's page: a repayment or an increase. */
    fun addDebtRecord(debt: Debt, action: String, amount: Long, description: String, accountId: String?, date: String, fee: Long) {
        val r = repo ?: return
        val out = debt.isLent == (action == ACTION_INCREASE)
        var record = DebtRecord(
            action = action, amount = amount,
            description = description.trim().ifBlank { if (action == ACTION_REPAY) "Repayment" else "Additional Debt" },
            account = accountLabel(accountId), date = date, timestamp = DebtLogic.nowIso(),
            accountId = accountId?.takeIf { it in accountIds() }, out = out,
        )
        record = record.copy(txId = postDebtTx(null, debt.name, record))
        r.saveDebt(DebtLogic.append(debt, record))
        debtFee(record, debt.name, if (action == ACTION_REPAY) "repay" else "lend", fee)
    }

    /** Records imported from the Debt app ([DebtRecord.out] null) never touch account balances. */
    fun editDebtRecord(debt: Debt, index: Int, amount: Long, description: String, accountId: String?, account: String, date: String) {
        val r = repo ?: return
        val old = debt.records.getOrNull(index) ?: return
        var record = old.copy(amount = amount, description = description.trim(), account = account, date = date)
        if (old.out != null) {
            record = record.copy(accountId = accountId?.takeIf { it in accountIds() }, account = accountLabel(accountId))
            record = record.copy(txId = postDebtTx(debtTxOf(old), debt.name, record))
            if (record.txId == null) record = record.copy(accountId = null)
        }
        r.saveDebt(DebtLogic.recalculate(debt, debt.records.toMutableList().also { it[index] = record }))
    }

    fun deleteDebtRecord(debt: Debt, index: Int) {
        val r = repo ?: return
        val old = debt.records.getOrNull(index) ?: return
        debtTxOf(old)?.let { r.deleteTransaction(it, accountIds()) }
        r.saveDebt(DebtLogic.recalculate(debt, debt.records.filterIndexed { i, _ -> i != index }))
    }

    fun setDebtArchived(debt: Debt, archived: Boolean) {
        repo?.setDebtArchived(debt.id, archived)
    }

    fun deleteDebt(debt: Debt) {
        repo?.deleteDebt(debt.id)
    }

    // ---- Transactions screen ----------------------------------------------------------

    fun loadMore() {
        pageSize.value += PAGE
    }

    fun resetPaging() {
        pageSize.value = PAGE
    }

    /** Opens the editor for a transaction id that arrived from a notification. */
    fun openTransaction(id: String) {
        viewModelScope.launch {
            val r = repoFlow.filterNotNull().first()
            r.transaction(id)?.let { sheet = Sheet.EditTx(it) }
        }
    }

    private companion object {
        const val PAGE = 100L
    }
}
