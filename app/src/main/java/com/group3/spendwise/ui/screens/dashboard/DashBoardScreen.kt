
package com.group3.spendwise.ui.screens.dashboard

import com.group3.spendwise.R
import androidx.compose.ui.res.stringResource
import com.group3.spendwise.ui.components.localizedCategory

import com.group3.spendwise.model.SpendTransaction
import com.group3.spendwise.model.TransactionKind

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group3.spendwise.ui.theme.SpendWarning
import com.group3.spendwise.ui.components.AppLanguageDialog
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

// Dashboard colors.
private val balanceNavy = Color(0xFF203846)
private val balanceSecondary = Color(0xFFDCE7E2)
private val balanceCheck = Color(0xFFA9D2B5)

// Placeholder shown instead of amounts when the user hides their numbers.
private const val HIDDEN_VALUE = "••••••"

// Currency formatting.
// Formats an amount as Canadian dollars, e.g. "$1,234.50".
private fun dashboardMoney(value: Double): String {
    return String.format(
        Locale.CANADA,
        "\$%,.2f",
        value
    )
}

// Date formatting.
// Short date for the recent list, e.g. "Nov 23". Falls back to the raw string if parsing fails.
private fun dashboardDate(date: String): String {
    return runCatching {
        LocalDate.parse(date).format(
            DateTimeFormatter.ofPattern(
                "MMM d",
                Locale.getDefault()
            )
        )
    }.getOrDefault(date)
}

// Category icons for recent transactions.
// Unknown categories get a generic receipt icon.
private fun dashboardIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.getDefault())) {
        "food", "groceries", "shopping" ->
            Icons.Outlined.ShoppingBasket

        "coffee" ->
            Icons.Outlined.Coffee

        "transportation", "transport" ->
            Icons.Outlined.DirectionsBus

        "school", "education" ->
            Icons.Outlined.School

        "salary", "work", "job" ->
            Icons.Outlined.Work

        else ->
            Icons.Outlined.ReceiptLong
    }
}

/**
 * Home screen shown after login. Shows a greeting, the current balance, this month's income
 * and expenses, how much of this month's income has been spent, and the five latest transactions.
 * The menu in the top-right corner has dark mode, language and logout.
 * All numbers are calculated from the transactions list, so they update automatically.
 * @param transactions All of the signed-in user's transactions.
 * @param onViewTransactions Switches to the Transactions tab.
 * @param onTransactionClick Opens the details screen for a transaction.
 */
