package com.group3.spendwise.ui.screens.budget

import com.group3.spendwise.R
import androidx.compose.ui.res.stringResource
import com.group3.spendwise.ui.components.localizedCategory

import com.group3.spendwise.model.SpendTransaction
import com.group3.spendwise.model.TransactionKind

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Budget colors follow the existing SpendWise light/dark MaterialTheme.
private data class BudgetPalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val muted: Color,
    val sage: Color,
    val error: Color,
    val warning: Color,
    val surface: Color,
    val warningBackground: Color,
    val exceededBackground: Color,
    val exceededTrack: Color
)

@Composable
private fun budgetPalette(): BudgetPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    return if (isDark) {
        BudgetPalette(
            background = scheme.background,
            foreground = scheme.onSurface,
            accent = scheme.primary,
            muted = scheme.onSurfaceVariant,
            sage = scheme.secondaryContainer,
            error = scheme.error,
            warning = Color(0xFFFFD38D),
            surface = scheme.surface,
            warningBackground = Color(0xFF493C27),
            exceededBackground = Color(0xFF452C33),
            exceededTrack = Color(0xFF60434A)
        )
    } else {
        // Exact original SpendWise Budget colors in light mode.
        BudgetPalette(
            background = Color(0xFFF6F7F4),
            foreground = Color(0xFF203846),
            accent = Color(0xFF477A62),
            muted = Color(0xFF7A8880),
            sage = Color(0xFFE7EFE7),
            error = Color(0xFFD23838),
            warning = Color(0xFF987127),
            surface = Color.White,
            warningBackground = Color(0xFFF9F1DE),
            exceededBackground = Color(0xFFFFF7F7),
            exceededTrack = Color(0xFFFFE2E2)
        )
    }
}

// Currency formatting.
private fun budgetMoney(value: Double): String {
    return String.format(Locale.CANADA, "$%,.2f", value)
}

// Category colors.
private fun categoryDot(category: String): Color {
    return when (category.lowercase(Locale.getDefault())) {
        "food", "groceries" -> Color(0xFF688B70)
        "transportation", "transport" -> Color(0xFF90B1C1)
        "shopping" -> Color(0xFFBCAE88)
        "entertainment" -> Color(0xFF9C9AB6)
        "other" -> Color(0xFFCED9C7)
        "school", "education" -> Color(0xFF9C9AB6)
        "housing", "rent" -> Color(0xFFBCAE88)
        "subscriptions" -> Color(0xFF90B1C1)
        else -> Color(0xFF688B70)
    }
}

