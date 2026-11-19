package com.kyant.backdrop.catalog.interview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String, val icon: String) {
    SYSTEM("跟随系统", "⚙️"),
    LIGHT("日间浅色", "☀️"),
    DARK("夜间深色", "🌙")
}

object ThemeState {
    var mode by mutableStateOf(ThemeMode.SYSTEM)

    val isDark: Boolean
        @Composable
        get() = when (mode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    fun toggle() {
        mode = when (mode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
    }
}

@Composable
fun isAppInDarkTheme(): Boolean = ThemeState.isDark

// -------------------------------------------------------------
// Dynamic Theme Colors (Auto-adapting between Light & Night)
// -------------------------------------------------------------

val BgColor: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF0B1118) else Color(0xFFF4F1EA)

val CardBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF161F2B) else Color(0xFFFBFAF6)

val BorderColor: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF283646) else Color(0xFFD8D2C4)

val TextInk: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFFF1F5F9) else Color(0xFF131B23)

val TextMuted: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF94A3B8) else Color(0xFF8A959E)

val TextDim: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF64748B) else Color(0xFFAFB7BD)

val PrimaryRed: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFFEF4444) else Color(0xFFC4432B)

val SecondaryButtonBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF243242) else Color(0xFFEFEBE2)

val InputBoxBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF0F172A) else Color.White

val UserChatBubbleBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF2563EB) else Color(0xFF1E293B)

val TagTeal: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF2DD4BF) else Color(0xFF2A6B63)

val TagTealBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF134E48) else Color(0xFFDFEDEA)

val TagAmber: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFFFBBF24) else Color(0xFFA9761B)

val TagAmberBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF451A03) else Color(0xFFF5EAD4)

val TagIndigo: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF60A5FA) else Color(0xFF24506B)

val TagIndigoBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF1E3A5F) else Color(0xFFDEE8EF)

val TagRed: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFFF87171) else Color(0xFFC4432B)

val TagRedBg: Color
    @Composable get() = if (isAppInDarkTheme()) Color(0xFF450A0A) else Color(0xFFF6E4DF)

val ProjectWarmYellow: Color = Color(0xFFE59819)
val KyantBlue: Color = ProjectWarmYellow
