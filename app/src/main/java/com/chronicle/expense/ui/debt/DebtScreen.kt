package com.chronicle.expense.ui.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.Sheet
import com.chronicle.expense.data.ACTION_INCREASE
import com.chronicle.expense.data.ACTION_INITIAL
import com.chronicle.expense.data.ACTION_REPAY
import com.chronicle.expense.data.Debt
import com.chronicle.expense.data.DebtLogic
import com.chronicle.expense.data.Money
import com.chronicle.expense.ui.components.CardBox
import com.chronicle.expense.ui.components.CardShape
import com.chronicle.expense.ui.components.Chip
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.EmptyState
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.IconBtn
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.components.QuietButton
import com.chronicle.expense.ui.components.ScreenTitle
import com.chronicle.expense.ui.components.Segmented
import com.chronicle.expense.ui.components.SmallToggle
import com.chronicle.expense.ui.components.TopBar
import com.chronicle.expense.ui.home.moneyDisplay
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** `2026-11-15` or an ISO instant → `15 Nov 2026`; anything else is shown as stored. */
internal fun prettyDate(text: String): String =
    runCatching { LocalDate.parse(text.take(10)).format(dayFormat) }.getOrDefault(text)

private fun prettyInstant(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFormat) }
        .getOrElse { prettyDate(iso) }

@Composable
internal fun debtColor(debt: Debt): Color = when {
    debt.isClosed -> C.textTertiary
    debt.isLent -> C.income
    else -> C.danger
}

@Composable
fun DebtScreen(vm: MainViewModel, bottomPadding: Dp, onOpen: (String) -> Unit) {
    val debts by vm.debts.collectAsStateWithLifecycle()
    var showClosed by rememberSaveable { mutableStateOf(false) }
    val tab = vm.debtTab
    val archivedTab = tab == 2

    val shown = debts.filter { d ->
        if (archivedTab) d.archived
        else !d.archived && (if (showClosed) d.amount == 0L else d.amount > 0) && d.isLent == (tab == 0)
    }.sortedByDescending { it.lastUpdated }
    val totalAmount = shown.sumOf { it.amount }
    val totalSettled = shown.sumOf { DebtLogic.settled(it.records) }
    val settledView = !archivedTab && showClosed
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Debt", Modifier.padding(start = 20.dp, end = 16.dp, top = 2.dp))
        Segmented(
            listOf("Lent", "Borrowed", "Archived"), tab, { vm.debtTab = it },
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = bottomPadding + 24.dp),
        ) {
            item("total") {
                CardBox(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(moneyDisplay(if (settledView) totalSettled else totalAmount), style = Type.display, color = C.text)
                            if (settledView || totalSettled > 0) {
                                Text(
                                    if (settledView) "Settled" else "Settled: ${Money.format(totalSettled)}",
                                    style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        if (!archivedTab) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Show closed", style = Type.caption, color = C.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                                SmallToggle(showClosed, { showClosed = it }, "Show closed")
                            }
                        }
                    }
                }
            }
            if (shown.isEmpty()) {
                item("empty") {
                    EmptyState(
                        Icons.Outlined.Handshake, "No records found",
                        if (archivedTab) "Archived records appear here." else "Tap + to add a new debt.",
                        Modifier.padding(top = 32.dp),
                    )
                }
            } else {
                item("gap") { Spacer(Modifier.height(12.dp)) }
                items(shown, key = { it.id }) { debt -> DebtRow(debt) { onOpen(debt.id) } }
            }
        }
    }
}

