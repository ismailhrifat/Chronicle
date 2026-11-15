package com.chronicle.expense.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.chronicle.expense.data.Event
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@Composable
fun AppMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset,
        shape = RoundedCornerShape(16.dp),
        containerColor = C.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, C.hairline),
        content = content,
    )
}

@Composable
fun MenuItem(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = Type.body, color = if (danger) C.danger else C.text) },
        leadingIcon = { Icon(icon, null, tint = if (danger) C.danger else C.textSecondary, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
        contentPadding = PaddingValues(start = 14.dp, end = 28.dp),
    )
}

/** The account's three-dot menu. [onConfigure] is null on the configure page itself. */
@Composable
fun AccountMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onTransfer: () -> Unit,
    onEditBalance: () -> Unit,
    onConfigure: (() -> Unit)?,
    onDelete: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
) {
    AppMenu(expanded, onDismiss, offset) {
        MenuItem(Icons.Outlined.SwapHoriz, "Transfer") { onDismiss(); onTransfer() }
        MenuItem(Icons.Outlined.Edit, "Edit balance") { onDismiss(); onEditBalance() }
        if (onConfigure != null) MenuItem(Icons.Outlined.Tune, "Configure") { onDismiss(); onConfigure() }
        MenuItem(Icons.Outlined.DeleteOutline, "Delete", danger = true) { onDismiss(); onDelete() }
    }
}

enum class EventAction { RENAME, TOGGLE, DELETE }

/** The event's three-dot menu, shared by the event page and long-press on the list. */
@Composable
fun EventMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    event: Event?,
    onAction: (EventAction) -> Unit,
    offset: DpOffset = DpOffset.Zero,
) {
    AppMenu(expanded, onDismiss, offset) {
        MenuItem(Icons.Outlined.Edit, "Rename") { onDismiss(); onAction(EventAction.RENAME) }
        if (event?.closed == true) {
            MenuItem(Icons.Outlined.CalendarMonth, "Mark as active") { onDismiss(); onAction(EventAction.TOGGLE) }
        } else {
            MenuItem(Icons.Outlined.CheckCircle, "Mark as closed") { onDismiss(); onAction(EventAction.TOGGLE) }
        }
        MenuItem(Icons.Outlined.DeleteOutline, "Delete event", danger = true) { onDismiss(); onAction(EventAction.DELETE) }
    }
}
