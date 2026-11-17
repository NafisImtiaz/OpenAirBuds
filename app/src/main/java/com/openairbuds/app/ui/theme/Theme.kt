package com.openairbuds.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Official Color System from Visual Specification
val DarkBackground = Color(0xFF000000)
val PrimarySurface = Color(0xFF17181D)
val SecondarySurface = Color(0xFF202127)
val ElevatedSurface = Color(0xFF252630)
val PrimaryText = Color(0xFFF5F5F7)
val SecondaryText = Color(0xFFA1A3AE)
val DisabledText = Color(0xFF686A75)
val AccentColor = Color(0xFF8587FF)
val AccentSecondary = Color(0xFFA4A7FF)
val SubtleBorder = Color(0xFF30313B)
val SwitchTrackColor = Color(0xFF454650)
val BatteryGreen = Color(0xFF10B981)
val BatteryIndigo = Color(0xFF8587FF)

// Kenkashi Acoustic Minimalist Design System
val KenkashiPaperBg = Color(0xFFF9F8F5)
val KenkashiPaperSurface = Color(0xFFF9F8F5)
val KenkashiPaperElevated = Color(0xFFEFECE6)
val KenkashiHairline = Color(0xFFDCD9D1)
val KenkashiTextPrimary = Color(0xFF111111)
val KenkashiTextSecondary = Color(0xFF7E7B74)
val KenkashiBlack = Color(0xFF161616)
val KenkashiWhite = Color(0xFFFFFFFF)

// Kenkashi Charcoal Dark Palette
val KenkashiDarkBg = Color(0xFF121212)
val KenkashiDarkSurface = Color(0xFF181818)
val KenkashiDarkElevated = Color(0xFF222222)
val KenkashiDarkHairline = Color(0xFF2C2B29)
val KenkashiDarkTextPrimary = Color(0xFFF9F8F5)
val KenkashiDarkTextSecondary = Color(0xFF8E8B84)

val KenkashiPaperColorScheme = lightColorScheme(
    primary = KenkashiBlack,
    onPrimary = KenkashiWhite,
    primaryContainer = KenkashiPaperElevated,
    onPrimaryContainer = KenkashiTextPrimary,
    secondary = KenkashiTextSecondary,
    onSecondary = KenkashiWhite,
    background = KenkashiPaperBg,
    surface = KenkashiPaperSurface,
    surfaceContainer = KenkashiPaperElevated,
    onSurface = KenkashiTextPrimary,
    onSurfaceVariant = KenkashiTextSecondary,
    outline = KenkashiHairline
)

val KenkashiDarkColorScheme = darkColorScheme(
    primary = KenkashiDarkTextPrimary,
    onPrimary = KenkashiDarkBg,
    primaryContainer = KenkashiDarkElevated,
    onPrimaryContainer = KenkashiDarkTextPrimary,
    secondary = KenkashiDarkTextSecondary,
    onSecondary = KenkashiDarkBg,
    background = KenkashiDarkBg,
    surface = KenkashiDarkSurface,
    surfaceContainer = KenkashiDarkElevated,
    onSurface = KenkashiDarkTextPrimary,
    onSurfaceVariant = KenkashiDarkTextSecondary,
    outline = KenkashiDarkHairline
)

// Backwards compatibility aliases
val NothingBlack = DarkBackground
val NothingCardBg = PrimarySurface
val NothingCardBorder = SubtleBorder
val NothingCircleInactive = ElevatedSurface
val NothingCircleActive = Color(0xFFFFFFFF)
val NothingTextWhite = PrimaryText
val NothingTextMuted = SecondaryText
val NothingEmerald = AccentColor

val AmoledDarkColorScheme = darkColorScheme(
    primary = AccentColor,
    onPrimary = Color(0xFF000000),
    primaryContainer = ElevatedSurface,
    onPrimaryContainer = PrimaryText,
    secondary = AccentSecondary,
    onSecondary = Color(0xFF000000),
    background = DarkBackground,
    surface = PrimarySurface,
    surfaceContainer = SecondarySurface,
    surfaceContainerHigh = ElevatedSurface,
    onSurface = PrimaryText,
    onSurfaceVariant = SecondaryText,
    outline = SubtleBorder
)

// Legacy Material 3 Forest Green fallback palette
private val M3ForestGreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF5CD894),
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF00522E),
    onPrimaryContainer = Color(0xFF7AF6AF),
    secondary = Color(0xFFB5CCBA),
    onSecondary = Color(0xFF213528),
    secondaryContainer = Color(0xFF374B3D),
    onSecondaryContainer = Color(0xFFD1E8D5),
    tertiary = Color(0xFFA2CED9),
    onTertiary = Color(0xFF01363F),
    background = Color(0xFF101412),
    surface = Color(0xFF101412),
    surfaceContainer = Color(0xFF1B211D),
    surfaceContainerHigh = Color(0xFF262C27),
    onSurface = Color(0xFFE0E3DE),
    onSurfaceVariant = Color(0xFFC0C9C0),
    outline = Color(0xFF8A938B)
)

private val M3ForestGreenLightColorScheme = lightColorScheme(
    primary = Color(0xFF006D3F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7AF6AF),
    onPrimaryContainer = Color(0xFF00210F),
    secondary = Color(0xFF4E6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E8D5),
    onSecondaryContainer = Color(0xFF0C1F14),
    background = Color(0xFFF6FAF4),
    surface = Color(0xFFF6FAF4),
    surfaceContainer = Color(0xFFECEFE8),
    surfaceContainerHigh = Color(0xFFE6E9E3),
    onSurface = Color(0xFF181D19),
    onSurfaceVariant = Color(0xFF404942),
    outline = Color(0xFF707971)
)

@Composable
fun OpenAirBudsTheme(
    themeMode: AppThemeMode = AppThemeMode.KENKASHI_PAPER,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        AppThemeMode.KENKASHI_PAPER -> KenkashiPaperColorScheme
        AppThemeMode.KENKASHI_DARK -> KenkashiDarkColorScheme
        AppThemeMode.NOTHING_DARK -> AmoledDarkColorScheme
        AppThemeMode.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) M3ForestGreenDarkColorScheme else M3ForestGreenLightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
