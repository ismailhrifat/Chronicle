package com.chronicle.expense.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Chronicle's own confirmation card: icon tile, short copy, two equal buttons. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(26.dp)
    val accent = if (danger) C.danger else C.primary
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(C.surface)
                .border(1.dp, C.hairline, shape)
                .padding(20.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (danger) C.danger.copy(alpha = 0.12f) else C.primarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon ?: if (danger) Icons.Outlined.DeleteOutline else Icons.AutoMirrored.Outlined.HelpOutline,
                    null, tint = accent, modifier = Modifier.size(22.dp),
                )
            }
            Text(title, style = Type.title, color = C.text, modifier = Modifier.padding(top = 16.dp))
            Text(
                body, style = Type.body, color = C.textSecondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuietButton("Cancel", onDismiss, Modifier.weight(1f).height(48.dp))
                PrimaryButton(confirm, onConfirm, Modifier.weight(1.25f).height(48.dp), color = accent)
            }
        }
    }
}

/** Destructive confirmation that only unlocks once the user types DELETE. */
@Composable
fun TypeDeleteDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    var typed by remember { mutableStateOf("") }
    val unlocked = typed.trim() == "DELETE"
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(C.surface)
                .border(1.dp, C.hairline, shape)
                .padding(20.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(C.danger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = C.danger, modifier = Modifier.size(22.dp))
            }
            Text(title, style = Type.title, color = C.text, modifier = Modifier.padding(top = 16.dp))
            Text(body, style = Type.body, color = C.textSecondary, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            FieldLabel("Type DELETE to confirm")
            Field(
                typed, { typed = it.take(10) }, "DELETE",
                capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done,
            )
            Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuietButton("Cancel", onDismiss, Modifier.weight(1f).height(48.dp))
                PrimaryButton("Delete", onConfirm, Modifier.weight(1.25f).height(48.dp), enabled = unlocked, color = C.danger)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun datePickerColors() = DatePickerDefaults.colors(
    containerColor = C.surface,
    titleContentColor = C.textSecondary,
    headlineContentColor = C.text,
    weekdayContentColor = C.textTertiary,
    subheadContentColor = C.textSecondary,
    navigationContentColor = C.textSecondary,
    yearContentColor = C.text,
    currentYearContentColor = C.primary,
    selectedYearContentColor = C.onPrimary,
    selectedYearContainerColor = C.primary,
    dayContentColor = C.text,
    selectedDayContentColor = C.onPrimary,
    selectedDayContainerColor = C.primary,
    todayContentColor = C.primary,
    todayDateBorderColor = C.primary,
    dayInSelectionRangeContainerColor = C.primarySoft,
    dayInSelectionRangeContentColor = C.text,
    dividerColor = C.hairline,
)

fun LocalDate.utcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
fun utcMillisToDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.utcMillis())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = datePickerColors(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(utcMillisToDate(it)) } ?: onDismiss() }) {
                Text("Done", style = Type.bodyMedium, color = C.primary, modifier = Modifier.padding(horizontal = 4.dp))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = Type.bodyMedium, color = C.textSecondary) }
        },
    ) {
        DatePicker(state, colors = datePickerColors(), showModeToggle = false)
    }
}
