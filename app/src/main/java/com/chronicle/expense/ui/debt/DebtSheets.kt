package com.chronicle.expense.ui.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.data.ACCOUNT_OTHER
import com.chronicle.expense.data.ACTION_INCREASE
import com.chronicle.expense.data.ACTION_REPAY
import com.chronicle.expense.data.Account
import com.chronicle.expense.data.DEBT_BORROWED
import com.chronicle.expense.data.DEBT_LENT
import com.chronicle.expense.data.Debt
import com.chronicle.expense.data.Money
import com.chronicle.expense.ui.account.FeeField
import com.chronicle.expense.ui.components.AmountField
import com.chronicle.expense.ui.components.AppSheet
import com.chronicle.expense.ui.components.Chip
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.DayPickerDialog
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.FieldLabel
import com.chronicle.expense.ui.components.FieldShape
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.components.SaveDeleteRow
import com.chronicle.expense.ui.components.Segmented
import com.chronicle.expense.ui.components.accountIcon
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.LocalDate

/** The + button on the Debt tab: two tabs, Lent and Borrowed. */
@Composable
fun AddDebtSheet(vm: MainViewModel, initialTab: Int, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    AppSheet(onDismiss) {
        Segmented(listOf("Lent", "Borrowed"), tab, { tab = it })
        Spacer(Modifier.height(8.dp))
        key(tab) {
            AddDebtForm(vm, if (tab == 0) DEBT_LENT else DEBT_BORROWED) {
                vm.debtTab = tab
                onDismiss()
            }
        }
    }
}

@Composable
private fun AddDebtForm(vm: MainViewModel, type: String, onDone: () -> Unit) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val debts by vm.debts.collectAsStateWithLifecycle()
    val isLent = type == DEBT_LENT

    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Debt?>(null) }
    var createAnyway by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var fee by remember { mutableStateOf("") }

    val q = name.trim().lowercase()
    val searching = q.isNotEmpty() && selected == null && !createAnyway
    val suggestions = if (searching) debts.filter { it.name.lowercase().contains(q) }.take(6) else emptyList()
    val exact = if (searching) debts.firstOrNull { it.name.trim().lowercase() == q } else null
    val minor = Money.parse(amount) ?: 0

    AmountField(amount, { amount = it }, Modifier.padding(vertical = 12.dp), accent = if (isLent) C.income else C.danger)

    FieldLabel("Name")
    Field(
        name, { name = it.take(200); selected = null; createAnyway = false }, "Who is this?",
        capitalization = KeyboardCapitalization.Words,
    )
    if (suggestions.isNotEmpty()) {
        Column(Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.surfaceAlt)) {
            suggestions.forEachIndexed { i, d ->
                if (i > 0) Hairline()
                Row(
                    Modifier.fillMaxWidth().clickable { selected = d; name = d.name }.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(d.name, style = Type.bodyMedium, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(if (d.amount > 0) Money.format(d.amount) else "Closed", style = Type.caption, color = debtColor(d))
                }
            }
        }
    }
    if (exact != null) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "“${exact.name}” already exists. Pick it above or ", style = Type.caption, color = C.textSecondary,
            )
            Text("create new", style = Type.label, color = C.primary, modifier = Modifier.clickable { createAnyway = true })
        }
    }
    selected?.let { d ->
        Row(
            Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.primarySoft).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Adding to existing record (${if (d.amount > 0) "${if (d.isLent) "lent" else "borrowed"} ${Money.format(d.amount)}" else "closed"})",
                style = Type.caption, color = C.primary, modifier = Modifier.weight(1f),
            )
            Text("Undo", style = Type.label, color = C.primary, modifier = Modifier.clickable { selected = null })
        }
    }

    Spacer(Modifier.height(18.dp))
    FieldLabel("Description")
    Field(description, { description = it.take(200) }, "What was it for?", imeAction = ImeAction.Done)

    Spacer(Modifier.height(18.dp))
    FieldLabel("Account")
    DebtAccountChips(accounts, accountId) { accountId = it }

    Spacer(Modifier.height(18.dp))
    FieldLabel("Date")
    DateRow(date) { date = it }

    if (isLent && accountId != null) {
        Spacer(Modifier.height(18.dp))
        FeeField(fee) { fee = it }
    }

    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        if (selected != null) "Add to record" else "Create debt record",
        enabled = name.isNotBlank() && minor > 0 && description.isNotBlank(),
        color = if (isLent) C.income else C.danger,
        onClick = {
            vm.addDebt(
                type, name, description, accountId, minor, date.toString(),
                fee = if (isLent) Money.parse(fee) ?: 0 else 0, existing = selected,
            )
            onDone()
        },
    )
}

