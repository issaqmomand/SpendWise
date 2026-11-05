
package com.group3.spendwise.auth

import android.content.Context
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val dashboardPreferences = FirebaseApp.getInstance()
        .applicationContext.getSharedPreferences(
            "spendwise_dashboard",
            Context.MODE_PRIVATE
        )

    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set

    var userName by mutableStateOf(
        auth.currentUser?.displayName.orEmpty()
    )
        private set

    var userEmail by mutableStateOf(
        auth.currentUser?.email.orEmpty()
    )
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var infoMessage by mutableStateOf<String?>(null)
        private set

    fun clearMessages() {
        errorMessage = null
        infoMessage = null
    }

    // Login
    fun login(email: String, password: String) {
        clearMessages()

        val cleanEmail = email.trim()

        if (cleanEmail.isEmpty() || password.isEmpty()) {
            errorMessage = "Enter your email and password."
            return
        }

        if (!isValidEmail(cleanEmail)) {
            errorMessage = "Enter a valid email address."
            return
        }

        isLoading = true

        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    refreshUser()
                } else {
                    errorMessage = friendlyError(task.exception)
                }
            }
    }

    // Registration
    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        clearMessages()

        val name = fullName.trim()
        val cleanEmail = email.trim()

        registrationError(
            name,
            cleanEmail,
            password,
            confirmPassword
        )?.let {
            errorMessage = it
            return
        }

        isLoading = true

        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    isLoading = false
                    errorMessage = friendlyError(task.exception)
                    return@addOnCompleteListener
                }

                val user = auth.currentUser

                if (user == null) {
                    isLoading = false
                    refreshUser()
                    return@addOnCompleteListener
                }

                // New accounts begin with blank financial values.
                val accountKey = "${user.email.orEmpty()}_"

                dashboardPreferences.edit()
                    .putBoolean("new_user_${user.uid}", true)
                    .remove("${accountKey}income")
                    .remove("${accountKey}expenses")
                    .remove("${accountKey}opening_balance")
                    .apply()

                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profile)
                    .addOnCompleteListener {
                        isLoading = false
                        refreshUser()
                    }
            }
    }

    // Password reset
    fun sendPasswordReset(email: String) {
        clearMessages()

        val cleanEmail = email.trim()

        if (!isValidEmail(cleanEmail)) {
            errorMessage =
                "Enter your email above, then tap Forgot password."
            return
        }

        isLoading = true

        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    infoMessage =
                        "If an account exists for that email, a reset link has been sent."
                } else {
                    errorMessage = friendlyError(task.exception)
                }
            }
    }

    // Logout
    fun logout() {
        auth.signOut()
        clearMessages()
        refreshUser()
    }

    // Update authenticated user information
    private fun refreshUser() {
        val user = auth.currentUser

        isLoggedIn = user != null
        userName = user?.displayName.orEmpty()
        userEmail = user?.email.orEmpty()
    }

    // Email validation
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Registration validation
    private fun registrationError(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? = when {

        name.isEmpty() ->
            "Enter your full name."

        !isValidEmail(email) ->
            "Enter a valid email address."

        password.length < 8 || !password.any { it.isDigit() } ->
            "Password must be at least 8 characters and include a number."

        password != confirmPassword ->
            "Passwords do not match."

        else -> null
    }

    // Firebase error messages
    private fun friendlyError(e: Exception?): String = when (e) {

        is FirebaseAuthInvalidUserException ->
            "No account found with that email."

        is FirebaseAuthInvalidCredentialsException ->
            "Incorrect email or password."

        is FirebaseAuthUserCollisionException ->
            "An account with that email already exists."

        is FirebaseNetworkException ->
            "No internet connection. Please try again."

        is FirebaseTooManyRequestsException ->
            "Too many attempts. Please wait a moment and try again."

        else ->
            e?.localizedMessage
                ?: "Something went wrong. Please try again."
    }
}
