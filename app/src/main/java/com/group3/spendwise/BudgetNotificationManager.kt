
package com.group3.spendwise

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
import kotlin.math.roundToLong

// Sends budget alerts when spending or the budget changes.
class BudgetNotificationManager(context: Context) {

    private val appContext = context.applicationContext

    private val preferences = appContext.getSharedPreferences(
        "spendwise_budget_notifications",
        Context.MODE_PRIVATE
    )

    private val notificationManager =
        NotificationManagerCompat.from(appContext)

    companion object {
        private const val CHANNEL_ID = "spendwise_budget_alerts"
        private const val CHANNEL_NAME = "Budget Alerts"

        private const val BELOW_WARNING = 0
        private const val WARNING_90 = 1
        private const val EXCEEDED_100 = 2
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Notifications when spending approaches or exceeds your budget"
            }

            val manager = appContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

    // Called when budget settings or transaction totals change.
    fun checkBudget(
        ownerUid: String,
        monthKey: String,
        monthlyBudget: Double,
        monthlyExpenses: Double,
        alertsEnabled: Boolean
    ) {
        if (!alertsEnabled || ownerUid.isBlank()) return
        if (!monthlyBudget.isFinite() || monthlyBudget <= 0.0) return
        if (!monthlyExpenses.isFinite()) return
        if (!hasNotificationPermission()) return

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

        var nextNotificationId: Int? = null

        if (shouldNotify) {

            val displayMonth = runCatching {
                YearMonth.parse(monthKey).format(
                    DateTimeFormatter.ofPattern(
                        "MMMM yyyy",
                        Locale.CANADA
                    )
                )
            }.getOrDefault(monthKey)

            val currency = NumberFormat.getCurrencyInstance(
                Locale.CANADA
            )

            val spentText = currency.format(monthlyExpenses)
            val budgetText = currency.format(monthlyBudget)

            val title: String
            val message: String

            if (currentLevel == EXCEEDED_100) {
                title = "Monthly Budget Exceeded"

                message =
                    "You've spent $spentText of your " +
                            "$budgetText budget for $displayMonth."

            } else {
                title = "Budget Warning"

                message =
                    "You've used at least 90% of your " +
                            "$budgetText budget for $displayMonth. " +
                            "Spent so far: $spentText."
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

    @SuppressLint("MissingPermission")
    private fun showNotification(
        title: String,
        message: String,
        notificationId: Int
    ): Boolean {

        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(appContext.packageName)
            ?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }

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
