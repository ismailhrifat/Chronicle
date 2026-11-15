package com.chronicle.expense.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.Sheet
import com.chronicle.expense.data.Account
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.ui.components.AccountMenu
import com.chronicle.expense.ui.components.CardBox
import com.chronicle.expense.ui.components.TypeDeleteDialog
import com.chronicle.expense.ui.components.EmptyState
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.SectionHeader
import com.chronicle.expense.ui.components.TxRow
import com.chronicle.expense.ui.components.accountIcon
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(vm: MainViewModel, bottomPadding: androidx.compose.ui.unit.Dp, onSeeAll: () -> Unit, onConfigure: (String) -> Unit) {
    val user by vm.user.collectAsStateWithLifecycle()
    val accountsOrNull by vm.accounts.collectAsStateWithLifecycle()
    val recentAll by vm.recentTransactions.collectAsStateWithLifecycle()
    val monthAll by vm.monthTransactions.collectAsStateWithLifecycle()

    val accounts = accountsOrNull.orEmpty()
    val names = remember(accounts) { accounts.associate { it.id to it.name } }
    val accountName: (String) -> String = { names[it] ?: "Deleted account" }

    val total = accounts.filter { !it.isCard }.sumOf { it.balance }
    val recent = recentAll.take(5)
    val expenses = monthAll.filter { it.kind == TxKind.EXPENSE }
    val spent = expenses.sumOf { it.amount }
    val top = expenses.groupBy { it.category }
        .map { (name, list) -> name to list.sumOf { it.amount } }
        .sortedByDescending { it.second }
        .take(5)

    var showAllAccounts by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Account?>(null) }
    deleting?.let { account ->
        TypeDeleteDialog(
            title = "Delete ${account.name}?",
            body = "The account and every transaction recorded on it will be removed. This can't be undone.",
            onConfirm = { deleting = null; vm.deleteAccount(account) },
            onDismiss = { deleting = null },
        )
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = bottomPadding + 24.dp),
    ) {
        item("total") {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
                Text("Total balance", style = Type.label, color = C.textSecondary)
                Text(moneyDisplay(total), style = Type.display, color = C.text, modifier = Modifier.padding(top = 6.dp))
                if (accounts.any { it.isCard }) {
                    Text(
                        "Credit cards not included",
                        style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        item("accounts") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Accounts", style = Type.heading, color = C.text, modifier = Modifier.weight(1f))
                if (accounts.size > 6) HeaderAction(if (showAllAccounts) "Show less" else "Show all") { showAllAccounts = !showAllAccounts }
            }
            if (accountsOrNull != null && accounts.isEmpty()) {
                CardBox(Modifier.padding(horizontal = 20.dp).clickable { vm.sheet = Sheet.Add(2) }) {
                    EmptyState(
                        Icons.Outlined.AccountBalanceWallet, "Add your first account",
                        "Cash, a bank account or a card — tap to set one up and start tracking.",
                    )
                }
            } else {
                AccountGrid(accounts, showAllAccounts, vm, onConfigure) { deleting = it }
            }
        }

        item("recent") {
            SectionHeader(
                "Recent transactions", Modifier.padding(top = 28.dp, bottom = 10.dp),
                action = if (recentAll.isNotEmpty()) "See all" else null, onAction = onSeeAll,
            )
            CardBox(Modifier.padding(horizontal = 20.dp)) {
                if (recent.isEmpty()) {
                    EmptyState(
                        Icons.AutoMirrored.Outlined.ReceiptLong, "Nothing logged yet",
                        "Tap + to record an expense or income. It will show up here.",
                    )
                } else {
                    recent.forEachIndexed { i, tx ->
                        if (i > 0) Hairline(inset = 68.dp)
                        TxRow(tx, accountName, onClick = { vm.sheet = Sheet.EditTx(tx) })
                    }
                }
            }
        }

        item("top") {
            SectionHeader("Top spending", Modifier.padding(top = 28.dp, bottom = 10.dp))
            CardBox(Modifier.padding(horizontal = 20.dp)) {
                if (top.isEmpty()) {
                    EmptyState(
                        Icons.Outlined.DonutLarge, "No spending this month",
                        "Your five biggest categories will appear here as you log expenses.",
                    )
                } else {
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Spent in ${LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM"))}",
                                style = Type.caption, color = C.textSecondary,
                            )
                            Text(Money.format(spent), style = Type.amountLarge, color = C.text, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text("${expenses.size} ${if (expenses.size == 1) "expense" else "expenses"}", style = Type.caption, color = C.textTertiary)
                    }
                    val max = top.first().second.coerceAtLeast(1)
                    top.forEachIndexed { i, (name, amount) ->
                        SpendingRow(
                            name = name.ifBlank { "Uncategorised" },
                            amount = amount,
                            share = if (spent > 0) amount.toFloat() / spent else 0f,
                            fill = amount.toFloat() / max,
                            strength = 1f - i * 0.15f,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * Three-column grid. A tap opens the account's configure page. Long-press a card and drag
 * to reorder; a long-press without movement opens the card's menu instead.
 */
@Composable
private fun AccountGrid(
    accounts: List<Account>,
    showAll: Boolean,
    vm: MainViewModel,
    onConfigure: (String) -> Unit,
    onDelete: (Account) -> Unit,
) {
    var localIds by remember { mutableStateOf<List<String>?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }

    val byId = accounts.associateBy { it.id }
    val ordered = localIds?.mapNotNull { byId[it] }?.takeIf { it.size == accounts.size } ?: accounts
    // Drop the local order once the saved order has caught up.
    LaunchedEffect(accounts) { if (dragId == null) localIds = null }

    val visible = if (showAll) ordered else ordered.take(6)
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
        val gap = 8.dp
        val cellH = 92.dp
        val cellW = (maxWidth - gap * 2) / 3
        val rows = (visible.size + 2) / 3
        val stepX = with(density) { (cellW + gap).toPx() }
        val stepY = with(density) { (cellH + gap).toPx() }
        val halfW = with(density) { cellW.toPx() / 2 }
        val halfH = with(density) { cellH.toPx() / 2 }
        fun slot(i: Int) = Offset((i % 3) * stepX, (i / 3) * stepY)

        Box(Modifier.fillMaxWidth().animateContentSize().height(cellH * rows + gap * (rows - 1).coerceAtLeast(0))) {
            visible.forEachIndexed { index, account ->
                key(account.id) {
                    val dragging = dragId == account.id
                    val settled by animateOffsetAsState(slot(index), label = "slot")
                    val scale by animateFloatAsState(if (dragging) 1.06f else 1f, label = "lift")
                    val pos = if (dragging) dragPos else settled
                    AccountCard(
                        account = account,
                        onTransfer = { vm.sheet = Sheet.Transfer(account.id) },
                        onEditBalance = { vm.sheet = Sheet.EditBalance(account.id) },
                        onConfigure = { onConfigure(account.id) },
                        onDelete = { onDelete(account) },
                        onDragStart = {
                            localIds = ordered.map { it.id }
                            dragId = account.id
                            dragPos = slot(index)
                        },
                        onDrag = { delta ->
                            dragPos += delta
                            val ids = localIds
                            if (ids != null) {
                                val col = ((dragPos.x + halfW) / stepX).toInt().coerceIn(0, 2)
                                val row = ((dragPos.y + halfH) / stepY).toInt().coerceIn(0, rows - 1)
                                val target = (row * 3 + col).coerceAtMost(visible.size - 1)
                                val from = ids.indexOf(account.id)
                                if (from >= 0 && target != from) {
                                    localIds = ids.toMutableList().apply { add(target, removeAt(from)) }
                                }
                            }
                        },
                        onDragEnd = {
                            dragId = null
                            val ids = localIds
                            if (ids != null && ids != accounts.map { it.id }) vm.reorderAccounts(ids) else localIds = null
                        },
                        modifier = Modifier
                            .zIndex(if (dragging) 1f else 0f)
                            .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                            .width(cellW)
                            .graphicsLayer { scaleX = scale; scaleY = scale },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: Account,
    onTransfer: () -> Unit,
    onEditBalance: () -> Unit,
    onConfigure: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val start by rememberUpdatedState(onDragStart)
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onDragEnd)
    val swatch = C.swatches[account.color.mod(C.swatches.size)]
    val shape = RoundedCornerShape(18.dp)
    val haptics = LocalHapticFeedback.current
    var menu by remember { mutableStateOf(false) }

    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(shape)
                .background(swatch.fill)
                .border(1.dp, swatch.ink.copy(alpha = 0.16f), shape)
                .clickable(onClickLabel = "Configure", onClick = onConfigure)
                .pointerInput(account.id) {
                    var moved = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            moved = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            start()
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            moved += amount.getDistance()
                            drag(amount)
                        },
                        onDragEnd = {
                            if (moved < viewConfiguration.touchSlop) menu = true
                            end()
                        },
                        onDragCancel = { end() },
                    )
                }
                .padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(C.surface.copy(alpha = if (C.isDark) 0.08f else 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(accountIcon(account.type), null, tint = swatch.ink, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .offset(x = 6.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .clickable(onClickLabel = "Account options") { menu = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.MoreVert, "Account options", tint = C.textSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(account.name, style = Type.caption, color = C.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val balance = Money.format(account.balance)
            Text(
                balance,
                style = Type.amount.copy(fontSize = when { balance.length <= 10 -> 14.sp; balance.length <= 12 -> 12.5.sp; else -> 11.sp }),
                color = C.text, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }

        AccountMenu(
            expanded = menu, onDismiss = { menu = false },
            onTransfer = onTransfer, onEditBalance = onEditBalance, onConfigure = onConfigure, onDelete = onDelete,
            offset = DpOffset(8.dp, (-8).dp),
        )
    }
}

@Composable
private fun HeaderAction(text: String, onClick: () -> Unit) {
    Text(
        text, style = Type.label, color = C.primary,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun SpendingRow(name: String, amount: Long, share: Float, fill: Float, strength: Float) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = Type.bodyMedium, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${(share * 100).toInt()}%", style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(end = 10.dp))
            Text(Money.format(amount), style = Type.amount, color = C.text)
        }
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(C.surfaceAlt)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fill.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(C.primary.copy(alpha = strength.coerceIn(0.35f, 1f)))
            )
        }
    }
}

/** Whole part prominent, decimals quieter. */
@Composable
fun moneyDisplay(minor: Long): AnnotatedString {
    val text = Money.format(minor)
    val dot = text.lastIndexOf('.')
    val muted = C.textTertiary
    return buildAnnotatedString {
        append(text.substring(0, dot))
        withStyle(SpanStyle(color = muted, fontSize = 22.sp)) { append(text.substring(dot)) }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
