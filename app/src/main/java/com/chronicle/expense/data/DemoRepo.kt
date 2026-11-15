package com.chronicle.expense.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory stand-in used only by debug builds launched with `--ez demo true`. */
class DemoRepo : Repo("demo") {
    private val now = System.currentTimeMillis()
    private fun ago(hours: Double) = now - (hours * 3_600_000).toLong()

    private val accountsState = MutableStateFlow(
        listOf(
            Account("cash", "Cash", AccountType.CASH, 1245000, 1, 1),
            Account("city", "City Bank", AccountType.BANK, 18432075, 2, 2),
            Account(
                "visa", "Platinum Card", AccountType.CREDIT_CARD, 8782975, 3, 3, settlementDay = 20, dueDay = 5,
                smsRules = listOf(SmsRule("CITY VISA", "BDT 9,203.00 purchased", TxKind.EXPENSE)),
            ),
            Account("fdr", "Fixed Deposit", AccountType.FDR, 50000000, 0, 4),
            Account("dps", "Monthly DPS", AccountType.DPS, 12000000, 6, 5),
        )
    )
    private val txState = MutableStateFlow(
        listOf(
            Transaction("t1", TxKind.EXPENSE, 920300, "visa", null, CATEGORY_AUTO_LOG, "BDT 9,203.00 purchased at bdtickets", ago(0.4), true),
            Transaction("t2", TxKind.EXPENSE, 45000, "cash", null, "Food", "Lunch with team", ago(3.0)),
            Transaction("t3", TxKind.INCOME, 8500000, "city", null, "Salary", "November", ago(26.0)),
            Transaction("t4", TxKind.TRANSFER, 1000000, "city", "cash", "Transfer", "", ago(27.0)),
            Transaction("t5", TxKind.EXPENSE, 320000, "city", null, "Groceries", "Weekly bazar", ago(30.0)),
            Transaction("t6", TxKind.EXPENSE, 2500000, "city", null, "Rent", "", ago(80.0)),
            Transaction("t7", TxKind.EXPENSE, 185000, "cash", null, "Transport", "Uber", ago(100.0)),
            Transaction("t8", TxKind.EXPENSE, 410000, "visa", null, "Shopping", "Shoes", ago(150.0)),
            Transaction("t9", TxKind.EXPENSE, 126000, "city", null, "Utilities", "Internet bill", ago(200.0)),
            Transaction("t10", TxKind.INCOME, 1200000, "city", null, "Freelance", "Logo project", ago(220.0)),
        )
    )
    private val categoriesState = MutableStateFlow(
        (listOf("Food", "Groceries", "Transport", "Rent", "Utilities", "Shopping", "Health", "Entertainment", "Other")
            .map { Category(Category.idFor(it, TxKind.EXPENSE), it, TxKind.EXPENSE) }) +
            (listOf("Salary", "Freelance", "Gift", "Interest", "Other")
                .map { Category(Category.idFor(it, TxKind.INCOME), it, TxKind.INCOME) })
    )

    override fun accounts(): Flow<List<Account>> = accountsState
    override fun categories(): Flow<List<Category>> = categoriesState
    override fun transactions(from: Long?, to: Long?, limit: Long?): Flow<List<Transaction>> = txState.map { list ->
        list.filter { (from == null || it.timestamp >= from) && (to == null || it.timestamp < to) }
            .sortedByDescending { it.timestamp }
            .let { if (limit != null) it.take(limit.toInt()) else it }
    }

    override suspend fun accountsOnce() = accountsState.value
    override suspend fun transaction(id: String) = txState.value.firstOrNull { it.id == id }

    private fun applyChanges(changes: Map<String, Long>) = accountsState.update { list ->
        list.map { a -> changes[a.id]?.let { a.copy(balance = a.balance + it) } ?: a }
    }

    override fun addAccount(name: String, type: AccountType, initialBalance: Long, color: Int): String {
        val id = "a${System.nanoTime()}"
        accountsState.update { it + Account(id, name.trim(), type, initialBalance, color, System.currentTimeMillis()) }
        return id
    }

