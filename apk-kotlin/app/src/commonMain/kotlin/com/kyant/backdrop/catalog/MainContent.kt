package com.kyant.backdrop.catalog

import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.catalog.interview.InterviewApp
import com.kyant.backdrop.catalog.interview.isAppInDarkTheme

@Composable
fun MainContent() {
    val isDark = isAppInDarkTheme()

    CompositionLocalProvider(
        LocalIndication provides ripple(color = if (!isDark) Color.Black else Color.White)
    ) {
        InterviewApp()
    }
}