/** "Update record" on a person's page: repay or increase. */
@Composable
fun AddDebtRecordSheet(vm: MainViewModel, debtId: String, onDismiss: () -> Unit) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val debt = vm.debts.collectAsStateWithLifecycle().value.firstOrNull { it.id == debtId }
    if (debt == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    var action by remember { mutableStateOf(ACTION_REPAY) }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var fee by remember { mutableStateOf("") }
    val minor = Money.parse(amount) ?: 0
    // Money leaves my account when I lend more, or when I pay back what I borrowed.
    val out = debt.isLent == (action == ACTION_INCREASE)

    AppSheet(onDismiss, title = "Update record") {
        Segmented(listOf("Repay debt", "Increase debt"), if (action == ACTION_REPAY) 0 else 1, {
            action = if (it == 0) ACTION_REPAY else ACTION_INCREASE
        })
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 12.dp))

        FieldLabel("Description")
        Field(description, { description = it.take(200) }, "Reason for update…", imeAction = ImeAction.Done)

        Spacer(Modifier.height(18.dp))
        FieldLabel("Account")
        DebtAccountChips(accounts, accountId) { accountId = it }

        Spacer(Modifier.height(18.dp))
        FieldLabel("Date")
        DateRow(date) { date = it }

        if (out && accountId != null) {
            Spacer(Modifier.height(18.dp))
            FeeField(fee) { fee = it }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton("Save record", enabled = minor > 0 && description.isNotBlank(), onClick = {
            vm.addDebtRecord(debt, action, minor, description, accountId, date.toString(), if (out) Money.parse(fee) ?: 0 else 0)
            onDismiss()
        })
    }
}

@Composable
fun EditDebtRecordSheet(vm: MainViewModel, debtId: String, index: Int, onDismiss: () -> Unit) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val debt = vm.debts.collectAsStateWithLifecycle().value.firstOrNull { it.id == debtId }
    val record = debt?.records?.getOrNull(index)
    if (debt == null || record == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    // Imported records carry only an account label; show the matching account as chosen.
    val matched = record.accountId ?: accounts.firstOrNull { it.name.equals(record.account, ignoreCase = true) }?.id
    var accountId by remember { mutableStateOf(matched?.takeIf { id -> accounts.any { it.id == id } }) }
    var touched by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(Money.toInput(record.amount)) }
    var description by remember { mutableStateOf(record.description) }
    var date by remember { mutableStateOf(runCatching { LocalDate.parse(record.date) }.getOrDefault(LocalDate.now())) }
    var confirmDelete by remember { mutableStateOf(false) }
    val minor = Money.parse(amount) ?: 0
    // A label with no matching account (e.g. the Debt app's "bKash") stays until another is picked.
    val keptLabel = record.account.takeIf { accountId == null && !touched && it.isNotBlank() && it != ACCOUNT_OTHER }

    AppSheet(onDismiss, title = "Edit record") {
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 12.dp))

        FieldLabel("Description")
        Field(description, { description = it.take(200) }, "Description", imeAction = ImeAction.Done)

        Spacer(Modifier.height(18.dp))
        FieldLabel("Account")
        DebtAccountChips(accounts, accountId, keptLabel) { accountId = it; touched = true }

        Spacer(Modifier.height(18.dp))
        FieldLabel("Date")
        DateRow(date) { date = it }

        Spacer(Modifier.height(24.dp))
        SaveDeleteRow("Save changes", minor > 0 && description.isNotBlank(), onSave = {
            val label = keptLabel ?: accounts.firstOrNull { it.id == accountId }?.name ?: ACCOUNT_OTHER
            vm.editDebtRecord(debt, index, minor, description, accountId, label, date.toString())
            onDismiss()
        }) { confirmDelete = true }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Remove this transaction?",
            body = if (record.txId != null) "The debt total is recalculated and the amount is put back on the account balance."
            else "The debt total is recalculated.",
            confirm = "Delete", danger = true,
            onConfirm = { confirmDelete = false; vm.deleteDebtRecord(debt, index); onDismiss() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** My accounts plus "Other", which records the movement without touching any balance. */
@Composable
private fun DebtAccountChips(accounts: List<Account>, selected: String?, keptLabel: String? = null, onSelect: (String?) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (keptLabel != null) Chip(keptLabel, true, {})
        accounts.forEach { account ->
            Chip(account.name, account.id == selected, { onSelect(account.id) }, leading = accountIcon(account.type))
        }
        Chip(ACCOUNT_OTHER, selected == null && keptLabel == null, { onSelect(null) })
    }
}

@Composable
private fun DateRow(date: LocalDate, onChange: (LocalDate) -> Unit) {
    var pick by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(FieldShape).background(C.surfaceAlt).clickable { pick = true }.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CalendarToday, null, tint = C.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(prettyDate(date.toString()), style = Type.body, color = C.text)
    }
    if (pick) DayPickerDialog(initial = date, onPick = { onChange(it); pick = false }, onDismiss = { pick = false })
}
