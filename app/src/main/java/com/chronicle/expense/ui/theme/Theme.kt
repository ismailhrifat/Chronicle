package com.chronicle.expense.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.chronicle.expense.R

@Immutable
data class Swatch(val name: String, val fill: Color, val ink: Color)

@Immutable
data class ChronicleColors(
    val bg: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val hairline: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val expense: Color,
    val income: Color,
    val danger: Color,
    val scrim: Color,
    val swatches: List<Swatch>,
    val isDark: Boolean,
)

private val LightColors = ChronicleColors(
    bg = Color(0xFFF6F5F2),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFF0EFEB),
    hairline = Color(0xFFE7E5E0),
    text = Color(0xFF1E2321),
    textSecondary = Color(0xFF6C726E),
    textTertiary = Color(0xFF9DA29E),
    primary = Color(0xFF3F6B5C),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFE6EEEA),
    expense = Color(0xFFB4594B),
    income = Color(0xFF3E7C5F),
    danger = Color(0xFFB4594B),
    scrim = Color(0x521E2321),
    swatches = listOf(
        Swatch("Sage", Color(0xFFE3ECE6), Color(0xFF4A7262)),
        Swatch("Sand", Color(0xFFF1E9DA), Color(0xFF8A6F3F)),
        Swatch("Dusty blue", Color(0xFFE1E9F1), Color(0xFF4C6C8C)),
        Swatch("Lilac", Color(0xFFE9E4F1), Color(0xFF6C5C8E)),
        Swatch("Clay", Color(0xFFF3E3DD), Color(0xFF9A5E4C)),
        Swatch("Mist", Color(0xFFE6EAEC), Color(0xFF55656D)),
        Swatch("Olive", Color(0xFFEAECD9), Color(0xFF6E7440)),
        Swatch("Rose", Color(0xFFF3E2E6), Color(0xFF955A6A)),
    ),
    isDark = false,
)

private val DarkColors = ChronicleColors(
    bg = Color(0xFF141615),
    surface = Color(0xFF1C1F1E),
    surfaceAlt = Color(0xFF232726),
    hairline = Color(0xFF2A2E2C),
    text = Color(0xFFE3E5E2),
    textSecondary = Color(0xFF9AA09C),
    textTertiary = Color(0xFF6B716D),
    primary = Color(0xFF8DB5A4),
    onPrimary = Color(0xFF12211C),
    primarySoft = Color(0xFF22302B),
    expense = Color(0xFFD08F84),
    income = Color(0xFF8FC2A6),
    danger = Color(0xFFD08F84),
    scrim = Color(0x99000000),
    swatches = listOf(
        Swatch("Sage", Color(0xFF24302B), Color(0xFF9CC2B1)),
        Swatch("Sand", Color(0xFF322C20), Color(0xFFD3BC8C)),
        Swatch("Dusty blue", Color(0xFF222B35), Color(0xFF9DB9D6)),
        Swatch("Lilac", Color(0xFF2B2735), Color(0xFFB9AAD9)),
        Swatch("Clay", Color(0xFF352722), Color(0xFFDBA896)),
        Swatch("Mist", Color(0xFF272B2D), Color(0xFFA9B8BF)),
        Swatch("Olive", Color(0xFF2C2E1F), Color(0xFFBEC48A)),
        Swatch("Rose", Color(0xFF35252A), Color(0xFFDDA5B4)),
    ),
    isDark = true,
)

val LocalColors = staticCompositionLocalOf { LightColors }

/** Shorthand accessor: `C.text`, `C.hairline`… */
val C: ChronicleColors
    @Composable get() = LocalColors.current

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

object Type {
    val display = style(34, 40, 600, -0.02).copy(fontFeatureSettings = "tnum")
    val title = style(20, 26, 600, -0.01)
    val heading = style(16, 22, 600, -0.005)
    val body = style(15, 21, 400)
    val bodyMedium = style(15, 21, 500)
    val label = style(13, 18, 500)
    val caption = style(12, 16, 400)
    val overline = style(11, 14, 600, 0.06)
    val amount = style(15, 21, 600).copy(fontFeatureSettings = "tnum")
    val amountLarge = style(20, 26, 600, -0.01).copy(fontFeatureSettings = "tnum")
}

private val AppTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = Inter),
        displayMedium = t.displayMedium.copy(fontFamily = Inter),
        displaySmall = t.displaySmall.copy(fontFamily = Inter),
        headlineLarge = t.headlineLarge.copy(fontFamily = Inter),
        headlineMedium = t.headlineMedium.copy(fontFamily = Inter),
        headlineSmall = t.headlineSmall.copy(fontFamily = Inter),
        titleLarge = Type.title,
        titleMedium = Type.heading,
        titleSmall = Type.label,
        bodyLarge = Type.body,
        bodyMedium = Type.body,
        bodySmall = Type.caption,
        labelLarge = Type.bodyMedium,
        labelMedium = Type.label,
        labelSmall = Type.caption,
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChronicleTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkColors else LightColors

    val scheme = if (dark) {
        darkColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primarySoft, onPrimaryContainer = c.text,
            secondary = c.primary, onSecondary = c.onPrimary,
            secondaryContainer = c.primarySoft, onSecondaryContainer = c.text,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surfaceAlt, surfaceContainerLow = c.surface,
            surfaceContainerLowest = c.bg, surfaceTint = Color.Transparent,
            outline = c.hairline, outlineVariant = c.hairline,
            error = c.danger, onError = c.onPrimary, scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = c.primary, onPrimary = c.onPrimary,
            primaryContainer = c.primarySoft, onPrimaryContainer = c.text,
            secondary = c.primary, onSecondary = c.onPrimary,
            secondaryContainer = c.primarySoft, onSecondaryContainer = c.text,
            background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text,
            surfaceVariant = c.surfaceAlt, onSurfaceVariant = c.textSecondary,
            surfaceContainer = c.surface, surfaceContainerHigh = c.surface,
            surfaceContainerHighest = c.surfaceAlt, surfaceContainerLow = c.surface,
            surfaceContainerLowest = c.bg, surfaceTint = Color.Transparent,
            outline = c.hairline, outlineVariant = c.hairline,
            error = c.danger, onError = c.onPrimary, scrim = Color.Black,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
