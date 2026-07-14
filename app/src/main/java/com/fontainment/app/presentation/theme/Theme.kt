package com.fontainment.app.presentation.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class CustomThemeColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onSurfaceText: Color,
    val accentSecondary: Color = primary,
    val isDark: Boolean = true
)

object ThemePresets {
    val Tesla = CustomThemeColors(TeslaPrimary, TeslaBg, TeslaSurface, TeslaText)
    val Bmw = CustomThemeColors(BmwPrimary, BmwBg, BmwSurface, BmwText)
    val Cyber = CustomThemeColors(CyberPrimary, CyberBg, CyberSurface, CyberText, CyberSecondary)
    val Amoled = CustomThemeColors(AmoledPrimary, AmoledBg, AmoledSurface, AmoledText)
    val Nothing = CustomThemeColors(NothingPrimary, NothingBg, NothingSurface, NothingText)
    val Lucid = CustomThemeColors(LucidPrimary, LucidBg, LucidSurface, LucidText, isDark = false)
    val Midnight = CustomThemeColors(MidnightPrimary, MidnightBg, MidnightSurface, MidnightText)
    val Minimal = CustomThemeColors(MinimalPrimary, MinimalBg, MinimalSurface, MinimalText)
    val Classic = CustomThemeColors(ClassicPrimary, ClassicBg, ClassicSurface, ClassicText)

    fun getPreset(themeName: String): CustomThemeColors {
        return when (themeName) {
            "Tesla Dark" -> Tesla
            "BMW Blue" -> Bmw
            "AMOLED Black" -> Amoled
            "Nothing Style" -> Nothing
            "Lucid White" -> Lucid
            "Cyber Neon" -> Cyber
            "Midnight Black" -> Midnight
            "Minimal Gray" -> Minimal
            "Classic Dashboard" -> Classic
            else -> Tesla
        }
    }
}

@Composable
fun FontainmentTheme(
    themeName: String = "Tesla Dark",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = if (themeName == "Wallpaper Adaptive" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        val customColors = ThemePresets.getPreset(themeName)
        if (customColors.isDark) {
            darkColorScheme(
                primary = customColors.primary,
                secondary = customColors.accentSecondary,
                background = customColors.background,
                surface = customColors.surface,
                onPrimary = Color.Black,
                onBackground = customColors.onSurfaceText,
                onSurface = customColors.onSurfaceText
            )
        } else {
            lightColorScheme(
                primary = customColors.primary,
                secondary = customColors.accentSecondary,
                background = customColors.background,
                surface = customColors.surface,
                onPrimary = Color.White,
                onBackground = customColors.onSurfaceText,
                onSurface = customColors.onSurfaceText
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

