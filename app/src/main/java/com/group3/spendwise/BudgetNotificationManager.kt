
package com.group3.spendwise

import com.group3.spendwise.R

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Currency
import kotlin.math.roundToLong

// Sends budget alerts when spending or the budget changes.
class BudgetNotificationManager(context: Context) {

    // Use the application context so this class never holds on to a screen/Activity.
    private val appContext = context.applicationContext

    // Remembers the last alert level per user and month, so we don't send the same alert twice.
    private val preferences = appContext.getSharedPreferences(
        "spendwise_budget_notifications",
        Context.MODE_PRIVATE
    )

    private val notificationManager =
        NotificationManagerCompat.from(appContext)

    companion object {
        // Notification channel ID (required on Android 8.0+).
        private const val CHANNEL_ID = "spendwise_budget_alerts"


        // Alert levels, in increasing order so they can be compared with > and <.
        private const val BELOW_WARNING = 0
        private const val WARNING_90 = 1
        private const val EXCEEDED_100 = 2
    }

    init {
        createNotificationChannel()
    }

    // Android 8.0 (API 26) and newer require a channel before any notification can be shown.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notifications_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    appContext.getString(R.string.notifications_channel_description)
            }

            val manager = appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    // Called when budget settings or transaction totals change.
    /**
     * Decides whether to send a "close to budget" (90%) or "over budget" (100%) notification.
     * Alerts are only sent when something meaningful changes: a higher level is reached,
     * the budget amount changes, or spending goes up while already over budget.
     * @param ownerUid Signed-in user's ID, so each user has separate alert history.
     * @param monthKey Month being checked, in yyyy-MM format.
     * @param monthlyBudget Budget set for that month.
     * @param monthlyExpenses Total expenses recorded for that month.
     * @param alertsEnabled The user's setting for budget alerts.
     */
    fun checkBudget(
        ownerUid: String,
        monthKey: String,
        monthlyBudget: Double,
        monthlyExpenses: Double,
        alertsEnabled: Boolean
    ) {
        // Skip if alerts are off, the input is invalid, or we are not allowed to post notifications.
        if (!alertsEnabled || ownerUid.isBlank()) return
        if (!monthlyBudget.isFinite() || monthlyBudget <= 0.0) return
        if (!monthlyExpenses.isFinite()) return
        if (!hasNotificationPermission()) return

        // All saved keys start with user + month so every month starts fresh.
        val keyPrefix = "${ownerUid}_${monthKey}"

        // Compare currency values to the nearest cent.
        val budgetCents =
            (monthlyBudget * 100.0).roundToLong()

        val spentCents =
            (monthlyExpenses * 100.0).roundToLong()

        // Determine the current alert level.
        val currentLevel = when {
            spentCents >= budgetCents -> EXCEEDED_100
            spentCents >= budgetCents * 0.90 -> WARNING_90
            else -> BELOW_WARNING
        }

        // Read the state from our previous budget check.
        val levelKey = "${keyPrefix}_currentLevel"
        val budgetKey = "${keyPrefix}_lastBudgetCents"
        val spentKey = "${keyPrefix}_lastSpentCents"

        val previousLevel = preferences.getInt(
            levelKey,
            BELOW_WARNING
        )

        val previousBudget = if (preferences.contains(budgetKey)) {
            preferences.getLong(budgetKey, 0L)
        } else {
            null
        }

        val previousSpent = if (preferences.contains(spentKey)) {
            preferences.getLong(spentKey, 0L)
        } else {
            null
        }

        // The user changed their monthly budget.
        val budgetChanged =
            previousBudget != null &&
                    previousBudget != budgetCents

        // Spending crossed into a higher warning level.
        val crossedThreshold =
            currentLevel > previousLevel

        // A new expense increased spending while already over budget.
        val increasedWhileOverBudget =
            currentLevel == EXCEEDED_100 &&
                    previousLevel == EXCEEDED_100 &&
                    previousSpent != null &&
                    spentCents > previousSpent

        // Allow an initial alert after installing this update.
        val initialAlert =
            previousBudget == null &&
                    currentLevel != BELOW_WARNING

        val shouldNotify =
            currentLevel != BELOW_WARNING &&
                    (
                            initialAlert ||
                                    crossedThreshold ||
                                    budgetChanged ||
                                    increasedWhileOverBudget
                            )

        // Only set if a notification was actually posted.
        var nextNotificationId: Int? = null

        if (shouldNotify) {

            // Format the month name (e.g. "November 2026") in the user's language; fall back to the raw key.
            val displayMonth = runCatching {
                YearMonth.parse(monthKey).format(
                    DateTimeFormatter.ofPattern(
                        "MMMM yyyy",
                        appContext.resources.configuration.locales[0]
                    )
                )
            }.getOrDefault(monthKey)

            // Amounts are shown in Canadian dollars using the device locale's formatting.
            val currency = NumberFormat.getCurrencyInstance(
                appContext.resources.configuration.locales[0]
            ).apply {
                currency = Currency.getInstance("CAD")
            }

            val spentText = currency.format(monthlyExpenses)
            val budgetText = currency.format(monthlyBudget)

            val title: String
            val message: String

            if (currentLevel == EXCEEDED_100) {
                title = appContext.getString(R.string.notifications_exceeded_title)
                message = appContext.getString(R.string.notifications_exceeded_body, spentText, budgetText, displayMonth)

            } else {
                title = appContext.getString(R.string.notifications_warning_title)
                message = appContext.getString(R.string.notifications_warning_body, budgetText, displayMonth, spentText)
            }

            // A fresh notification ID creates a new alert
            // instead of silently updating the previous one.
            val notificationId = preferences.getInt(
                "nextNotificationId",
                5000
            ) + 1

            val posted = showNotification(
                title = title,
                message = message,
                notificationId = notificationId
            )

            // Retry later if posting failed.
            if (!posted) return

            nextNotificationId = notificationId
        }

        // Store the current state after checking the thresholds.
        preferences.edit().apply {
            putInt(levelKey, currentLevel)
            putLong(budgetKey, budgetCents)
            putLong(spentKey, spentCents)

            if (nextNotificationId != null) {
                putInt(
                    "nextNotificationId",
                    nextNotificationId
                )
            }
        }.apply()
    }

    /** Returns true if notifications are enabled and, on Android 13+, the POST_NOTIFICATIONS permission is granted. */
    private fun hasNotificationPermission(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            return ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        return true
    }

    /**
     * Builds and posts the notification. Tapping it opens the app.
     * Returns false if posting failed (e.g. permission was revoked).
     */
    @SuppressLint("MissingPermission")
    private fun showNotification(
        title: String,
        message: String,
        notificationId: Int
    ): Boolean {

        // Intent that brings the existing app screen to the front instead of starting a new copy.
        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

        // FLAG_IMMUTABLE is required for PendingIntents on Android 12+.
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                appContext,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(
            appContext,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_dialog_info
            )
            .setContentTitle(title)
            .setContentText(message)
            // BigTextStyle lets the full message show when the notification is expanded.
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
            )
            .setPriority(
                NotificationCompat.PRIORITY_DEFAULT
            )
            .setCategory(
                NotificationCompat.CATEGORY_REMINDER
            )
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        return try {
            notificationManager.notify(
                notificationId,
                builder.build()
            )
            true

        } catch (_: SecurityException) {
            false
        }
    }
}
