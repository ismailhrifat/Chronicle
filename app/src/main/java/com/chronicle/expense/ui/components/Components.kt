package com.chronicle.expense.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.SouthWest
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chronicle.expense.data.AccountType
import com.chronicle.expense.data.ACCOUNT_OTHER
import com.chronicle.expense.data.CATEGORY_AUTO_LOG
import com.chronicle.expense.data.Money
import com.chronicle.expense.data.Transaction
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ---- Surfaces -------------------------------------------------------------------------

val CardShape = RoundedCornerShape(18.dp)
val FieldShape = RoundedCornerShape(14.dp)

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(CardShape)
            .background(C.surface)
            .border(1.dp, C.hairline, CardShape),
        content = content,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(
        modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(C.hairline)
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Type.heading, color = C.text, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action, style = Type.label, color = C.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.label, color = C.textSecondary, modifier = modifier.padding(bottom = 8.dp))
}

// ---- Buttons & chips ------------------------------------------------------------------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = C.primary,
    contentColor: Color = C.onPrimary,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(FieldShape)
            .background(color)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = contentColor)
    }
}

@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = C.text) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(FieldShape)
            .border(1.dp, C.hairline, FieldShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodyMedium, color = color)
    }
}

/** Bottom of every edit form: Delete on the left, Save on the right. */
@Composable
fun SaveDeleteRow(save: String, enabled: Boolean, onSave: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuietButton("Delete", onDelete, Modifier.weight(1f), color = C.danger)
        PrimaryButton(save, onSave, Modifier.weight(1.4f), enabled = enabled)
    }
}

/** Compact on/off switch for inline options. */
@Composable
fun SmallToggle(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    val track by animateColorAsState(if (checked) C.primary else C.hairline, label = "toggleTrack")
    val x by androidx.compose.animation.core.animateDpAsState(if (checked) 18.dp else 2.dp, label = "toggleThumb")
    Box(
        Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(onClickLabel = label, role = androidx.compose.ui.semantics.Role.Switch) { onChange(!checked) },
    ) {
        Box(Modifier.padding(start = x, top = 2.dp).size(18.dp).clip(CircleShape).background(if (checked) C.onPrimary else C.surface))
    }
}

@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val shape = RoundedCornerShape(10.dp)
    val bg by animateColorAsState(if (selected) C.primarySoft else C.surface, label = "chipBg")
    val border = if (selected) C.primary.copy(alpha = 0.35f) else C.hairline
    val fg = if (selected) C.primary else C.textSecondary
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = Type.label, color = fg, maxLines = 1)
    }
}

/** iOS-style segmented control with a sliding thumb. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(C.surfaceAlt)
            .padding(3.dp)
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.85f, stiffness = 500f), label = "thumb")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .shadow(if (C.isDark) 0.dp else 1.dp, RoundedCornerShape(9.dp))
                .clip(RoundedCornerShape(9.dp))
                .background(if (C.isDark) C.hairline else C.surface)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, style = Type.label,
                        color = if (i == selected) C.text else C.textSecondary,
                    )
                }
            }
        }
    }
}

// ---- Inputs ---------------------------------------------------------------------------

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = Type.body,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val border by animateColorAsState(if (focused) C.primary.copy(alpha = 0.55f) else Color.Transparent, label = "fieldBorder")
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = textStyle.copy(color = C.text),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        cursorBrush = SolidColor(C.primary),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() ?: defaultKeyboardAction(imeAction) }),
        decorationBox = { inner ->
            Row(
                Modifier
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(FieldShape)
                    .background(C.surfaceAlt)
                    .border(1.5.dp, border, FieldShape)
                    .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = C.textTertiary, maxLines = 1)
                    inner()
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        },
    )
}

/** Large centered money input used at the top of forms. */
@Composable
fun AmountField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, accent: Color = C.text) {
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(sanitizeAmount(it)) },
        modifier = modifier.fillMaxWidth(),
        textStyle = Type.display.copy(color = accent, textAlign = TextAlign.Start),
        singleLine = true,
        cursorBrush = SolidColor(C.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Money.SYMBOL, style = Type.display.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = C.textTertiary)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min).defaultMinSize(minWidth = 24.dp)) {
                    if (value.isEmpty()) Text("0", style = Type.display, color = C.textTertiary)
                    inner()
                }
            }
        },
    )
}

/** Keeps digits and a single decimal point with at most two decimals. */
fun sanitizeAmount(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    if (dot < 0) return filtered.take(11)
    val whole = filtered.substring(0, dot).take(11)
    val fraction = filtered.substring(dot + 1).replace(".", "").take(2)
    return "$whole.$fraction"
}

// ---- Bottom sheet ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(onDismiss: () -> Unit, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Never taller than the area below the status bar, keyboard open or not.
        modifier = Modifier.statusBarsPadding().padding(top = 8.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = C.surface,
        contentColor = C.text,
        scrimColor = C.scrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(C.hairline)
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        ) {
            if (title != null) {
                Text(title, style = Type.title, color = C.text, modifier = Modifier.padding(bottom = 16.dp))
            }
            content()
        }
    }
}

