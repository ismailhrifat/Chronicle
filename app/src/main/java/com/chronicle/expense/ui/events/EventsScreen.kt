package com.chronicle.expense.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import com.chronicle.expense.ui.components.EventAction
import com.chronicle.expense.ui.components.EventMenu
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.chronicle.expense.ui.components.ScreenTitle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.Sheet
import com.chronicle.expense.data.Event
import com.chronicle.expense.data.Money
import com.chronicle.expense.ui.components.CardBox
import com.chronicle.expense.ui.components.CardShape
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.EmptyState
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.IconBtn
import com.chronicle.expense.ui.components.PrimaryButton
import com.chronicle.expense.ui.components.QuietButton
import com.chronicle.expense.ui.components.TopBar
import com.chronicle.expense.ui.components.TxRow
import com.chronicle.expense.ui.components.formatDay
import com.chronicle.expense.ui.home.moneyDisplay
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@Composable
fun EventsScreen(vm: MainViewModel, bottomPadding: Dp, onOpen: (String) -> Unit) {
    val events by vm.events.collectAsStateWithLifecycle()
    val transactions by vm.eventTransactions.collectAsStateWithLifecycle()
    val byEvent = remember(transactions) { transactions.groupBy { it.eventId } }
    val active = events.filter { !it.closed }
    val closed = events.filter { it.closed }
    var pending by remember { mutableStateOf<Pair<String, EventAction>?>(null) }
    EventDialogs(vm, events.firstOrNull { it.id == pending?.first }, pending?.second, onDismiss = { pending = null })

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenTitle("Events", Modifier.padding(start = 20.dp, end = 16.dp, top = 2.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(C.primarySoft).clickable(onClickLabel = "New event") { vm.sheet = Sheet.NewEvent },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "New event", tint = C.primary, modifier = Modifier.size(20.dp))
            }
        }

        if (events.isEmpty()) {
            Spacer(Modifier.weight(1f))
            EmptyState(
                Icons.Outlined.CalendarMonth, "No events yet",
                "Create an event for a trip or occasion, then log its expenses together.",
            )
            Spacer(Modifier.weight(1.6f).padding(bottom = bottomPadding))
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomPadding + 24.dp),
        ) {
            if (active.isNotEmpty()) {
                item("active-h") { SectionLabel("Active", active.size, C.primary) }
                items(active, key = { it.id }) { e ->
                    val list = byEvent[e.id].orEmpty()
                    EventRow(e, list.sumOf { it.amount }, list.size, onAction = { pending = e.id to it }) { onOpen(e.id) }
                }
            }
            if (closed.isNotEmpty()) {
                item("closed-h") { SectionLabel("Closed", closed.size, C.textTertiary) }
                items(closed, key = { it.id }) { e ->
                    val list = byEvent[e.id].orEmpty()
                    EventRow(e, list.sumOf { it.amount }, list.size, onAction = { pending = e.id to it }) { onOpen(e.id) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, count: Int, dot: Color) {
    Row(Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = Type.overline, color = C.textSecondary)
        Text("  $count", style = Type.overline, color = C.textTertiary)
    }
}

/** Active events read as live cards; closed ones recede into the page. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun EventRow(event: Event, total: Long, count: Int, onAction: (EventAction) -> Unit, onClick: () -> Unit) {
    val closed = event.closed
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }
    Box {
    Row(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(CardShape)
            .background(if (closed) C.surfaceAlt else C.surface)
            .border(1.dp, if (closed) Color.Transparent else C.primary.copy(alpha = 0.22f), CardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClickLabel = "Event options",
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    menu = true
                },
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (closed) C.hairline else C.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (closed) Icons.Outlined.CheckCircle else Icons.Outlined.CalendarMonth, null,
                tint = if (closed) C.textTertiary else C.primary, modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                event.name, style = Type.bodyMedium, color = if (closed) C.textSecondary else C.text,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${if (count == 1) "1 expense" else "$count expenses"} · ${formatDay(event.createdAt)}",
                style = Type.caption, color = if (closed) C.textTertiary else C.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(Money.format(total), style = Type.amount, color = if (closed) C.textSecondary else C.text)
    }
    EventMenu(menu, { menu = false }, event, onAction, offset = DpOffset(12.dp, (-8).dp))
    }
}

@Composable
fun EventDetailScreen(vm: MainViewModel, eventId: String, onBack: () -> Unit) {
    val events by vm.events.collectAsStateWithLifecycle()
    val all by vm.eventTransactions.collectAsStateWithLifecycle()
    val accounts = vm.accounts.collectAsStateWithLifecycle().value.orEmpty()
    val names = remember(accounts) { accounts.associate { it.id to it.name } }
    val event = events.firstOrNull { it.id == eventId }
    val expenses = all.filter { it.eventId == eventId }
    val total = expenses.sumOf { it.amount }

    var menu by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<EventAction?>(null) }

    Column(Modifier.fillMaxSize().background(C.bg).statusBarsPadding()) {
        TopBar(event?.name ?: "Event", onBack) {
            Box {
                IconBtn(Icons.Outlined.MoreVert, "More", { menu = true })
                EventMenu(menu, { menu = false }, event, { action = it })
            }
        }
        if (event == null) return@Column

        Box(Modifier.weight(1f)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp)) {
            item("total") {
                CardBox(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Total spent", style = Type.label, color = C.textSecondary, modifier = Modifier.weight(1f))
                            Text(
                                if (event.closed) "Closed" else "Active", style = Type.caption,
                                color = if (event.closed) C.textSecondary else C.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (event.closed) C.surfaceAlt else C.primarySoft)
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                        Text(moneyDisplay(total), style = Type.display, color = C.text, modifier = Modifier.padding(top = 6.dp))
                        Text(
                            "${if (expenses.size == 1) "1 expense" else "${expenses.size} expenses"} · started ${formatDay(event.createdAt)}",
                            style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            item("list") {
                Text("Expenses", style = Type.heading, color = C.text, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
                CardBox(Modifier.fillMaxWidth()) {
                    if (expenses.isEmpty()) {
                        EmptyState(
                            Icons.AutoMirrored.Outlined.ReceiptLong, "No expenses yet",
                            if (event.closed) "This event was closed without any expenses." else "Add the first expense for this event below.",
                        )
                    } else {
                        expenses.forEachIndexed { i, tx ->
                            if (i > 0) Hairline(inset = 68.dp)
                            TxRow(
                                tx, { names[it] ?: "Deleted account" }, onClick = { vm.sheet = Sheet.EditTx(tx) },
                                title = tx.description.ifBlank { "Expense" },
                            )
                        }
                    }
                }
            }
        }

        if (!event.closed) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 20.dp)
                    .size(56.dp)
                    .shadow(10.dp, CircleShape, ambientColor = C.primary.copy(alpha = 0.35f), spotColor = C.primary.copy(alpha = 0.45f))
                    .clip(CircleShape)
                    .background(C.primary)
                    .clickable(onClickLabel = "Add expense") { vm.sheet = Sheet.EventExpense(event.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "Add expense", tint = C.onPrimary, modifier = Modifier.size(28.dp))
            }
        }
        }
    }

    EventDialogs(vm, event, action, onDismiss = { action = null }, onDeleted = onBack)
}

/** Rename, close/reopen and delete confirmations behind the event menu. */
@Composable
private fun EventDialogs(vm: MainViewModel, event: Event?, action: EventAction?, onDismiss: () -> Unit, onDeleted: () -> Unit = {}) {
    if (event == null || action == null) return
    when (action) {
        EventAction.RENAME -> {
            var name by remember(event.id) { mutableStateOf(event.name) }
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = C.surface, tonalElevation = 0.dp, shape = RoundedCornerShape(24.dp),
                title = { Text("Rename event", style = Type.title, color = C.text) },
                text = { Field(name, { name = it.take(40) }, "Event name", capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done) },
                confirmButton = {
                    TextButton(enabled = name.isNotBlank(), onClick = { vm.renameEvent(event, name); onDismiss() }) {
                        Text("Save", style = Type.bodyMedium, color = C.primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel", style = Type.bodyMedium, color = C.textSecondary) }
                },
            )
        }
        EventAction.TOGGLE -> ConfirmDialog(
            title = if (event.closed) "Reopen ${event.name}?" else "Close ${event.name}?",
            body = if (event.closed) "It moves back to Active and can take new expenses again."
            else "It moves to Closed and stops appearing when you add event expenses. You can reopen it any time.",
            confirm = if (event.closed) "Mark as active" else "Mark as closed",
            icon = if (event.closed) Icons.Outlined.CalendarMonth else Icons.Outlined.CheckCircle,
            onConfirm = { vm.setEventClosed(event.id, !event.closed); onDismiss() },
            onDismiss = onDismiss,
        )
        EventAction.DELETE -> ConfirmDialog(
            title = "Delete ${event.name}?",
            body = "The event is removed. Its expenses stay in your history as ordinary expenses.",
            confirm = "Delete", danger = true,
            onConfirm = { onDismiss(); vm.deleteEvent(event.id); onDeleted() },
            onDismiss = onDismiss,
        )
    }
}