@Composable
fun DashboardScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    transactions: List<SpendTransaction>,
    onViewTransactions: () -> Unit,
    onTransactionClick: (SpendTransaction) -> Unit
) {
    // Hide or show every financial number on the Dashboard.
    var numbersVisible by rememberSaveable {
        mutableStateOf(true)
    }

    // Dropdown menu (dark mode / language / logout) and language dialog visibility.
    var showAccountMenu by rememberSaveable {
        mutableStateOf(false)
    }
    var showLanguageDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val colors = MaterialTheme.colorScheme
    val titleColor = colors.onBackground
    val mutedColor = colors.onSurfaceVariant
    val accentColor = colors.primary
    val cardColor = colors.surface

    // CALCULATE MONTHLY TOTALS FROM ROOM TRANSACTIONS

    // Keep only transactions dated in the current month.
    val currentMonth = YearMonth.now()

    val monthTransactions = transactions.filter { transaction ->
        runCatching {
            YearMonth.from(
                LocalDate.parse(transaction.date)
            )
        }.getOrNull() == currentMonth
    }

    val monthlyIncome = monthTransactions
        .filter {
            it.kind == TransactionKind.INCOME
        }
        .sumOf {
            it.amount
        }

    val monthlyExpenses = monthTransactions
        .filter {
            it.kind == TransactionKind.EXPENSE
        }
        .sumOf {
            it.amount
        }

    // Current balance includes transactions from all months.
    val totalIncome = transactions
        .filter {
            it.kind == TransactionKind.INCOME
        }
        .sumOf {
            it.amount
        }

    val totalExpenses = transactions
        .filter {
            it.kind == TransactionKind.EXPENSE
        }
        .sumOf {
            it.amount
        }

    val currentBalance = totalIncome - totalExpenses

    // Show the five latest saved transactions.
    val recentTransactions = transactions
        .sortedWith(
            compareByDescending<SpendTransaction> {
                it.date
            }.thenByDescending {
                it.id
            }
        )
        .take(5)

    // MONTHLY INCOME OVERVIEW

    // How much of this month's income is left after expenses.
    val remainingIncome = monthlyIncome - monthlyExpenses

    // Percentage of income spent; null when there is no income (can't divide by zero).
    val percentSpent = if (monthlyIncome > 0.0) {
        monthlyExpenses / monthlyIncome * 100.0
    } else {
        null
    }

    // Progress bar fill from 0 to 1. Shows full if there are expenses but no income.
    val progress = when {
        monthlyIncome > 0.0 ->
            (monthlyExpenses / monthlyIncome)
                .toFloat()
                .coerceIn(0f, 1f)

        monthlyExpenses > 0.0 -> 1f

        else -> 0f
    }

    // Bar colour: red when overspent or no income, warning colour at 80%+, green otherwise.
    val barColor = when {
        monthlyIncome <= 0.0 &&
                monthlyExpenses > 0.0 ->
            colors.error

        remainingIncome < 0.0 ->
            colors.error

        percentSpent != null &&
                percentSpent >= 80.0 ->
            SpendWarning

        else ->
            accentColor
    }

    // Status sentence under the progress bar.
    val incomeStatus = when {
        monthlyIncome == 0.0 &&
                monthlyExpenses == 0.0 ->
            stringResource(R.string.dashboard_income_prompt)

        monthlyIncome == 0.0 ->
            stringResource(R.string.dashboard_no_income)

        remainingIncome >= 0.0 ->
            stringResource(R.string.dashboard_income_remaining, dashboardMoney(remainingIncome))

        else ->
            stringResource(R.string.dashboard_income_over, dashboardMoney(-remainingIncome))
    }

    val percentLabel = if (percentSpent != null) {
        stringResource(R.string.dashboard_percent_used, String.format(Locale.getDefault(), "%.1f%%", percentSpent))
    } else {
        "—"
    }

    // Greeting based on local time.
    val hour = Calendar.getInstance()
        .get(Calendar.HOUR_OF_DAY)

    val greeting = when {
        hour < 12 -> stringResource(R.string.dashboard_greeting_morning)
        hour < 18 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }

    // Fall back to the email when the user has no display name.
    val displayName = userName.ifBlank {
        userEmail
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 20.dp,
                vertical = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // HEADER AND ACCOUNT MENU
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )

                Text(
                    text = stringResource(R.string.dashboard_signed_in, displayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor
                )
            }

            Box {
                IconButton(
                    onClick = {
                        showAccountMenu = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.account_options),
                        tint = titleColor
                    )
                }

                DropdownMenu(
                    expanded = showAccountMenu,
                    onDismissRequest = {
                        showAccountMenu = false
                    }
                ) {
                    // Toggle between light and dark mode.
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isDarkMode) {
                                    stringResource(R.string.theme_light)
                                } else {
                                    stringResource(R.string.theme_dark)
                                }
                            )
                        },
                        onClick = {
                            showAccountMenu = false
                            onDarkModeChange(!isDarkMode)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isDarkMode) {
                                    Icons.Outlined.LightMode
                                } else {
                                    Icons.Outlined.DarkMode
                                },
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { enabled ->
                                    showAccountMenu = false
                                    onDarkModeChange(enabled)
                                }
                            )
                        }
                    )

                    HorizontalDivider()

                    // Opens the language picker dialog.
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.language_label)) },
                        onClick = {
                            showAccountMenu = false
                            showLanguageDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Language, contentDescription = null)
                        },
                        trailingIcon = {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                        }
                    )

                    HorizontalDivider()

                    // Signs the user out.
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.action_logout))
                        },
                        onClick = {
                            showAccountMenu = false
                            onLogout()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }

        if (showLanguageDialog) {
            AppLanguageDialog(onDismiss = { showLanguageDialog = false })
        }

        // CURRENT BALANCE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = balanceNavy
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 22.dp,
                        vertical = 18.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_current_balance),
                        fontSize = 16.sp,
                        color = balanceSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    // Eye button that hides or shows all amounts on the dashboard.
                    IconButton(
                        onClick = {
                            numbersVisible = !numbersVisible
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (numbersVisible) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = if (numbersVisible) {
                                stringResource(R.string.dashboard_hide_amounts)
                            } else {
                                stringResource(R.string.dashboard_show_amounts)
                            },
                            tint = balanceSecondary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (numbersVisible) {
                        dashboardMoney(currentBalance)
                    } else {
                        HIDDEN_VALUE
                    },
                    fontSize = 40.sp,
                    lineHeight = 47.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(13.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = balanceCheck,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(R.string.app_privacy_line),
                        color = balanceSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // READ-ONLY MONTHLY INCOME AND EXPENSES
        // These values are calculated from transactions.
        // No edit buttons or manual inputs.
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardStatCard(
                label = stringResource(R.string.dashboard_monthly_income),
                value = if (numbersVisible) {
                    dashboardMoney(monthlyIncome)
                } else {
                    HIDDEN_VALUE
                },
                valueColor = accentColor,
                modifier = Modifier.weight(1f)
            )

            DashboardStatCard(
                label = stringResource(R.string.dashboard_monthly_expenses),
                value = if (numbersVisible) {
                    dashboardMoney(monthlyExpenses)
                } else {
                    HIDDEN_VALUE
                },
                valueColor = titleColor,
                modifier = Modifier.weight(1f)
            )
        }

        // MONTHLY INCOME OVERVIEW
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardColor
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_income_overview),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = if (numbersVisible) {
                            percentLabel
                        } else {
                            HIDDEN_VALUE
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    )
                }

                Spacer(Modifier.height(9.dp))

                Text(
                    text = if (numbersVisible) {
                        stringResource(R.string.dashboard_spent_income, dashboardMoney(monthlyExpenses), dashboardMoney(monthlyIncome))
                    } else {
                        stringResource(R.string.dashboard_figures_hidden)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor
                )

                Spacer(Modifier.height(13.dp))

                // Progress bar of income spent; an empty grey bar is shown while numbers are hidden.
                if (numbersVisible) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = barColor,
                        trackColor = colors.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(
                                colors.secondaryContainer,
                                RoundedCornerShape(5.dp)
                            )
                    )
                }

                Spacer(Modifier.height(11.dp))

                Text(
                    text = if (numbersVisible) {
                        incomeStatus
                    } else {
                        stringResource(R.string.dashboard_remaining_hidden)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (
                        !numbersVisible ||
                        remainingIncome >= 0.0
                    ) {
                        accentColor
                    } else {
                        colors.error
                    }
                )
            }
        }

        // RECENT TRANSACTIONS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = cardColor
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_recent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = onViewTransactions
                    ) {
                        Text(stringResource(R.string.dashboard_view_all))
                    }
                }

                if (recentTransactions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.dashboard_no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedColor,
                        modifier = Modifier.padding(
                            vertical = 12.dp
                        )
                    )
                } else {
                    // List the five most recent transactions with dividers between them; tapping one opens its details.
                    recentTransactions.forEachIndexed {
                            index, transaction ->

                        if (index > 0) {
                            HorizontalDivider(
                                color = colors.outlineVariant
                            )
                        }

                        DashboardTransactionRow(
                            transaction = transaction,
                            numbersVisible = numbersVisible,
                            onClick = {
                                onTransactionClick(transaction)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// READ-ONLY MONTHLY SUMMARY CARD
// Pencil icons and editing callbacks removed.
/** Small card showing a label (e.g. "Monthly income") and its value. */
@Composable
private fun DashboardStatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(Modifier.height(9.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// RECENT TRANSACTION ROW
/** One row in the recent transactions list: category icon, description, category/date and signed amount. */
@Composable
private fun DashboardTransactionRow(
    transaction: SpendTransaction,
    numbersVisible: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val isIncome =
        transaction.kind == TransactionKind.INCOME

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    colors.secondaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = dashboardIcon(transaction.category),
                contentDescription = null,
                tint = colors.primary
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (numbersVisible) {
                    "${localizedCategory(transaction.category)} · " +
                            dashboardDate(transaction.date)
                } else {
                    transaction.category
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        Text(
            text = if (numbersVisible) {
                (if (isIncome) "+" else "-") +
                        dashboardMoney(transaction.amount)
            } else {
                HIDDEN_VALUE
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isIncome) {
                colors.primary
            } else {
                colors.onSurface
            }
        )
    }
}