// ---- Top bar --------------------------------------------------------------------------

@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
        Text(title, style = Type.title, color = C.text, modifier = Modifier.weight(1f).padding(start = 4.dp))
        actions()
    }
}

/** Heading for the four tab screens; fixed height so every tab's title sits at the same spot. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Type.title, color = C.text, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun IconBtn(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.text) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

// ---- Transactions ---------------------------------------------------------------------

fun accountIcon(type: AccountType): ImageVector = when (type) {
    AccountType.CASH -> Icons.Outlined.Payments
    AccountType.BANK -> Icons.Outlined.AccountBalance
    AccountType.CREDIT_CARD -> Icons.Outlined.CreditCard
    AccountType.FDR -> Icons.Outlined.Lock
    AccountType.DPS -> Icons.Outlined.Savings
}

fun categoryIcon(tx: Transaction): ImageVector {
    if (tx.kind == TxKind.TRANSFER) return Icons.Outlined.SwapHoriz
    if (tx.kind.isDebt) return Icons.Outlined.Handshake
    if (tx.eventId != null) return Icons.Outlined.CalendarMonth
    val name = tx.category.lowercase()
    fun has(vararg keys: String) = keys.any { name.contains(it) }
    return when {
        name == CATEGORY_AUTO_LOG.lowercase() -> Icons.Outlined.Sms
        has("grocer", "bazar", "market") -> Icons.Outlined.ShoppingBasket
        has("food", "restaurant", "dining", "lunch", "dinner", "breakfast", "snack", "coffee") -> Icons.Outlined.Restaurant
        has("transport", "uber", "fuel", "bus", "ride", "travel", "car") -> Icons.Outlined.DirectionsCar
        has("rent", "home", "house") -> Icons.Outlined.Home
        has("utilit", "bill", "electric", "internet", "gas", "water", "mobile") -> Icons.Outlined.Bolt
        has("shop", "cloth") -> Icons.Outlined.ShoppingBag
        has("health", "medic", "doctor", "pharma") -> Icons.Outlined.MedicalServices
        has("entertain", "movie", "game", "subscription") -> Icons.Outlined.Movie
        has("salary", "freelance", "work", "business") -> Icons.Outlined.WorkOutline
        has("gift") -> Icons.Outlined.CardGiftcard
        has("interest", "profit") -> Icons.Outlined.Percent
        name == "other" -> Icons.Outlined.Category
        tx.kind == TxKind.INCOME -> Icons.Outlined.SouthWest
        else -> Icons.AutoMirrored.Outlined.ReceiptLong
    }
}

@Composable
fun signedAmount(tx: Transaction): Pair<String, Color> = when (tx.kind) {
    TxKind.EXPENSE -> "−${Money.format(tx.amount)}" to C.text
    TxKind.INCOME -> "+${Money.format(tx.amount)}" to C.income
    TxKind.TRANSFER -> Money.format(tx.amount) to C.textSecondary
    TxKind.DEBT_OUT -> "−${Money.format(tx.amount)}" to C.text
    TxKind.DEBT_IN -> "+${Money.format(tx.amount)}" to C.income
}

@Composable
fun TxRow(
    tx: Transaction,
    accountName: (String) -> String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Replaces the category as the row title; the subtitle then shows only the account. */
    title: String? = null,
) {
    val (amount, amountColor) = signedAmount(tx)
    val needsReview = tx.autoLogged && tx.category == CATEGORY_AUTO_LOG
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (needsReview) C.primarySoft else C.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryIcon(tx), null, tint = if (needsReview) C.primary else C.textSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title ?: if (tx.kind == TxKind.TRANSFER) "Transfer" else tx.category.ifBlank { tx.kind.label },
                style = Type.bodyMedium, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                title != null -> accountName(tx.accountId)
                tx.kind == TxKind.TRANSFER -> "${accountName(tx.accountId)} → ${tx.toAccountId?.let(accountName) ?: ACCOUNT_OTHER}"
                needsReview -> "${accountName(tx.accountId)} · Needs a category"
                tx.description.isNotBlank() -> "${accountName(tx.accountId)} · ${tx.description}"
                else -> accountName(tx.accountId)
            }
            Text(
                sub, style = Type.caption, color = if (needsReview) C.primary else C.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, style = Type.amount, color = amountColor, maxLines = 1)
            Text(formatTime(tx.timestamp), style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(C.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = C.textSecondary, modifier = Modifier.size(22.dp))
        }
        Text(title, style = Type.bodyMedium, color = C.text, modifier = Modifier.padding(top = 14.dp))
        Text(
            body, style = Type.caption.copy(lineHeight = Type.body.lineHeight), color = C.textSecondary,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---- Date helpers ---------------------------------------------------------------------

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")
private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")
private val dayYearFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

private fun zoned(ts: Long) = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())

fun formatTime(ts: Long): String = zoned(ts).format(timeFormat)

fun formatDay(ts: Long): String {
    val date = zoned(ts).toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> date.format(dayFormat)
        else -> date.format(dayYearFormat)
    }
}

fun formatDateTime(ts: Long): String = "${formatDay(ts)}, ${formatTime(ts)}"
