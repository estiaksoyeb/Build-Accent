package com.buildaccent.`as`

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.buildaccent.`as`.ui.navigation.AccentBuilderNavHost
import com.buildaccent.`as`.ui.navigation.AccentBuilderScreen
import com.buildaccent.`as`.ui.theme.AccentBuilderTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- EDGE TO EDGE SETUP ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        val appContainer = (application as AccentBuilderApp).container
        val userPreferences = appContainer.userPreferencesRepository

        setContent {
            AccentBuilderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val hasSeenOnboarding by userPreferences.hasSeenOnboarding.collectAsState(initial = null)

                    if (hasSeenOnboarding != null) {
                        val startDest = if (hasSeenOnboarding == true) AccentBuilderScreen.Home.name else AccentBuilderScreen.Welcome.name
                        
                        AccentBuilderNavHost(
                            startDestination = startDest,
                            onOnboardingComplete = {
                                lifecycleScope.launch {
                                    userPreferences.setOnboardingSeen(true)
                                }
                            }
                        )
                    }
                    // Else: Loading state (blank screen), effectively a splash
                }
            }
        }
    }
}