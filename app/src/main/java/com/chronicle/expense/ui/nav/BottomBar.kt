package com.chronicle.expense.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type
import kotlin.math.atan2
import kotlin.math.sqrt

private val BarHeight = 64.dp
private val FabSize = 56.dp
private val FabLift = 24.dp      // how far the button rises above the bar's top edge
private val NotchGap = 7.dp      // breathing room between button and bar
private val Shoulder = 12.dp     // radius easing the notch into the top edge
private val Corner = 24.dp

/** Total height the bar occupies above the system navigation inset. */
val BottomBarClearance = BarHeight + FabLift

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab("debt", "Debt", Icons.Outlined.Handshake, Icons.Filled.Handshake),
    Tab("events", "Events", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    Tab("profile", "Profile", Icons.Outlined.Person, Icons.Filled.Person),
)

/**
 * Top edge of the bar: rounded outer corners and a circular cradle for the add button,
 * joined to the straight edge by small fillets so nothing looks cut out with scissors.
 */
private fun topEdge(size: Size, density: Density, close: Boolean): Path = with(density) {
    val w = size.width
    val cx = w / 2f
    val corner = Corner.toPx()
    val r = FabSize.toPx() / 2f + NotchGap.toPx()
    val cy = FabSize.toPx() / 2f - FabLift.toPx()   // notch centre relative to the top edge
    val s = Shoulder.toPx()
    val dx = sqrt((r + s) * (r + s) - (cy - s) * (cy - s))
    val theta = Math.toDegrees(atan2((s - cy).toDouble(), dx.toDouble())).toFloat()

    Path().apply {
        moveTo(0f, corner)
        arcTo(Rect(0f, 0f, corner * 2, corner * 2), 180f, 90f, false)
        lineTo(cx - dx, 0f)
        arcTo(Rect(cx - dx - s, 0f, cx - dx + s, s * 2), 270f, 90f - theta, false)
        arcTo(Rect(cx - r, cy - r, cx + r, cy + r), 180f - theta, -(180f - 2 * theta), false)
        arcTo(Rect(cx + dx - s, 0f, cx + dx + s, s * 2), 180f + theta, 90f - theta, false)
        lineTo(w - corner, 0f)
        arcTo(Rect(w - corner * 2, 0f, w, corner * 2), 270f, 90f, false)
        if (close) {
            lineTo(w, size.height)
            lineTo(0f, size.height)
            close()
        }
    }
}

private object BarShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(topEdge(size, density, close = true))
}

@Composable
fun ChronicleBottomBar(current: String?, onTab: (String) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val hairline = C.hairline
    val shadowColor = if (C.isDark) Color.Transparent else Color(0xFF1E2321)

    Box(modifier.fillMaxWidth().height(BarHeight + FabLift + inset)) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BarHeight + inset)
                .shadow(18.dp, BarShape, clip = false, ambientColor = shadowColor.copy(alpha = 0.10f), spotColor = shadowColor.copy(alpha = 0.10f))
                .background(C.surface, BarShape)
                .drawBehind {
                    drawPath(topEdge(size, density, close = false), hairline, style = Stroke(1.dp.toPx()))
                }
        ) {
            Row(Modifier.fillMaxWidth().height(BarHeight).padding(horizontal = 8.dp)) {
                tabs.take(2).forEach { NavItem(it, current == it.route) { onTab(it.route) } }
                Spacer(Modifier.width(FabSize + NotchGap * 2 + Shoulder * 2))
                tabs.drop(2).forEach { NavItem(it, current == it.route) { onTab(it.route) } }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(FabSize)
                .shadow(10.dp, CircleShape, ambientColor = C.primary.copy(alpha = 0.35f), spotColor = C.primary.copy(alpha = 0.45f))
                .clip(CircleShape)
                .background(C.primary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = C.onPrimary),
                    role = Role.Button,
                    onClickLabel = "Add",
                    onClick = onAdd,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, "Add", tint = C.onPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun RowScope.NavItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) C.primary else C.textTertiary
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(if (selected) tab.selectedIcon else tab.icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            tab.label,
            style = Type.caption.copy(fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = if (selected) C.primary else C.textSecondary,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