@Composable
private fun DebtRow(debt: Debt, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(C.surface)
            .border(1.dp, C.hairline, CardShape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(debt.name, style = Type.bodyMedium, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                debt.description.ifBlank { "No description" }, style = Type.caption, color = C.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Money.format(if (debt.isClosed) DebtLogic.settled(debt.records) else debt.amount),
                style = Type.amount, color = debtColor(debt),
            )
            Text(prettyDate(DebtLogic.displayDate(debt)), style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun DebtDetailScreen(vm: MainViewModel, debtId: String, onBack: () -> Unit) {
    val debts by vm.debts.collectAsStateWithLifecycle()
    val debt = debts.firstOrNull { it.id == debtId }
    var archiving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) {
        TopBar(debt?.name ?: "Debt", onBack) {
            if (debt != null) {
                IconBtn(
                    if (debt.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    if (debt.archived) "Unarchive" else "Archive", { archiving = true },
                )
                IconBtn(Icons.Outlined.DeleteOutline, "Delete", { deleting = true })
            }
        }
        if (debt == null) return@Column
        val settled = DebtLogic.settled(debt.records)
        val status = when {
            debt.archived -> "Archived"
            debt.isClosed -> "Closed"
            else -> "Active"
        }

        Box(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp)) {
                item("total") {
                    CardBox(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    moneyDisplay(if (debt.isClosed) settled else debt.amount),
                                    style = Type.display, color = C.text, modifier = Modifier.weight(1f),
                                )
                                Text(
                                    status, style = Type.caption,
                                    color = if (status == "Active") debtColor(debt) else C.textSecondary,
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(C.surfaceAlt).padding(horizontal = 9.dp, vertical = 4.dp),
                                )
                            }
                            if (debt.isClosed || settled > 0) {
                                Text(
                                    if (debt.isClosed) "Settled" else "Settled: ${Money.format(settled)}",
                                    style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            Text(
                                "Last updated ${prettyInstant(debt.lastUpdated)}",
                                style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
                item("history") {
                    Text("Transaction history", style = Type.heading, color = C.text, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
                    CardBox(Modifier.fillMaxWidth()) {
                        if (debt.records.isEmpty()) {
                            EmptyState(Icons.Outlined.Handshake, "No history", "Every lend, borrow and repayment is listed here.")
                        }
                        // Newest first; the index passed on is the record's real position.
                        debt.records.indices.reversed().forEachIndexed { row, index ->
                            val record = debt.records[index]
                            if (row > 0) Hairline(inset = 16.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(onClickLabel = "Edit record") { vm.sheet = Sheet.EditDebtRecord(debt.id, index) }
                                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(record.description, style = Type.bodyMedium, color = C.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${prettyDate(record.date)} · ${record.account}", style = Type.caption,
                                        color = C.textSecondary, modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${if (record.action == ACTION_REPAY) "−" else "+"}${Money.format(record.amount)}",
                                        style = Type.amount,
                                        color = when (record.action) {
                                            ACTION_REPAY -> C.income
                                            ACTION_INCREASE -> C.danger
                                            else -> C.text
                                        },
                                    )
                                    Text(
                                        when (record.action) { ACTION_INITIAL -> "INITIAL"; ACTION_INCREASE -> "INCREASE"; else -> "REPAY" },
                                        style = Type.overline, color = C.textTertiary, modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                IconBtn(Icons.Outlined.Edit, "Edit record", { vm.sheet = Sheet.EditDebtRecord(debt.id, index) }, tint = C.textTertiary)
                            }
                        }
                    }
                }
            }

            if (debt.amount > 0) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = 20.dp)
                        .size(56.dp)
                        .shadow(10.dp, CircleShape, ambientColor = C.primary.copy(alpha = 0.35f), spotColor = C.primary.copy(alpha = 0.45f))
                        .clip(CircleShape)
                        .background(C.primary)
                        .clickable(onClickLabel = "Add record") { vm.sheet = Sheet.AddDebtRecord(debt.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, "Add record", tint = C.onPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (archiving && debt != null) {
        ConfirmDialog(
            title = if (debt.archived) "Unarchive ${debt.name}?" else "Archive ${debt.name}?",
            body = if (debt.archived) "It will appear in your lists again." else "It will be hidden from your lists.",
            confirm = if (debt.archived) "Unarchive" else "Archive",
            icon = if (debt.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
            onConfirm = { archiving = false; vm.setDebtArchived(debt, !debt.archived); onBack() },
            onDismiss = { archiving = false },
        )
    }
    if (deleting && debt != null) {
        ConfirmDialog(
            title = "Delete ${debt.name}?",
            body = "This debt record is removed completely. Account balances and history entries it created stay as they are.",
            confirm = "Delete", danger = true,
            onConfirm = { deleting = false; vm.deleteDebt(debt); onBack() },
            onDismiss = { deleting = false },
        )
    }
}