    override fun reorderAccounts(ids: List<String>) = accountsState.update { list ->
        list.map { it.copy(order = ids.indexOf(it.id).toLong()) }
    }

    override fun saveAccountSettings(account: Account) = accountsState.update { list ->
        list.map { if (it.id == account.id) account.copy(balance = it.balance) else it }
    }

    override suspend fun deleteAccount(account: Account, otherAccountIds: Set<String>) {
        txState.update { list -> list.filter { it.accountId != account.id && it.toAccountId != account.id } }
        accountsState.update { list -> list.filter { it.id != account.id } }
    }

    override fun saveTransaction(old: Transaction?, new: Transaction, knownAccounts: Set<String>, id: String?): String {
        val txId = old?.id ?: id ?: "t${System.nanoTime()}"
        txState.update { list -> list.filter { it.id != txId } + new.copy(id = txId) }
        applyChanges(balanceChanges(old, new))
        return txId
    }

    override fun deleteTransaction(old: Transaction, knownAccounts: Set<String>) {
        txState.update { list -> list.filter { it.id != old.id } }
        applyChanges(balanceChanges(old, null))
    }

    override fun ensureCategory(name: String, kind: TxKind) {
        val id = Category.idFor(name, kind)
        categoriesState.update { list -> if (list.any { it.id == id }) list else list + Category(id, name.trim(), kind) }
    }

    override fun deleteCategory(category: Category) = categoriesState.update { list -> list.filter { it.id != category.id } }

    override suspend fun renameCategory(category: Category, newName: String) {
        val clean = newName.trim()
        categoriesState.update { list -> list.filter { it.id != category.id } }
        ensureCategory(clean, category.kind)
        txState.update { list -> list.map { if (it.category == category.name && it.kind == category.kind) it.copy(category = clean) else it } }
    }

    private val eventsState = MutableStateFlow(
        listOf(Event("e1", "Cox's Bazar trip", false, now - 86_400_000L * 3), Event("e2", "Eid shopping", true, now - 86_400_000L * 40))
    )

    override fun events(): Flow<List<Event>> = eventsState.map { list -> list.sortedByDescending { it.createdAt } }
    override fun eventTransactions(): Flow<List<Transaction>> = txState.map { list -> list.filter { it.eventId != null } }

    override fun addEvent(name: String): String {
        val id = "e${System.nanoTime()}"
        eventsState.update { it + Event(id, name.trim(), false, System.currentTimeMillis()) }
        return id
    }

    override fun setEventClosed(id: String, closed: Boolean) =
        eventsState.update { list -> list.map { if (it.id == id) it.copy(closed = closed) else it } }

    override fun deleteEvent(id: String) = eventsState.update { list -> list.filter { it.id != id } }

    override suspend fun renameEvent(event: Event, newName: String) {
        eventsState.update { list -> list.map { if (it.id == event.id) it.copy(name = newName.trim()) else it } }
        txState.update { list -> list.map { if (it.eventId == event.id) it.copy(category = newName.trim()) else it } }
    }

    override fun seedCategories(existing: List<Category>, firstRun: Boolean) = Unit

    private val debtsState = MutableStateFlow(
        listOf(
            DebtLogic.create(DEBT_LENT, "Rahim", "Laptop repair", DebtRecord(ACTION_INITIAL, 500000, "Laptop repair", "Cash", "2026-11-02", "", out = true)).copy(id = "d1"),
            DebtLogic.create(DEBT_BORROWED, "Karim", "Trip advance", DebtRecord(ACTION_INITIAL, 250000, "Trip advance", "City Bank", "2026-10-20", "", out = false)).copy(id = "d2"),
        )
    )

    override fun debts(): Flow<List<Debt>> = debtsState
    override fun saveDebt(debt: Debt): String {
        val id = debt.id.ifBlank { "d${System.nanoTime()}" }
        debtsState.update { list -> list.filter { it.id != id } + debt.copy(id = id) }
        return id
    }
    override fun setDebtArchived(id: String, archived: Boolean) =
        debtsState.update { list -> list.map { if (it.id == id) it.copy(archived = archived) else it } }
    override fun deleteDebt(id: String) = debtsState.update { list -> list.filter { it.id != id } }
}
