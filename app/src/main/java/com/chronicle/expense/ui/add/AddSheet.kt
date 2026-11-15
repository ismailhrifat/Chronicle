package com.chronicle.expense.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.Sheet
import com.chronicle.expense.data.ACCOUNT_OTHER
import com.chronicle.expense.data.Account
import com.chronicle.expense.ui.account.FeeField
import com.chronicle.expense.data.AccountType
import com.chronicle.expense.data.CATEGORY_AUTO_LOG
import com.chronicle.expense.data.Category
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.toLocalDate
import com.chronicle.expense.ui.components.AmountField
import com.chronicle.expense.ui.components.AppSheet
import com.chronicle.expense.ui.components.Chip
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.DayPickerDialog
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.FieldLabel
import com.chronicle.expense.ui.components.FieldShape
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.components.QuietButton
import com.chronicle.expense.ui.components.SaveDeleteRow
import com.chronicle.expense.ui.components.Segmented
import com.chronicle.expense.ui.components.accountIcon
import com.chronicle.expense.ui.components.formatDateTime
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.Instant
import java.time.ZoneId

@Composable
fun AddSheet(vm: MainViewModel, initialTab: Int, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(initialTab) }
    AppSheet(onDismiss) {
        Segmented(listOf("Expense", "Income", "Account"), tab, { tab = it })
        Spacer(Modifier.height(8.dp))
        when (tab) {
            0 -> key("expense") { TransactionForm(vm, TxKind.EXPENSE, null, onDismiss) { tab = 2 } }
            1 -> key("income") { TransactionForm(vm, TxKind.INCOME, null, onDismiss) { tab = 2 } }
            else -> AccountForm(vm, onDismiss)
        }
    }
}

/** Opened by the + button while on the Events tab: an expense filed under an active event. */
@Composable
fun EventExpenseSheet(vm: MainViewModel, eventId: String?, onDismiss: () -> Unit) {
    AppSheet(onDismiss, title = "Event expense") {
        TransactionForm(vm, TxKind.EXPENSE, null, onDismiss, eventMode = true, presetEventId = eventId) { vm.sheet = Sheet.Add(2) }
    }
}

@Composable
fun NewEventSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AppSheet(onDismiss, title = "New event") {
        FieldLabel("Event name")
        Field(name, { name = it.take(40) }, "e.g. Cox's Bazar trip", capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Create event", enabled = name.isNotBlank(), onClick = {
            vm.addEvent(name)
            onDismiss()
        })
    }
}

