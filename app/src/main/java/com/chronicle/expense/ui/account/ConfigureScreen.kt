package com.chronicle.expense.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.chronicle.expense.Sheet
import com.chronicle.expense.data.ACCOUNT_OTHER
import com.chronicle.expense.data.Account
import com.chronicle.expense.data.AccountType
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.SmsRule
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.sms.SmsPattern
import com.chronicle.expense.ui.add.TypeChips
import com.chronicle.expense.ui.components.AccountMenu
import com.chronicle.expense.ui.components.AppSheet
import com.chronicle.expense.ui.components.TypeDeleteDialog
import com.chronicle.expense.ui.components.CardBox
import com.chronicle.expense.ui.components.Chip
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.FieldLabel
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.IconBtn
import com.chronicle.expense.ui.components.NotificationPermissions
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.components.QuietButton
import com.chronicle.expense.ui.components.Segmented
import com.chronicle.expense.ui.components.SmsPermissions
import com.chronicle.expense.ui.components.TopBar
import com.chronicle.expense.ui.components.rememberPermissions
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigureScreen(vm: MainViewModel, accountId: String, onBack: () -> Unit) {
    val saved = vm.accounts.collectAsStateWithLifecycle().value.orEmpty().firstOrNull { it.id == accountId }
    var draft by remember { mutableStateOf<Account?>(null) }
    if (draft == null && saved != null) draft = saved
    val account = draft

    var addRule by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val permissions = rememberPermissions(*SmsPermissions, *NotificationPermissions)

    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) {
        TopBar(saved?.name?.ifBlank { null } ?: "Account", onBack) {
            if (saved != null) Box {
                IconBtn(Icons.Outlined.MoreVert, "Account options", { menu = true })
                AccountMenu(
                    expanded = menu, onDismiss = { menu = false },
                    onTransfer = { vm.sheet = Sheet.Transfer(saved.id) },
                    onEditBalance = { vm.sheet = Sheet.EditBalance(saved.id) },
                    onConfigure = null,
                    onDelete = { confirmDelete = true },
                )
            }
        }
        if (account == null || saved == null) return@Column

        // Balance is owned by transactions, so it is excluded from the dirty check.
        val dirty = account.copy(balance = 0) != saved.copy(balance = 0)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Section("Details") {
                FieldLabel("Name")
                Field(
                    account.name, { draft = account.copy(name = it.take(40)) }, "Account name",
                    capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done,
                )
                Spacer(Modifier.height(18.dp))
                FieldLabel("Type")
                TypeChips(account.type) { draft = account.copy(type = it) }
            }

            Section("Card colour") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    C.swatches.forEachIndexed { i, swatch ->
                        val selected = i == account.color.mod(C.swatches.size)
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatch.fill)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    swatch.ink.copy(alpha = if (selected) 0.9f else 0.2f), CircleShape,
                                )
                                .clickable(onClickLabel = swatch.name) { draft = account.copy(color = i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) Icon(Icons.Outlined.Check, swatch.name, tint = swatch.ink, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            if (account.type == AccountType.CREDIT_CARD) {
                Section("Credit card dates") {
                    DayRow("Settlement date", "When the statement closes", account.settlementDay) {
                        draft = account.copy(settlementDay = it)
                    }
                    Hairline(Modifier.padding(vertical = 14.dp))
                    DayRow("Payment due date", "Last day to pay the bill", account.dueDay) {
                        draft = account.copy(dueDay = it)
                    }
                    Hairline(Modifier.padding(vertical = 14.dp))
                    FieldLabel("Remind me")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5, 7).forEach { days ->
                            Chip(
                                if (days == 1) "1 day before" else "$days days before",
                                account.remindDaysBefore == days,
                                { draft = account.copy(remindDaysBefore = days) },
                            )
                        }
                    }
                    Text(
                        "You also get a reminder on the day itself.",
                        style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Section("Auto-log from SMS") {
                Text(
                    "Chronicle can read transaction alerts from your bank and log them to this account automatically.",
                    style = Type.caption.copy(lineHeight = Type.body.lineHeight), color = C.textSecondary,
                )
                if (account.smsRules.isNotEmpty() && !permissions.granted) {
                    Row(
                        Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(C.primarySoft)
                            .clickable(onClick = permissions.request)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Allow SMS and notification access so rules can run.",
                            style = Type.caption, color = C.text, modifier = Modifier.weight(1f),
                        )
                        Text("Allow", style = Type.label, color = C.primary)
                    }
                }
                account.smsRules.forEachIndexed { i, rule ->
                    Hairline(Modifier.padding(top = 14.dp, bottom = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(C.surfaceAlt),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Sms, null, tint = C.textSecondary, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${rule.sender} · ${rule.kind.label}", style = Type.bodyMedium, color = C.text,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "“${rule.sample}”", style = Type.caption, color = C.textSecondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        IconBtn(Icons.Outlined.DeleteOutline, "Remove rule", {
                            val next = account.copy(smsRules = account.smsRules.filterIndexed { j, _ -> j != i })
                            draft = next
                            vm.saveAccount(next)
                        }, tint = C.textSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                QuietButton("Add a rule", { addRule = true })
            }

            Spacer(Modifier.height(28.dp))
        }

        if (dirty) {
            Hairline()
            Box(Modifier.background(C.surface).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                PrimaryButton("Save changes", enabled = account.name.isNotBlank(), onClick = {
                    val clean = account.copy(name = account.name.trim())
                    vm.saveAccount(clean)
                    draft = clean
                    onBack()
                })
            }
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (addRule && account != null) {
        RuleSheet(
            onSave = { rule ->
                val next = account.copy(smsRules = account.smsRules + rule)
                draft = next
                vm.saveAccount(next)
                addRule = false
                if (!permissions.granted) permissions.request()
            },
            onDismiss = { addRule = false },
        )
    }
    if (confirmDelete && account != null) {
        TypeDeleteDialog(
            title = "Delete ${account.name}?",
            body = "The account and every transaction recorded on it will be removed. This can't be undone.",
            onConfirm = { confirmDelete = false; vm.deleteAccount(account); onBack() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title.uppercase(), style = Type.overline, color = C.textTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
    CardBox(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayRow(title: String, hint: String, day: Int?, onChange: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.bodyMedium, color = C.text)
            Text(hint, style = Type.caption, color = C.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            day?.let { "Day $it" } ?: "Not set", style = Type.label,
            color = if (day != null) C.primary else C.textTertiary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (day != null) C.primarySoft else C.surfaceAlt)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
    if (open) {
        FlowRow(
            Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            maxItemsInEachRow = 7,
        ) {
            (1..31).forEach { d ->
                val selected = d == day
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (selected) C.primary else C.surfaceAlt)
                        .clickable { onChange(if (selected) null else d); open = false },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$d", style = Type.label, color = if (selected) C.onPrimary else C.text)
                }
            }
        }
        Text(
            "Day of every month. Tap the selected day to clear it.",
            style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun RuleSheet(onSave: (SmsRule) -> Unit, onDismiss: () -> Unit) {
    var sender by remember { mutableStateOf("") }
    var sample by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(TxKind.EXPENSE) }
    var test by remember { mutableStateOf("") }

    val pattern = remember(sample) { SmsPattern.compile(sample) }
    val valid = sender.isNotBlank() && pattern != null

    AppSheet(onDismiss, title = "New auto-log rule") {
        FieldLabel("Sender name")
        Field(
            sender, { sender = it.take(40) }, "e.g. CITY VISA",
            capitalization = KeyboardCapitalization.Characters,
        )
        Hint("Exactly as the sender appears in your messages app.")

        Spacer(Modifier.height(18.dp))
        FieldLabel("Phrase containing the amount")
        Field(sample, { sample = it.take(120) }, "e.g. BDT 9,203.00 purchased", capitalization = KeyboardCapitalization.None)
        Hint(
            when {
                sample.isBlank() -> "Copy the part of a real alert that holds the amount, with a word before or after it."
                pattern == null -> "Include the number and at least one word next to it."
                else -> "Chronicle will read the number in this position from future messages."
            },
            warn = sample.isNotBlank() && pattern == null,
        )

        Spacer(Modifier.height(18.dp))
        FieldLabel("Log as")
        val kinds = listOf(TxKind.EXPENSE, TxKind.INCOME, TxKind.TRANSFER)
        Segmented(kinds.map { it.label }, kinds.indexOf(kind), { kind = kinds[it] })
        if (kind == TxKind.TRANSFER) {
            Hint("Logged as a transfer from this account to $ACCOUNT_OTHER. Tap the notification to pick the real destination.")
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel("Test with a message (optional)")
        Field(
            test, { test = it.take(600) }, "Paste a full SMS here", singleLine = false,
            capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Default,
        )
        if (test.isNotBlank() && pattern != null) {
            val amount = pattern.extract(test)
            Hint(
                if (amount == null) "No match. Check that the phrase appears in this message."
                else if (kind == TxKind.TRANSFER) "Match — would log ${Money.format(amount)} as a transfer to $ACCOUNT_OTHER."
                else "Match — would log ${Money.format(amount)} as ${kind.label.lowercase()}.",
                warn = amount == null, good = amount != null,
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton("Save rule", enabled = valid, onClick = {
            onSave(SmsRule(sender.trim(), sample.trim(), kind))
        })
    }
}

@Composable
private fun Hint(text: String, warn: Boolean = false, good: Boolean = false) {
    Text(
        text, style = Type.caption,
        color = when {
            warn -> C.danger
            good -> C.income
            else -> C.textTertiary
        },
        modifier = Modifier.padding(top = 8.dp),
    )
}
