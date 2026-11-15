package com.chronicle.expense.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Category
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.data.DebtLogic
import com.chronicle.expense.data.Money
import com.chronicle.expense.ui.components.CardBox
import com.chronicle.expense.ui.components.ConfirmDialog
import com.chronicle.expense.ui.components.EmptyState
import com.chronicle.expense.ui.components.Hairline
import com.chronicle.expense.ui.components.NotificationPermissions
import com.chronicle.expense.ui.components.Segmented
import com.chronicle.expense.ui.components.ScreenTitle
import com.chronicle.expense.ui.components.SmsPermissions
import com.chronicle.expense.ui.components.rememberPermissions
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@Composable
fun ProfileScreen(vm: MainViewModel, bottomPadding: Dp) {
    val user by vm.user.collectAsStateWithLifecycle()
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val sms = rememberPermissions(*SmsPermissions)
    val notifications = rememberPermissions(*NotificationPermissions)
    var confirmSignOut by remember { mutableStateOf(false) }
    var manageCategories by remember { mutableStateOf(false) }
    val modes = listOf("system", "light", "dark")

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = bottomPadding + 24.dp),
    ) {
        ScreenTitle("Profile")

        CardBox(Modifier.padding(top = 10.dp).fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = user?.photoUrl, contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(C.surfaceAlt).border(1.dp, C.hairline, CircleShape),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(user?.displayName ?: "Signed in", style = Type.heading, color = C.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        user?.email ?: "", style = Type.caption, color = C.textSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        // Lifetime totals: what is still outstanding plus everything already paid back.
        val debts by vm.debts.collectAsStateWithLifecycle()
        Label("Debt")
        CardBox(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp)) {
                listOf(true to "Total lent", false to "Total borrowed").forEach { (lent, title) ->
                    Column(Modifier.weight(1f)) {
                        Text(title, style = Type.caption, color = C.textSecondary)
                        Text(
                            Money.format(debts.filter { it.isLent == lent }.sumOf { it.amount + DebtLogic.settled(it.records) }),
                            style = Type.amountLarge, color = C.text, modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }

        Label("Appearance")
        CardBox(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Segmented(listOf("System", "Light", "Dark"), modes.indexOf(theme).coerceAtLeast(0), { vm.setTheme(modes[it]) })
            }
        }

        Label("Auto-log")
        CardBox(Modifier.fillMaxWidth()) {
            PermissionRow(Icons.Outlined.Sms, "SMS access", "Reads bank alerts for accounts with rules", sms.granted, sms.request)
            Hairline(inset = 64.dp)
            PermissionRow(Icons.Outlined.NotificationsNone, "Notifications", "Auto-log alerts and card reminders", notifications.granted, notifications.request)
        }

        Label("Categories")
        CardBox(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { manageCategories = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(Icons.Outlined.Category)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Manage categories", style = Type.bodyMedium, color = C.text)
                    Text("Add, rename or remove categories and sources", style = Type.caption, color = C.textSecondary, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        Label("Account")
        CardBox(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { confirmSignOut = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(Icons.AutoMirrored.Outlined.Logout)
                Spacer(Modifier.width(12.dp))
                Text("Sign out", style = Type.bodyMedium, color = C.text)
            }
        }

        Text(
            "Chronicle 1.0", style = Type.caption, color = C.textTertiary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        )
    }

    if (manageCategories) CategoriesSheet(vm) { manageCategories = false }

    if (confirmSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            body = "Your records stay safe in your account and return when you sign back in.",
            confirm = "Sign out",
            onConfirm = { confirmSignOut = false; vm.signOut() },
            onDismiss = { confirmSignOut = false },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(), style = Type.overline, color = C.textTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun IconTile(icon: ImageVector) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(C.surfaceAlt), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = C.textSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, hint: String, granted: Boolean, onRequest: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = !granted, onClick = onRequest).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Type.bodyMedium, color = C.text)
            Text(hint, style = Type.caption, color = C.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            if (granted) "On" else "Allow", style = Type.label,
            color = if (granted) C.textSecondary else C.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (granted) C.surfaceAlt else C.primarySoft)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

@Composable
fun PlaceholderScreen(title: String, icon: ImageVector, heading: String, body: String) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 2.dp)) {
        ScreenTitle(title)
        Spacer(Modifier.weight(1f))
        EmptyState(icon, heading, body)
        Spacer(Modifier.weight(1.6f))
    }
}
