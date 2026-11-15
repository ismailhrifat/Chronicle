package com.chronicle.expense.ui.account

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.chronicle.expense.ui.components.sanitizeAmount
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.data.Money
import com.chronicle.expense.ui.add.AccountChips
import com.chronicle.expense.ui.components.AmountField
import com.chronicle.expense.ui.components.AppSheet
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.FieldLabel
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@Composable
fun TransferSheet(vm: MainViewModel, accountId: String, onDismiss: () -> Unit) {
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    var from by remember { mutableStateOf(accountId) }
    var to by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    val minor = Money.parse(amount) ?: 0
    val source = accounts.firstOrNull { it.id == from }

    AppSheet(onDismiss, title = "Transfer") {
        AmountField(amount, { amount = it }, Modifier.padding(bottom = 12.dp))

        FieldLabel("From")
        AccountChips(accounts, from) { from = it; if (to == it) to = null }
        if (source != null) {
            Text(
                "Available ${Money.format(source.balance)}", style = Type.caption, color = C.textTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel("To")
        if (accounts.size < 2) {
            Text("Add another account to transfer between them.", style = Type.body, color = C.textSecondary)
        } else {
            AccountChips(accounts, to, exclude = from) { to = it }
        }

        Spacer(Modifier.height(18.dp))
        FeeField(fee) { fee = it }

        Spacer(Modifier.height(18.dp))
        FieldLabel("Note (optional)")
        Field(note, { note = it.take(200) }, "Add a note", imeAction = ImeAction.Done)

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            if (minor > 0) "Transfer ${Money.format(minor)}" else "Transfer",
            enabled = minor > 0 && to != null && source != null,
            onClick = {
                vm.transfer(from, to!!, minor, note, Money.parse(fee) ?: 0)
                onDismiss()
            },
        )
    }
}

/** Optional fee of a transfer; logged as a separate "Transaction Fee" expense on the source account. */
@Composable
fun FeeField(value: String, onChange: (String) -> Unit) {
    FieldLabel("Transaction fee (optional)")
    Field(
        value, { onChange(sanitizeAmount(it)) }, "0.00",
        keyboardType = KeyboardType.Decimal,
        leading = { Text(Money.SYMBOL, style = Type.body, color = C.textSecondary) },
        textStyle = Type.body.copy(fontFeatureSettings = "tnum"),
    )
    if ((Money.parse(value) ?: 0) > 0) {
        Text(
            "Logged as a Transaction Fee expense on the paying account.",
            style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun EditBalanceSheet(vm: MainViewModel, accountId: String, onDismiss: () -> Unit) {
    val account = vm.accounts.collectAsStateWithLifecycle().value.orEmpty().firstOrNull { it.id == accountId }
    if (account == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onDismiss() }
        return
    }
    var amount by remember { mutableStateOf(Money.toInput(account.balance.coerceAtLeast(0))) }
    val target = Money.parse(amount)
    val diff = (target ?: account.balance) - account.balance

    AppSheet(onDismiss, title = "Edit balance") {
        Text(
            "${account.name} · currently ${Money.format(account.balance)}",
            style = Type.body, color = C.textSecondary,
        )
        AmountField(amount, { amount = it }, Modifier.padding(vertical = 12.dp))
        Text(
            when {
                target == null -> "Enter the balance this account should show."
                diff < 0 -> "The difference of ${Money.format(-diff)} will be logged as an expense under Other."
                diff > 0 -> "The difference of ${Money.format(diff)} will be logged as income from Other."
                else -> "No change to log."
            },
            style = Type.caption.copy(lineHeight = Type.body.lineHeight), color = C.textSecondary,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Update balance", enabled = target != null && diff != 0L, onClick = {
            vm.setBalance(account, target!!)
            onDismiss()
        })
    }
}
