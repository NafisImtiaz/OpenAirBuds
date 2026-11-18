package com.openairbuds.app.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppThemeMode(val id: String, val title: String, val subtitle: String) {
    AUTO(
        id = "auto",
        title = "Auto",
        subtitle = "Follow system light and dark mode settings"
    ),
    KENKASHI_PAPER(
        id = "kenkashi_paper",
        title = "Kenkashi Paper Light",
        subtitle = "Warm ivory canvas with hairline grid & editorial typography"
    ),
    KENKASHI_DARK(
        id = "kenkashi_dark",
        title = "Kenkashi Charcoal Dark",
        subtitle = "Minimalist matte charcoal dark with ivory line accents"
    ),
    NOTHING_DARK(
        id = "nothing_dark",
        title = "Nothing X Dark",
        subtitle = "Pitch black AMOLED with custom squircles & serif header"
    ),
    MATERIAL_YOU(
        id = "material_you",
        title = "Material 3 Dynamic",
        subtitle = "Google Material You with dynamic wallpaper Monet theming"
    );

    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        AUTO -> systemInDarkTheme
        KENKASHI_DARK, NOTHING_DARK -> true
        KENKASHI_PAPER -> false
        MATERIAL_YOU -> systemInDarkTheme
    }

    companion object {
        fun fromId(id: String?): AppThemeMode = when (id) {
            AUTO.id -> AUTO
            KENKASHI_DARK.id -> KENKASHI_DARK
            KENKASHI_PAPER.id -> KENKASHI_PAPER
            NOTHING_DARK.id -> NOTHING_DARK
            MATERIAL_YOU.id -> MATERIAL_YOU
            else -> AUTO
        }
    }
}

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("openairbuds_theme_prefs", Context.MODE_PRIVATE)

    var currentTheme: AppThemeMode by mutableStateOf(loadTheme())
        private set

    private fun loadTheme(): AppThemeMode {
        val saved = prefs.getString("selected_theme", AppThemeMode.AUTO.id)
        return AppThemeMode.fromId(saved)
    }

    fun setTheme(theme: AppThemeMode) {
        currentTheme = theme
        prefs.edit().putString("selected_theme", theme.id).apply()
    }
}
