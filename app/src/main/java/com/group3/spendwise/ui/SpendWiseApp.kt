package com.group3.spendwise.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.group3.spendwise.auth.AuthViewModel
import com.group3.spendwise.ui.screens.auth.LoginScreen
import com.group3.spendwise.ui.screens.navigation.MainScreen
import com.group3.spendwise.ui.screens.auth.RegisterScreen

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
