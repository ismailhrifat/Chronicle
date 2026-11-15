package com.chronicle.expense.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.data.CATEGORY_AUTO_LOG
import com.chronicle.expense.data.CATEGORY_OTHER
import com.chronicle.expense.data.Category
import com.chronicle.expense.data.TxKind
import com.chronicle.expense.ui.components.AppSheet
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.Field
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.IconBtn
import com.chronicle.expense.ui.components.Segmented
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@Composable
fun CategoriesSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val all by vm.categories.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var new by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    val kind = if (tab == 0) TxKind.EXPENSE else TxKind.INCOME
    val noun = if (tab == 0) "category" else "source"
    val list = all.filter { it.kind == kind }.sortedBy { it.name.lowercase() }
    val exists = list.any { it.name.equals(new.trim(), ignoreCase = true) }

    AppSheet(onDismiss, title = "Categories") {
        Segmented(listOf("Expense", "Income"), tab, { tab = it; new = "" })
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Field(
                new, { new = it.take(40) }, "New $noun", Modifier.weight(1f),
                capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done,
            )
            Spacer(Modifier.width(8.dp))
            val canAdd = new.isNotBlank() && !exists
            Text(
                "Add", style = Type.bodyMedium, color = if (canAdd) C.primary else C.textTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = canAdd) { vm.addCategory(new, kind); new = "" }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        list.forEachIndexed { i, c ->
            if (i > 0) Hairline()
            val reserved = c.name == CATEGORY_OTHER || c.name == CATEGORY_AUTO_LOG
            Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, style = Type.body, color = C.text, modifier = Modifier.weight(1f))
                if (reserved) {
                    Text("Built in", style = Type.caption, color = C.textTertiary, modifier = Modifier.padding(end = 12.dp))
                } else {
                    IconBtn(Icons.Outlined.Edit, "Rename", { renaming = c }, tint = C.textSecondary)
                    IconBtn(Icons.Outlined.DeleteOutline, "Delete", { deleting = c }, tint = C.textSecondary)
                }
            }
        }
    }

    renaming?.let { c ->
        var name by remember(c) { mutableStateOf(c.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = C.surface, tonalElevation = 0.dp, shape = RoundedCornerShape(24.dp),
            title = { Text("Rename $noun", style = Type.title, color = C.text) },
            text = {
                Field(name, { name = it.take(40) }, "Name", capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = { vm.renameCategory(c, name); renaming = null }) {
                    Text("Save", style = Type.bodyMedium, color = C.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renaming = null }) { Text("Cancel", style = Type.bodyMedium, color = C.textSecondary) }
            },
        )
    }
    deleting?.let { c ->
        ConfirmDialog(
            title = "Delete ${c.name}?",
            body = "Existing transactions keep this name. It just stops appearing as a choice.",
            confirm = "Delete", danger = true,
            onConfirm = { vm.deleteCategory(c); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}
