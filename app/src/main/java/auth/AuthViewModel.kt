package com.group3.spendwise.auth

import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest

/**
 * Handles sign up, login, password reset and logout using Firebase Authentication.
 *
 * The screens read the public state below (isLoggedIn, isLoading, errorMessage, ...)
 * and Compose redraws automatically whenever one of these values changes.
 *
 * Firebase stores the signed-in session on the device, so the user stays logged in
 * after the app is closed until they tap log out.
 */
class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var isLoggedIn by mutableStateOf(auth.currentUser != null)
        private set

    var userName by mutableStateOf(auth.currentUser?.displayName.orEmpty())
        private set

    var userEmail by mutableStateOf(auth.currentUser?.email.orEmpty())
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

    // ---------------------------------------------------------------- Login

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

    // ------------------------------------------------------------- Register

    fun register(fullName: String, email: String, password: String, confirmPassword: String) {
        clearMessages()
        val name = fullName.trim()
        val cleanEmail = email.trim()

        registrationError(name, cleanEmail, password, confirmPassword)?.let {
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

                // Account created. Save the full name on the profile, then show the dashboard.
                val user = auth.currentUser
                if (user == null) {
                    isLoading = false
                    refreshUser()
                    return@addOnCompleteListener
                }

                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profile).addOnCompleteListener {
                    isLoading = false
                    refreshUser()
                }
            }
    }

    // -------------------------------------------------------- Password reset

    fun sendPasswordReset(email: String) {
        clearMessages()
        val cleanEmail = email.trim()

        if (!isValidEmail(cleanEmail)) {
            errorMessage = "Enter your email above, then tap Forgot password."
            return
        }

        isLoading = true
        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    infoMessage = "If an account exists for that email, a reset link has been sent."
                } else {
                    errorMessage = friendlyError(task.exception)
                }
            }
    }

    // --------------------------------------------------------------- Logout

    fun logout() {
        auth.signOut()
        clearMessages()
        refreshUser()
    }

    // -------------------------------------------------------------- Helpers

    private fun refreshUser() {
        val user = auth.currentUser
        isLoggedIn = user != null
        userName = user?.displayName.orEmpty()
        userEmail = user?.email.orEmpty()
    }

    private fun isValidEmail(email: String): Boolean =
        Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun registrationError(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? = when {
        name.isEmpty() -> "Enter your full name."
        !isValidEmail(email) -> "Enter a valid email address."
        password.length < 8 || !password.any { it.isDigit() } ->
            "Password must be at least 8 characters and include a number."
        password != confirmPassword -> "Passwords do not match."
        else -> null
    }

    private fun friendlyError(e: Exception?): String = when (e) {
        is FirebaseAuthInvalidUserException -> "No account found with that email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "An account with that email already exists."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
        else -> e?.localizedMessage ?: "Something went wrong. Please try again."
    }
}