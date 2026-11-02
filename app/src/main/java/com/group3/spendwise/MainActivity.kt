package com.group3.spendwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group3.spendwise.auth.AuthViewModel
import com.group3.spendwise.ui.screens.LoginScreen
import com.group3.spendwise.ui.screens.MainScreen
import com.group3.spendwise.ui.screens.RegisterScreen
import com.group3.spendwise.ui.theme.SpendWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Dark status bar icons, because our background is light
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            SpendWiseTheme {
                SpendWiseApp()
            }
        }
    }
}

/**
 * Decides which screen to show:
 *  - signed in            -> MainScreen (dashboard + bottom navigation)
 *  - not signed in        -> LoginScreen, or RegisterScreen if the user tapped "Create Account"
 *
 * Firebase remembers the signed-in user, so reopening the app skips the login screen.
 */
@Composable
fun SpendWiseApp(authViewModel: AuthViewModel = viewModel()) {
    var showRegister by rememberSaveable { mutableStateOf(false) }

    when {
        authViewModel.isLoggedIn -> MainScreen(
            userName = authViewModel.userName,
            userEmail = authViewModel.userEmail,
            onLogout = {
                authViewModel.logout()
                showRegister = false
            }
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