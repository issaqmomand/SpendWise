
package com.group3.spendwise.ui.screens

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
@Composable
fun BudgetAlertObserver(
    ownerUid: String,
    transactions: List<SpendTransaction>,
    budgetTabVisible: Boolean
) {
    val context = LocalContext.current

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
    var preferencesRevision by remember {
        mutableIntStateOf(0)
    }

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

    val month = YearMonth.now()
    val monthKey = month.toString()

    val budgetKey = "budget_${ownerUid}_$monthKey"
    val alertKey = "alert90_$ownerUid"

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

    val permissionGranted =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionRevision++
    }

    var permissionRequested by rememberSaveable(ownerUid) {
        mutableStateOf(false)
    }

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
        Log.d(
            "SpendWiseBudget",
            "Budget check: budget=$monthlyBudget " +
                    "expenses=$monthlyExpenses " +
                    "alerts=$alertsEnabled " +
                    "permission=$permissionGranted " +
                    "tab=$budgetTabVisible"
        )

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
