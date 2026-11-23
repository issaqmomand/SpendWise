package com.group3.spendwise

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.group3.spendwise.ui.theme.SpendWiseTheme
import com.group3.spendwise.ui.SpendWiseApp

/**
 * The single Activity of the app. Everything on screen is drawn with Jetpack Compose,
 * starting from [SpendWiseApp]. Extends AppCompatActivity so per-app language switching works.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw the app behind the status and navigation bars (edge-to-edge layout).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Theme preference is device-local and survives app restarts.
        val settings = getSharedPreferences("spendwise_settings", Context.MODE_PRIVATE)

        setContent {
            // Dark mode flag, initialised from saved settings and kept across rotations.
            var darkMode by rememberSaveable {
                mutableStateOf(settings.getBoolean("dark_mode", false))
            }

            // Match status/navigation bar icons to the selected app theme.
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkMode
                    isAppearanceLightNavigationBars = !darkMode
                }
            }

            SpendWiseTheme(darkTheme = darkMode) {
                SpendWiseApp(
                    isDarkMode = darkMode,
                    // Update the UI immediately and save the choice for the next launch.
                    onDarkModeChange = { enabled ->
                        darkMode = enabled
                        settings.edit().putBoolean("dark_mode", enabled).apply()
                    }
                )
            }
        }
    }
}

