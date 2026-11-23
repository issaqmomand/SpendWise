
package com.group3.spendwise.auth

import android.content.Context
import com.group3.spendwise.R
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

/**
 * Handles sign-in, registration, password reset and logout using Firebase Authentication.
 * The public properties are Compose state, so the auth screens update automatically
 * whenever they change. Error/info messages are loaded from string resources so they are translated.
 */
class AuthViewModel : ViewModel() {

    // Application context, used to read translated strings outside of Compose.
    private val appContext = FirebaseApp.getInstance().applicationContext
    // Firebase Auth entry point; keeps the user signed in between app launches.
    private val auth = FirebaseAuth.getInstance()

    // Local preferences used by the dashboard (cleared for brand-new accounts).
    private val dashboardPreferences = FirebaseApp.getInstance()
        .applicationContext.getSharedPreferences(
            "spendwise_dashboard",
            Context.MODE_PRIVATE
        )

    // True when a Firebase user is signed in. Read-only from outside (private set).
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

    // True while a Firebase request is running; used to show a spinner on the button.
    var isLoading by mutableStateOf(false)
        private set

    // Error text shown under the form, or null when there is no error.
    var errorMessage by mutableStateOf<String?>(null)
        private set

    // Success/info text (e.g. "reset email sent"), or null.
    var infoMessage by mutableStateOf<String?>(null)
        private set

    /** Removes any error or info message currently shown on screen. */
    fun clearMessages() {
        errorMessage = null
        infoMessage = null
    }

    // Login
    /** Validates the inputs, then signs in with Firebase. On success the app switches to the main screen. */
    fun login(email: String, password: String) {
        clearMessages()

        val cleanEmail = email.trim()

        // Stop early if a field is empty or the email is not valid, so we don't call Firebase for nothing.
        if (cleanEmail.isEmpty() || password.isEmpty()) {
            errorMessage = appContext.getString(R.string.error_auth_empty)
            return
        }

        if (!isValidEmail(cleanEmail)) {
            errorMessage = appContext.getString(R.string.error_auth_email)
            return
        }

        isLoading = true

        // Firebase runs the request in the background and calls this listener when it finishes.
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
    /**
     * Validates the form, creates the Firebase account, then saves the user's full name
     * as their Firebase display name.
     */
    fun register(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        clearMessages()

        val name = fullName.trim()
        val cleanEmail = email.trim()

        // registrationError returns a message when validation fails, or null when everything is OK.
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

                // Save the display name; refreshUser() runs afterwards so the dashboard shows the name.
                user.updateProfile(profile)
                    .addOnCompleteListener {
                        isLoading = false
                        refreshUser()
                    }
            }
    }

    // Password reset
    /** Sends a Firebase password reset email to [email]. */
    fun sendPasswordReset(email: String) {
        clearMessages()

        val cleanEmail = email.trim()

        if (!isValidEmail(cleanEmail)) {
            errorMessage =
                appContext.getString(R.string.error_auth_reset_hint)
            return
        }

        isLoading = true

        auth.sendPasswordResetEmail(cleanEmail)
            .addOnCompleteListener { task ->
                isLoading = false

                if (task.isSuccessful) {
                    infoMessage =
                        appContext.getString(R.string.auth_reset_sent)
                } else {
                    errorMessage = friendlyError(task.exception)
                }
            }
    }

    // Logout
    /** Signs the user out and returns to the login screen. */
    fun logout() {
        auth.signOut()
        clearMessages()
        refreshUser()
    }

    // Update authenticated user information
    // Copies the current Firebase user into the Compose state so the UI recomposes.
    private fun refreshUser() {
        val user = auth.currentUser

        isLoggedIn = user != null
        userName = user?.displayName.orEmpty()
        userEmail = user?.email.orEmpty()
    }

    // Email validation
    // Uses Android's built-in email pattern to check the format.
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Registration validation
    // Returns the first validation problem found, or null if the form is valid.
    // Passwords must be at least 8 characters and contain a digit.
    private fun registrationError(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? = when {

        name.isEmpty() ->
            appContext.getString(R.string.error_auth_name)

        !isValidEmail(email) ->
            appContext.getString(R.string.error_auth_email)

        password.length < 8 || !password.any { it.isDigit() } ->
            appContext.getString(R.string.error_auth_password_rule)

        password != confirmPassword ->
            appContext.getString(R.string.error_auth_password_match)

        else -> null
    }

    // Firebase error messages
    // Turns a Firebase exception into a short, user-friendly (translated) message.
    private fun friendlyError(e: Exception?): String = when (e) {

        is FirebaseAuthInvalidUserException ->
            appContext.getString(R.string.error_auth_user_missing)

        is FirebaseAuthInvalidCredentialsException ->
            appContext.getString(R.string.error_auth_credentials)

        is FirebaseAuthUserCollisionException ->
            appContext.getString(R.string.error_auth_collision)

        is FirebaseNetworkException ->
            appContext.getString(R.string.error_auth_network)

        is FirebaseTooManyRequestsException ->
            appContext.getString(R.string.error_auth_rate_limit)

        else ->
            appContext.getString(R.string.error_auth_generic)
    }
}