@Composable
fun EditTransactionSheet(vm: MainViewModel, tx: Transaction, onDismiss: () -> Unit) {
    var kind by remember { mutableStateOf(tx.kind) }
    val events by vm.events.collectAsStateWithLifecycle()
    val isEvent = tx.eventId != null && events.any { it.id == tx.eventId }
    AppSheet(onDismiss, title = if (tx.kind == TxKind.TRANSFER) "Edit transfer" else if (tx.kind.isDebt) tx.category else "Edit transaction") {
        if (tx.kind.isDebt) {
            Text(
                "${if (tx.kind == TxKind.DEBT_OUT) "−" else "+"}${Money.format(tx.amount)}",
                style = Type.display, color = if (tx.kind == TxKind.DEBT_IN) C.income else C.text,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Text(tx.description, style = Type.body, color = C.text)
            Text(
                "${formatDateTime(tx.timestamp)}\nThis entry belongs to a debt record. Change or remove it from the person's page in the Debt tab.",
                style = Type.caption.copy(lineHeight = Type.body.lineHeight), color = C.textSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
            QuietButton("Close", onDismiss)
        } else if (tx.kind == TxKind.TRANSFER) {
            TransferEditForm(vm, tx, onDismiss)
        } else if (isEvent) {
            TransactionForm(vm, TxKind.EXPENSE, tx, onDismiss, eventMode = true) {}
        } else {
            Segmented(listOf("Expense", "Income"), if (kind == TxKind.EXPENSE) 0 else 1, {
                kind = if (it == 0) TxKind.EXPENSE else TxKind.INCOME
            })
            Spacer(Modifier.height(8.dp))
            TransactionForm(vm, kind, tx, onDismiss) {}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionForm(
    vm: MainViewModel,
    kind: TxKind,
    existing: Transaction?,
    onDone: () -> Unit,
    eventMode: Boolean = false,
    presetEventId: String? = null,
    onCreateAccount: () -> Unit,
) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val categories by vm.categories.collectAsStateWithLifecycle()

    var amount by remember { mutableStateOf(existing?.let { Money.toInput(it.amount) } ?: "") }
    var accountId by remember {
        mutableStateOf(existing?.accountId ?: accounts.firstOrNull()?.id)
    }
    // An auto-logged entry opens with no category so the user is nudged to choose one.
    var category by remember(kind) {
        mutableStateOf(existing?.category?.takeIf { existing.kind == kind && it != CATEGORY_AUTO_LOG && it.isNotBlank() })
    }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var timestamp by remember { mutableLongStateOf(existing?.timestamp ?: System.currentTimeMillis()) }
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val noun = if (kind == TxKind.INCOME) "Source" else "Category"
    val events by vm.events.collectAsStateWithLifecycle()
    var eventId by remember { mutableStateOf(existing?.eventId ?: presetEventId) }
    // Closed events are hidden, except the one this expense already belongs to.
    val eventChoices = events.filter { !it.closed || it.id == existing?.eventId }
    val chosenEvent = eventChoices.firstOrNull { it.id == eventId } ?: eventChoices.firstOrNull().takeIf { eventMode }
    val recentTx by vm.recentTransactions.collectAsStateWithLifecycle()
    val usage = remember(recentTx, kind) { recentTx.filter { it.kind == kind }.groupingBy { it.category }.eachCount() }
    val minor = Money.parse(amount) ?: 0
    val valid = minor > 0 && accountId != null && accounts.any { it.id == accountId } &&
        (if (eventMode) chosenEvent != null else category != null)

    AmountField(amount, { amount = it }, Modifier.padding(vertical = 12.dp), accent = if (kind == TxKind.INCOME) C.income else C.text)

    FieldLabel("Account")
    if (accounts.isEmpty()) {
        Chip("Create an account first", false, onCreateAccount, leading = Icons.Rounded.Add)
    } else {
        AccountChips(accounts, accountId) { accountId = it }
    }

    Spacer(Modifier.height(18.dp))
    if (eventMode) {
        FieldLabel("Event")
        if (eventChoices.isEmpty()) {
            Chip("Create an event first", false, { vm.sheet = Sheet.NewEvent }, leading = Icons.Rounded.Add)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eventChoices.forEach { e -> Chip(e.name, e.id == chosenEvent?.id, { eventId = e.id }) }
            }
        }
    } else {
        FieldLabel(noun)
        CategoryPicker(
            noun = noun,
            options = categories.filter { it.kind == kind && it.name != CATEGORY_AUTO_LOG },
            usage = usage,
            selected = category,
            onSelect = { category = it },
        )
    }

    Spacer(Modifier.height(18.dp))
    FieldLabel("Description (optional)")
    Field(description, { description = it.take(200) }, "Add a note", imeAction = ImeAction.Done)

    Spacer(Modifier.height(18.dp))
    FieldLabel("Date")
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(FieldShape)
            .background(C.surfaceAlt)
            .clickable { pickDate = true }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CalendarToday, null, tint = C.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(formatDateTime(timestamp), style = Type.body, color = C.text)
    }

    Spacer(Modifier.height(24.dp))
    val save = {
        val tx = Transaction(
            id = existing?.id ?: "",
            kind = kind, amount = minor, accountId = accountId!!,
            category = if (eventMode) chosenEvent!!.name else category!!,
            eventId = if (eventMode) chosenEvent!!.id else null,
            description = description.trim(), timestamp = timestamp,
            autoLogged = existing?.autoLogged ?: false,
        )
        vm.saveTransaction(existing, tx)
        onDone()
    }
    if (existing != null) {
        SaveDeleteRow("Save changes", valid, save) { confirmDelete = true }
    } else {
        PrimaryButton("Save ${kind.label.lowercase()}", save, enabled = valid)
    }

    if (pickDate) {
        DayPickerDialog(
            initial = timestamp.toLocalDate(),
            onPick = { date ->
                val zone = ZoneId.systemDefault()
                val time = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime()
                timestamp = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
                pickDate = false
            },
            onDismiss = { pickDate = false },
        )
    }
    if (confirmDelete && existing != null) {
        ConfirmDialog(
            title = "Delete transaction?",
            body = "The amount will be put back on the account balance. This can't be undone.",
            confirm = "Delete", danger = true,
            onConfirm = { vm.deleteTransaction(existing); confirmDelete = false; onDone() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TransferEditForm(vm: MainViewModel, tx: Transaction, onDone: () -> Unit) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val names = accounts.associate { it.id to it.name }
    var amount by remember { mutableStateOf(Money.toInput(tx.amount)) }
    var to by remember { mutableStateOf(tx.toAccountId) }
    var fee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(tx.description) }
    var confirmDelete by remember { mutableStateOf(false) }
    val minor = Money.parse(amount) ?: 0

    Text("From ${names[tx.accountId] ?: "Deleted account"}", style = Type.body, color = C.textSecondary)
    AmountField(amount, { amount = it }, Modifier.padding(vertical = 12.dp))

    FieldLabel("To")
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // A destination that was deleted since stays visible until another one is picked.
        if (to != null && to !in names) Chip("Deleted account", true, {})
        Chip(ACCOUNT_OTHER, to == null, { to = null })
        accounts.filter { it.id != tx.accountId }.forEach { account ->
            Chip(account.name, account.id == to, { to = account.id }, leading = accountIcon(account.type))
        }
    }

    Spacer(Modifier.height(18.dp))
    FeeField(fee) { fee = it }

    Spacer(Modifier.height(18.dp))
    FieldLabel("Note (optional)")
    Field(note, { note = it.take(200) }, "Add a note", imeAction = ImeAction.Done)
    Spacer(Modifier.height(24.dp))
    SaveDeleteRow("Save changes", minor > 0, onSave = {
        vm.saveTransaction(tx, tx.copy(amount = minor, toAccountId = to, description = note.trim()))
        vm.logTransferFee(tx.accountId, to, minor, Money.parse(fee) ?: 0, tx.timestamp)
        onDone()
    }) { confirmDelete = true }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete transfer?",
            body = "Both account balances will be restored. This can't be undone.",
            confirm = "Delete", danger = true,
            onConfirm = { vm.deleteTransaction(tx); confirmDelete = false; onDone() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun AccountChips(accounts: List<Account>, selected: String?, exclude: String? = null, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        accounts.filter { it.id != exclude }.forEach { account ->
            Chip(account.name, account.id == selected, { onSelect(account.id) }, leading = accountIcon(account.type))
        }
    }
}

/**
 * Categories can only be picked from search results. A "Create" option appears only when
 * the search finds nothing, which keeps the list free of near-duplicates.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(noun: String, options: List<Category>, usage: Map<String, Int>, selected: String?, onSelect: (String?) -> Unit) {
    var query by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }

    if (selected != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clip(FieldShape)
                .background(C.primarySoft)
                .clickable { onSelect(null) }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(selected, style = Type.bodyMedium, color = C.primary, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.Close, "Change ${noun.lowercase()}", tint = C.primary, modifier = Modifier.size(18.dp))
        }
        return
    }

    Field(
        query, { query = it.take(40) }, "Search ${noun.lowercase()}",
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Done,
        leading = { Icon(Icons.Outlined.Search, null, tint = C.textTertiary, modifier = Modifier.size(18.dp)) },
    )

    val q = query.trim()
    val sorted = options.sortedBy { it.name.lowercase() }
    val collapsed = q.isEmpty() && !showAll
    val matches = when {
        q.isNotEmpty() -> sorted.filter { it.name.contains(q, ignoreCase = true) }
        showAll -> sorted
        else -> sorted.sortedByDescending { usage[it.name] ?: 0 }.take(3)
    }

    FlowRow(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        matches.forEach { c ->
            Chip(c.name, false, { onSelect(c.name); query = "" })
        }
        if (collapsed && sorted.size > 3) {
            Chip("More", false, { showAll = true }, leading = Icons.Rounded.Add)
        }
        if (matches.isEmpty() && q.isNotEmpty()) {
            Chip("Create “$q”", true, { onSelect(q); query = "" }, leading = Icons.Rounded.Add)
        }
    }
    if (matches.isEmpty() && q.isEmpty()) {
        Text("Type a name to create your first ${noun.lowercase()}.", style = Type.caption, color = C.textTertiary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountForm(vm: MainViewModel, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(AccountType.CASH) }
    var balance by remember { mutableStateOf("") }

    Spacer(Modifier.height(12.dp))
    FieldLabel("Name")
    Field(name, { name = it.take(40) }, "e.g. City Bank", capitalization = KeyboardCapitalization.Words)

    Spacer(Modifier.height(18.dp))
    FieldLabel("Type")
    TypeChips(type) { type = it }

    Spacer(Modifier.height(18.dp))
    FieldLabel("Initial balance")
    Field(
        balance, { balance = com.chronicle.expense.ui.components.sanitizeAmount(it) }, "0.00",
        keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done,
        leading = { Text(Money.SYMBOL, style = Type.body, color = C.textSecondary) },
        textStyle = Type.body.copy(fontFeatureSettings = "tnum"),
    )

    Spacer(Modifier.height(24.dp))
    PrimaryButton("Create account", enabled = name.isNotBlank(), onClick = {
        vm.addAccount(name, type, Money.parse(balance) ?: 0)
        onDone()
    })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeChips(selected: AccountType, onSelect: (AccountType) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountType.entries.forEach { t ->
            Chip(t.label, t == selected, { onSelect(t) }, leading = accountIcon(t))
        }
    }
}
