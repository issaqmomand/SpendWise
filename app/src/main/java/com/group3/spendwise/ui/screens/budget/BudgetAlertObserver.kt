
package com.group3.spendwise.ui.screens.budget

import com.group3.spendwise.model.SpendTransaction
import com.group3.spendwise.model.TransactionKind

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.group3.spendwise.BudgetNotificationManager
import java.time.LocalDate
import java.time.YearMonth

// Monitors the monthly budget without changing the Budget screen.
/**
 * Invisible composable (draws nothing) placed in MainScreen. It recalculates this month's
 * spending whenever transactions or budget settings change and asks
 * [BudgetNotificationManager] to send a budget alert if needed.
 * It also asks for the notification permission on Android 13+ the first time the Budget tab is opened with alerts on.
 * @param ownerUid Signed-in user's ID.
 * @param transactions All of the user's transactions.
 * @param budgetTabVisible True while the Budget tab is selected.
 */
@Composable
fun BudgetAlertObserver(
    ownerUid: String,
    transactions: List<SpendTransaction>,
    budgetTabVisible: Boolean
) {
    val context = LocalContext.current

    // Same preferences file the Budget screen writes the monthly budget into.
    val preferences = remember(context) {
        context.getSharedPreferences(
            "spendwise_monthly_budgets",
            Context.MODE_PRIVATE
        )
    }

    val notificationManager = remember(context) {
        BudgetNotificationManager(context)
    }

    // Refresh when budget settings change.
    // Incrementing this counter forces the budget check below to run again.
    var preferencesRevision by remember {
        mutableIntStateOf(0)
    }

    // Listen for budget or alert setting changes; the listener is removed when this composable leaves the screen.
    DisposableEffect(preferences) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (
                    key?.startsWith("budget_") == true ||
                    key?.startsWith("alert90_") == true
                ) {
                    preferencesRevision++
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Refresh after returning from Android Settings.
    var resumeRevision by remember {
        mutableIntStateOf(0)
    }

    // Bumped each time the activity resumes, e.g. after the user enables notifications in Android Settings.
    val activity = context as? Activity

    DisposableEffect(activity) {
        val callbacks = object : Application.ActivityLifecycleCallbacks {

            override fun onActivityResumed(resumedActivity: Activity) {
                if (resumedActivity === activity) {
                    resumeRevision++
                }
            }

            override fun onActivityCreated(
                a: Activity,
                b: Bundle?
            ) = Unit

            override fun onActivityStarted(a: Activity) = Unit

            override fun onActivityPaused(a: Activity) = Unit

            override fun onActivityStopped(a: Activity) = Unit

            override fun onActivitySaveInstanceState(
                a: Activity,
                b: Bundle
            ) = Unit

            override fun onActivityDestroyed(a: Activity) = Unit
        }

        activity?.application?.registerActivityLifecycleCallbacks(callbacks)

        onDispose {
            activity?.application?.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }

    // Current month, e.g. "2026-11"; used to build the preference keys.
    val month = YearMonth.now()
    val monthKey = month.toString()

    val budgetKey = "budget_${ownerUid}_$monthKey"
    val alertKey = "alert90_$ownerUid"

    // Read this month's budget. Missing, invalid or non-positive values become 0.0 (no budget set).
    val monthlyBudget = preferences.getString(
        budgetKey,
        null
    )
        ?.toDoubleOrNull()
        ?.takeIf {
            it.isFinite() && it > 0.0
        }
        ?: 0.0

    val alertsEnabled = preferences.getBoolean(
        alertKey,
        false
    )

    // Add up all expenses dated in the current month. Dates that fail to parse are skipped.
    val monthlyExpenses = transactions.asSequence()
        .filter {
            it.kind == TransactionKind.EXPENSE
        }
        .filter {
            runCatching {
                YearMonth.from(LocalDate.parse(it.date))
            }.getOrNull() == month
        }
        .sumOf {
            it.amount
        }

    var permissionRevision by remember {
        mutableIntStateOf(0)
    }

    // Notifications need runtime permission only on Android 13 (TIRAMISU) and newer.
    val permissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    // Shows the system permission dialog; when it closes, bump the counter to re-run the check.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionRevision++
    }

    // Make sure we only ask once per user (saved across rotation).
    var permissionRequested by rememberSaveable(ownerUid) {
        mutableStateOf(false)
    }

    // Ask for notification permission when the user opens the Budget tab with alerts on but no permission yet.
    LaunchedEffect(
        budgetTabVisible,
        alertsEnabled,
        permissionGranted,
        permissionRequested
    ) {
        if (
            ownerUid.isNotBlank() &&
            budgetTabVisible &&
            alertsEnabled &&
            !permissionGranted &&
            !permissionRequested &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            permissionRequested = true

            permissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    // Recheck when settings or transactions change.
    LaunchedEffect(
        ownerUid,
        monthKey,
        budgetTabVisible,
        monthlyBudget,
        monthlyExpenses,
        alertsEnabled,
        permissionGranted,
        preferencesRevision,
        resumeRevision,
        permissionRevision
    ) {
        // Debug log so the budget check can be followed in Logcat.
        Log.d(
            "SpendWiseBudget",
            "Budget check: budget=$monthlyBudget " +
                    "expenses=$monthlyExpenses " +
                    "alerts=$alertsEnabled " +
                    "permission=$permissionGranted " +
                    "tab=$budgetTabVisible"
        )

        // Nothing to check without a user, permission, alerts enabled, and a budget.
        if (
            ownerUid.isBlank() ||
            !permissionGranted ||
            !alertsEnabled ||
            monthlyBudget <= 0.0
        ) {
            return@LaunchedEffect
        }

        // Retry a previously marked 90% alert once after this fix.
        if (monthlyExpenses >= monthlyBudget * 0.90) {
            val history = context.getSharedPreferences(
                "spendwise_budget_notifications",
                Context.MODE_PRIVATE
            )

            val retryKey =
                "retry_after_settings_fix_${ownerUid}_$monthKey"

            if (!history.getBoolean(retryKey, false)) {
                val warningKey =
                    "${ownerUid}_${monthKey}_warning90"

                history.edit()
                    .remove(warningKey)
                    .putBoolean(retryKey, true)
                    .apply()
            }
        }

        notificationManager.checkBudget(
            ownerUid = ownerUid,
            monthKey = monthKey,
            monthlyBudget = monthlyBudget,
            monthlyExpenses = monthlyExpenses,
            alertsEnabled = alertsEnabled
        )
    }
}