@Composable
fun BudgetScreen(
    transactions: List<SpendTransaction>,
    ownerUid: String
) {
    val context = LocalContext.current
    val colors = budgetPalette()

    // Save budgets separately for each user and month.
    val preferences = remember(context) {
        context.getSharedPreferences(
            "spendwise_monthly_budgets",
            Context.MODE_PRIVATE
        )
    }

    var monthOffset by rememberSaveable(ownerUid) {
        mutableIntStateOf(0)
    }

    var monthMenuOpen by remember {
        mutableStateOf(false)
    }

    val selectedMonth = YearMonth.now()
        .plusMonths(monthOffset.toLong())

    val monthKey = selectedMonth.toString()
    val budgetKey = "budget_${ownerUid}_$monthKey"
    val alertKey = "alert90_$ownerUid"

    // Load the selected month's budget.
    var target by remember(ownerUid, monthKey) {
        mutableStateOf(
            preferences.getString(
                budgetKey,
                null
            )?.toDoubleOrNull()?.takeIf {
                it.isFinite() && it > 0.0
            }
        )
    }

    var targetInput by remember(ownerUid, monthKey) {
        mutableStateOf(
            target?.let {
                String.format(Locale.US, "%.2f", it)
            } ?: ""
        )
    }

    // Only the editor dialog is shown when the budget button is tapped.
    var budgetDialogOpen by remember(ownerUid, monthKey) {
        mutableStateOf(false)
    }

    var budgetErrorText by remember(ownerUid, monthKey) {
        mutableStateOf("")
    }

    var alertsEnabled by remember(ownerUid) {
        mutableStateOf(
            preferences.getBoolean(alertKey, false)
        )
    }

    // Transactions for the selected month.
    val monthTransactions = transactions.filter { transaction ->
        runCatching {
            YearMonth.from(
                LocalDate.parse(transaction.date)
            )
        }.getOrNull() == selectedMonth
    }

    val monthExpenses = monthTransactions.filter {
        it.kind == TransactionKind.EXPENSE
    }

    val spent = monthExpenses.sumOf { it.amount }
    val monthlyIncome = monthTransactions
        .filter { it.kind == TransactionKind.INCOME }
        .sumOf { it.amount }

    // Monthly budget calculations.
    val remaining = target?.minus(spent)

    val percent = target?.let {
        spent / it * 100.0
    }

    val progress = target?.let {
        (spent / it).toFloat().coerceIn(0f, 1f)
    } ?: 0f

    val isExceeded = target?.let {
        spent > it
    } ?: false

    val nearLimit = target?.let {
        spent >= it * 0.9 && spent <= it
    } ?: false

    val meterColor = when {
        isExceeded -> colors.error
        nearLimit -> colors.warning
        else -> colors.accent
    }

    // Category spending calculated using Room transactions.
    val categoryTotals = monthExpenses
        .groupBy {
            it.category.ifBlank { "Other" }
        }
        .mapValues { (_, items) ->
            items.sumOf { it.amount }
        }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 48.dp
            ),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.budget_title),
                    fontSize = 32.sp,
                    color = colors.foreground
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = stringResource(R.string.budget_tagline),
                    fontSize = 14.sp,
                    color = colors.muted
                )
            }

            Text(
                text = stringResource(R.string.currency_cad),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // MONTH SELECTOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            MonthArrowButton(
                previous = true,
                onClick = {
                    monthOffset--
                    monthMenuOpen = false
                }
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            monthMenuOpen = true
                        }
                        .padding(
                            horizontal = 5.dp,
                            vertical = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(Modifier.width(7.dp))

                    Text(
                        text = selectedMonth.format(
                            DateTimeFormatter.ofPattern(
                                "MMMM yyyy",
                                Locale.getDefault()
                            )
                        ),
                        color = colors.foreground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )

                    Spacer(Modifier.width(5.dp))

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.budget_choose_month),
                        tint = colors.foreground,
                        modifier = Modifier.size(19.dp)
                    )
                }

                DropdownMenu(
                    expanded = monthMenuOpen,
                    onDismissRequest = {
                        monthMenuOpen = false
                    }
                ) {
                    (-12..3).reversed().forEach { offset ->
                        val month = YearMonth.now()
                            .plusMonths(offset.toLong())

                        DropdownMenuItem(
                            text = {
                                Text(
                                    month.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMMM yyyy",
                                            Locale.getDefault()
                                        )
                                    )
                                )
                            },
                            onClick = {
                                monthOffset = offset
                                monthMenuOpen = false
                            }
                        )
                    }
                }
            }

            MonthArrowButton(
                previous = false,
                onClick = {
                    monthOffset++
                    monthMenuOpen = false
                }
            )
        }

        // MONTHLY BUDGET OVERVIEW
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BudgetRing(
                        progress = progress,
                        label = percent?.let {
                            String.format(
                                Locale.CANADA,
                                "%.1f%%",
                                it
                            )
                        } ?: "—",
                        color = meterColor,
                        modifier = Modifier.size(132.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.budget_title),
                            fontSize = 14.sp,
                            color = colors.muted
                        )

                        Text(
                            text = target?.let(::budgetMoney)
                                ?: stringResource(R.string.budget_not_set),
                            fontSize = if (target == null) {
                                23.sp
                            } else {
                                27.sp
                            },
                            color = colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(3.dp))

                        BudgetAmountLine(
                            label = stringResource(R.string.budget_total_spent),
                            value = budgetMoney(spent),
                            valueColor = colors.foreground
                        )

                        BudgetAmountLine(
                            label = stringResource(R.string.budget_remaining),
                            value = remaining?.let(::budgetMoney)
                                ?: "—",
                            valueColor = if (
                                remaining != null &&
                                remaining < 0.0
                            ) {
                                colors.error
                            } else {
                                colors.accent
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.budget_used),
                        color = colors.muted,
                        fontSize = 14.sp
                    )

                    Text(
                        text = percent?.let {
                            String.format(
                                Locale.CANADA,
                                "%.1f%%",
                                it
                            )
                        } ?: "—",
                        color = meterColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = meterColor,
                    trackColor = colors.sage
                )

                if (target == null) {
                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = stringResource(R.string.budget_set_prompt),
                        color = colors.muted,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // CATEGORY SPENDING
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = stringResource(R.string.budget_category_spending),
                    color = colors.foreground,
                    fontSize = 20.sp
                )

                if (categoryTotals.isEmpty()) {
                    Text(
                        text = stringResource(R.string.budget_no_expenses),
                        color = colors.muted,
                        fontSize = 14.sp
                    )
                } else {
                    categoryTotals.forEach { (category, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .background(
                                        categoryDot(category),
                                        CircleShape
                                    )
                            )

                            Spacer(Modifier.width(10.dp))

                            Text(
                                text = localizedCategory(category),
                                color = colors.foreground,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = budgetMoney(amount),
                                color = colors.foreground,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // SET / EDIT MONTHLY BUDGET
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(13.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colors.sage,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            targetInput = target?.let {
                                String.format(Locale.US, "%.2f", it)
                            } ?: ""
                            budgetErrorText = ""
                            budgetDialogOpen = true
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.budget_edit),
                        color = colors.accent,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ALERT ME AT 90%
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    colors.warningBackground,
                    RoundedCornerShape(17.dp)
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 5.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(21.dp)
            )

            Spacer(Modifier.width(9.dp))

            Text(
                text = stringResource(R.string.budget_alert_90),
                color = colors.warning,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = alertsEnabled,
                onCheckedChange = { enabled ->
                    alertsEnabled = enabled

                    if (ownerUid.isNotBlank()) {
                        preferences.edit()
                            .putBoolean(
                                alertKey,
                                enabled
                            )
                            .apply()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.accent
                )
            )
        }

        // BUDGET EXCEEDED WARNING
        // Displays only the red warning box.
        if (isExceeded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.surface,
                        RoundedCornerShape(23.dp)
                    )
                    .padding(13.dp)
            ) {

                // Removed the "If budget is exceeded"
                // heading and the "Exceeded" badge.

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colors.exceededBackground,
                            RoundedCornerShape(17.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VolumeUp,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(23.dp)
                        )

                        Spacer(Modifier.width(9.dp))

                        Text(
                            text = stringResource(R.string.budget_exceeded_message),
                            color = colors.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = stringResource(R.string.budget_exceeded_amounts, budgetMoney(spent), budgetMoney(target ?: 0.0), budgetMoney(spent - (target ?: 0.0))),
                        color = colors.error,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.budget_used),
                            color = colors.muted,
                            fontSize = 13.sp
                        )

                        Text(
                            text = String.format(
                                Locale.CANADA,
                                "%.1f%%",
                                percent ?: 0.0
                            ),
                            color = colors.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        color = colors.error,
                        trackColor = colors.exceededTrack
                    )
                }
            }
        } else if (alertsEnabled && nearLimit) {
            Text(
                text = stringResource(R.string.budget_warning_used, String.format(Locale.getDefault(), "%.1f", percent ?: 0.0)),
                color = colors.warning,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.warningBackground,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
    }

    // Editor opens only after tapping Set/Edit Monthly Budget.
    if (budgetDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                budgetDialogOpen = false
                budgetErrorText = ""
            },
            title = {
                Text(
                    text = stringResource(R.string.budget_edit),
                    color = colors.foreground,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.budget_monthly_income, budgetMoney(monthlyIncome)),
                        color = colors.muted,
                        fontSize = 14.sp
                    )

                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { value ->
                            if (
                                value.isEmpty() ||
                                value.matches(
                                    Regex("\\d{0,9}(\\.\\d{0,2})?")
                                )
                            ) {
                                targetInput = value
                                budgetErrorText = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = stringResource(R.string.budget_amount_cad),
                                fontSize = 12.sp
                            )
                        },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = budgetErrorText.isNotEmpty(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.sage
                        )
                    )

                    if (budgetErrorText.isNotEmpty()) {
                        Text(
                            text = budgetErrorText,
                            color = colors.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = targetInput.toDoubleOrNull()
                        budgetErrorText = when {
                            ownerUid.isBlank() ->
                                context.getString(R.string.budget_sign_in_error)
                            amount == null || !amount.isFinite() || amount <= 0.0 ->
                                context.getString(R.string.budget_positive_error)
                            monthlyIncome <= 0.0 ->
                                context.getString(R.string.budget_income_required)
                            amount > monthlyIncome ->
                                context.getString(R.string.budget_income_max_error, budgetMoney(monthlyIncome))
                            else -> {
                                preferences.edit()
                                    .putString(budgetKey, amount.toString())
                                    .apply()
                                target = amount
                                targetInput = String.format(Locale.US, "%.2f", amount)
                                budgetDialogOpen = false
                                ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        budgetDialogOpen = false
                        budgetErrorText = ""
                    }
                ) {
                    Text(stringResource(R.string.action_cancel), color = colors.muted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// Separate white month arrow buttons.
@Composable
private fun MonthArrowButton(
    previous: Boolean,
    onClick: () -> Unit
) {
    val colors = budgetPalette()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(
                colors.surface,
                RoundedCornerShape(15.dp)
            )
    ) {
        Icon(
            imageVector = if (previous) {
                Icons.Outlined.ChevronLeft
            } else {
                Icons.Outlined.ChevronRight
            },
            contentDescription = if (previous) {
                stringResource(R.string.budget_prev_month)
            } else {
                stringResource(R.string.budget_next_month)
            },
            tint = colors.foreground,
            modifier = Modifier.size(26.dp)
        )
    }
}

// Circular budget progress indicator.
@Composable
private fun BudgetRing(
    progress: Float,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = budgetPalette()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val stroke = 14.dp.toPx()
            val diameter = size.minDimension - stroke

            drawCircle(
                color = colors.sage,
                radius = diameter / 2f,
                style = Stroke(width = stroke)
            )

            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = Offset(
                        stroke / 2f,
                        stroke / 2f
                    ),
                    size = Size(
                        diameter,
                        diameter
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Butt
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = label,
                color = colors.foreground,
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Text(
                text = stringResource(R.string.budget_used_short),
                color = colors.muted,
                fontSize = 13.sp
            )
        }
    }
}

// Compact label/value row.
@Composable
private fun BudgetAmountLine(
    label: String,
    value: String,
    valueColor: Color
) {
    val colors = budgetPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.muted,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

