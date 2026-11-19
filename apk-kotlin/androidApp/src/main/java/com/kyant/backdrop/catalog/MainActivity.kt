package com.kyant.backdrop.catalog

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import com.kyant.backdrop.catalog.interview.ThemeMode
import com.kyant.backdrop.catalog.interview.ThemeState
import com.kyant.backdrop.catalog.interview.isAppInDarkTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 读取本地持久化主题偏好
        val prefs = getSharedPreferences("app_theme", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        ThemeState.mode = try {
            ThemeMode.valueOf(savedTheme)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        // 2. 启用系统级全面沉浸式 Edge-to-Edge
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        // 3. 锁定系统最高 120Hz 刷新率调度
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val modes = display?.supportedModes ?: emptyArray()
                val maxMode = modes.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate >= 90f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = maxMode.modeId
                        preferredRefreshRate = maxMode.refreshRate
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val modes = windowManager.defaultDisplay.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null && maxMode.refreshRate >= 90f) {
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = maxMode.modeId
                        preferredRefreshRate = maxMode.refreshRate
                    }
                }
            }
        } catch (_: Exception) {
        }

        setContent {
            val isDark = isAppInDarkTheme()

            // 动态响应日间/夜间模式：暗黑模式下状态栏/导航栏文字为亮白，日间模式下为墨色
            DisposableEffect(isDark) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
                onDispose {}
            }

            // 持久化当前选中的主题模式
            DisposableEffect(ThemeState.mode) {
                prefs.edit().putString("theme_mode", ThemeState.mode.name).apply()
                onDispose {}
            }

            MainContent()
        }
    }
}
