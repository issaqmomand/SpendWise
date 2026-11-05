package com.group3.spendwise

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group3.spendwise.auth.AuthViewModel
import com.group3.spendwise.ui.screens.LoginScreen
import com.group3.spendwise.ui.screens.MainScreen
import com.group3.spendwise.ui.screens.RegisterScreen
import com.group3.spendwise.ui.theme.SpendWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Theme preference is device-local and survives app restarts.
        val settings = getSharedPreferences("spendwise_settings", Context.MODE_PRIVATE)

        setContent {
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
                    onDarkModeChange = { enabled ->
                        darkMode = enabled
                        settings.edit().putBoolean("dark_mode", enabled).apply()
                    }
                )
            }
        }
    }
}

/** Keeps the existing Firebase login, registration, and logout behavior. */
@Composable
fun SpendWiseApp(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    var showRegister by rememberSaveable { mutableStateOf(false) }

    when {
        authViewModel.isLoggedIn -> MainScreen(
            userName = authViewModel.userName,
            userEmail = authViewModel.userEmail,
            onLogout = {
                authViewModel.logout()
                showRegister = false
            },
            isDarkMode = isDarkMode,
            onDarkModeChange = onDarkModeChange
        )

        showRegister -> RegisterScreen(
            authViewModel = authViewModel,
            onGoToLogin = {
                authViewModel.clearMessages()
                showRegister = false
            }
        )

        else -> LoginScreen(
            authViewModel = authViewModel,
            onGoToRegister = {
                authViewModel.clearMessages()
                showRegister = true
            }
        )
    }
}
