package com.group3.spendwise.ui.screens.auth

import com.group3.spendwise.R
import androidx.compose.ui.res.stringResource
import com.group3.spendwise.ui.components.localizedCategory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.group3.spendwise.auth.AuthViewModel
import com.group3.spendwise.ui.components.AppTextField
import com.group3.spendwise.ui.components.AuthHeader
import com.group3.spendwise.ui.components.AuthMessages
import com.group3.spendwise.ui.components.PasswordField
import com.group3.spendwise.ui.components.PrimaryButton

@Composable
fun RegisterScreen(
    authViewModel: AuthViewModel,
    onGoToLogin: () -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        AuthHeader()

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.register_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        AppTextField(
            label = stringResource(R.string.register_full_name),
            value = fullName,
            onValueChange = { fullName = it },
            placeholder = stringResource(R.string.register_name_hint)
        )

        Spacer(Modifier.height(12.dp))

        AppTextField(
            label = stringResource(R.string.auth_email),
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.auth_email_hint),
            keyboardType = KeyboardType.Email
        )

        Spacer(Modifier.height(12.dp))

        PasswordField(
            label = stringResource(R.string.auth_password),
            value = password,
            onValueChange = { password = it },
            placeholder = stringResource(R.string.register_password_hint)
        )

        Spacer(Modifier.height(12.dp))

        PasswordField(
            label = stringResource(R.string.register_confirm_password),
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = stringResource(R.string.register_confirm_hint),
            imeAction = ImeAction.Done
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.register_password_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        AuthMessages(
            errorMessage = authViewModel.errorMessage,
            infoMessage = authViewModel.infoMessage
        )

        PrimaryButton(
            text = stringResource(R.string.register_button),
            onClick = {
                authViewModel.register(fullName, email, password, confirmPassword)
            },
            isLoading = authViewModel.isLoading
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.register_existing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onGoToLogin) {
                Text(
                    text = stringResource(R.string.login_button),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.app_privacy_line),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}