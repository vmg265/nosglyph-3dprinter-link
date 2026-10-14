package com.example.bambuglyph

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.bambuglyph.data.PreferencesManager
import com.example.bambuglyph.ui.dashboard.DashboardScreen
import com.example.bambuglyph.ui.onboarding.OnboardingScreen

enum class Screen {
    ONBOARDING,
    DASHBOARD
}

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var currentScreen by remember {
        mutableStateOf(
            if (prefs.isOnboardingCompleted) Screen.DASHBOARD else Screen.ONBOARDING
        )
    }

    when (currentScreen) {
        Screen.ONBOARDING -> {
            OnboardingScreen(
                onComplete = {
                    currentScreen = Screen.DASHBOARD
                },
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            )
        }
        Screen.DASHBOARD -> {
            DashboardScreen(
                onNavigateToOnboarding = {
                    currentScreen = Screen.ONBOARDING
                },
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            )
        }
    }
}
