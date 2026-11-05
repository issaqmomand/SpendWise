package com.group3.spendwise.ui.screens

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// Keep category colors consistent across light and dark themes.
private val insightsGreen = Color(0xFF688B70)
private val insightsBlue = Color(0xFF90B1C1)
private val insightsGold = Color(0xFFBCAE88)
private val insightsPurple = Color(0xFF9C9AB6)
private val insightsPale = Color(0xFFC8D1BF)

// Only colours change in dark mode; layout, charts, and calculations stay the same.
private data class InsightsPalette(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val accent: Color,
    val grid: Color,
    val surface: Color,
    val highlightCard: Color,
    val highlightIconBackground: Color
)

@Composable
private fun insightsPalette(): InsightsPalette {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) {
        InsightsPalette(
            background = scheme.background,
            foreground = scheme.onSurface,
            muted = scheme.onSurfaceVariant,
            accent = scheme.primary,
            grid = scheme.outlineVariant,
            surface = scheme.surface,
            highlightCard = scheme.secondaryContainer,
            highlightIconBackground = scheme.surfaceVariant
        )
    } else {
        // Preserve the exact original Insights appearance in light mode.
        InsightsPalette(
            background = Color(0xFFF6F7F4),
            foreground = Color(0xFF203846),
            muted = Color(0xFF7A8880),
            accent = Color(0xFF477A62),
            grid = Color(0xFFD8DED6),
            surface = Color.White,
            highlightCard = Color(0xFFE7EFE7),
            highlightIconBackground = Color.White
        )
    }
}

// Donut chart category model.
private data class SpendingSlice(
    val category: String,
    val amount: Double,
    val color: Color
)

// Currency formatting.
private fun insightsMoney(value: Double): String {
    return "$" + String.format(
        Locale.CANADA,
        "%,.0f",
        value
    )
}

// Consistent category colors.
private fun categoryColor(category: String): Color {
    return when (category.lowercase(Locale.ENGLISH)) {
        "food", "groceries" -> insightsGreen
        "transportation", "transport" -> insightsBlue
        "shopping" -> insightsGold
        "entertainment" -> insightsPurple
        "other" -> insightsPale
        "subscriptions" -> insightsBlue
        "school", "education" -> insightsPurple
        "housing", "rent" -> insightsGold
        else -> insightsPale
    }
}

// Calculate chart categories from Room transactions.
private fun spendingSlices(
    expenses: List<SpendTransaction>
): List<SpendingSlice> {

    val totals = expenses
        .groupBy {
            it.category.ifBlank { "Other" }
        }
        .map { (category, entries) ->
            category to entries.sumOf { it.amount }
        }
        .filter {
            it.second > 0.0
        }
        .sortedByDescending {
            it.second
        }

    // Show up to four main categories.
    val mainCategories = totals
        .filterNot {
            it.first.equals(
                "Other",
                ignoreCase = true
            )
        }
        .take(4)

    val mainNames = mainCategories
        .map { it.first }
        .toSet()

    // Combine any additional categories into Other.
    val otherAmount = totals
        .filterNot {
            it.first in mainNames
        }
        .sumOf {
            it.second
        }

    return buildList {
        mainCategories.forEach { (category, amount) ->
            add(
                SpendingSlice(
                    category = category,
                    amount = amount,
                    color = categoryColor(category)
                )
            )
        }

        if (otherAmount > 0.0) {
            add(
                SpendingSlice(
                    category = "Other",
                    amount = otherAmount,
                    color = insightsPale
                )
            )
        }
    }
}

@Composable
fun InsightsScreen(
    transactions: List<SpendTransaction>
) {
    val colors = insightsPalette()
    var monthOffset by rememberSaveable {
        mutableIntStateOf(0)
    }

    var monthMenuOpen by remember {
        mutableStateOf(false)
    }

    val selectedMonth = YearMonth.now()
        .plusMonths(monthOffset.toLong())

    val monthName = selectedMonth.format(
        DateTimeFormatter.ofPattern(
            "MMMM",
            Locale.ENGLISH
        )
    )

    val monthLabel = selectedMonth.format(
        DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            Locale.ENGLISH
        )
    )

    // Read current selected month's Room transactions.
    val monthTransactions = transactions.filter { transaction ->
        runCatching {
            YearMonth.from(
                LocalDate.parse(transaction.date)
            )
        }.getOrNull() == selectedMonth
    }

    val monthlyIncome = monthTransactions
        .filter {
            it.kind == TransactionKind.INCOME
        }
        .sumOf {
            it.amount
        }

    val expenses = monthTransactions.filter {
        it.kind == TransactionKind.EXPENSE
    }

    val totalSpent = expenses.sumOf {
        it.amount
    }

    val slices = spendingSlices(expenses)

    val highestCategory = expenses
        .groupBy {
            it.category.ifBlank { "Other" }
        }
        .maxByOrNull { (_, entries) ->
            entries.sumOf { it.amount }
        }
        ?.key

    // Scrollable page with reduced overall whitespace.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 26.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Spending Insights",
                    fontSize = 30.sp,
                    color = colors.foreground
                )

                Spacer(Modifier.height(1.dp))

                Text(
                    text = "Small insights. Smarter habits.",
                    fontSize = 14.sp,
                    color = colors.muted
                )
            }

            Text(
                text = "CAD",
                fontSize = 12.sp,
                color = colors.muted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // MONTH SELECTOR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsightsMonthButton(
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
                            vertical = 7.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(19.dp)
                    )

                    Spacer(Modifier.width(7.dp))

                    Text(
                        text = monthLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.foreground,
                        maxLines = 1
                    )

                    Spacer(Modifier.width(5.dp))

                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Choose month",
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
                                            Locale.ENGLISH
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

            InsightsMonthButton(
                previous = false,
                onClick = {
                    monthOffset++
                    monthMenuOpen = false
                }
            )
        }

        // DONUT CHART CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "${insightsMoney(totalSpent)} spent this month",
                    fontSize = 23.sp,
                    color = colors.foreground
                )

                Text(
                    text = "A breakdown of your $monthName expenses",
                    fontSize = 13.sp,
                    color = colors.muted
                )

                Spacer(Modifier.height(4.dp))

                // Responsive donut and category legend.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val compact = maxWidth < 340.dp

                    val donutSize = (maxWidth * 0.41f)
                        .coerceIn(120.dp, 176.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (compact) 7.dp else 12.dp
                        )
                    ) {
                        InsightsDonutChart(
                            slices = slices,
                            total = totalSpent,
                            modifier = Modifier.size(donutSize)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(
                                7.dp
                            )
                        ) {
                            if (slices.isEmpty()) {
                                Text(
                                    text = "No expense data",
                                    fontSize = 13.sp,
                                    color = colors.muted
                                )
                            } else {
                                slices.forEach { slice ->
                                    CategoryLegendRow(
                                        slice = slice,
                                        total = totalSpent,
                                        compact = compact
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // INCOME VS EXPENSES CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 15.dp,
                    vertical = 15.dp
                ),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "Income vs. expenses",
                    color = colors.foreground,
                    fontSize = 19.sp
                )

                Text(
                    text = "$monthLabel · CAD · scale starts at \$0",
                    color = colors.muted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(4.dp))

                // Original bar style restored.
                // Gridlines stay behind the solid bars.
                InsightsBarChart(
                    income = monthlyIncome,
                    expenses = totalSpent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                )
            }
        }

        // MONTH AT A GLANCE
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.highlightCard
            )
        ) {
            Row(
                modifier = Modifier.padding(15.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            colors.highlightIconBackground,
                            RoundedCornerShape(13.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(21.dp)
                    )
                }

                Spacer(Modifier.width(11.dp))

                Column {
                    Text(
                        text = "YOUR MONTH AT A GLANCE",
                        color = colors.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (highestCategory == null) {
                            "No expense insights yet for this month."
                        } else {
                            "$highestCategory is your highest spending category this month."
                        },
                        color = colors.foreground,
                        fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

// MONTH NAVIGATION BUTTONS
@Composable
private fun InsightsMonthButton(
    previous: Boolean,
    onClick: () -> Unit
) {
    val colors = insightsPalette()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(45.dp)
            .background(
                colors.surface,
                RoundedCornerShape(14.dp)
            )
    ) {
        Icon(
            imageVector = if (previous) {
                Icons.Outlined.ChevronLeft
            } else {
                Icons.Outlined.ChevronRight
            },
            contentDescription = if (previous) {
                "Previous month"
            } else {
                "Next month"
            },
            tint = colors.foreground,
            modifier = Modifier.size(25.dp)
        )
    }
}

// DONUT CHART
@Composable
private fun InsightsDonutChart(
    slices: List<SpendingSlice>,
    total: Double,
    modifier: Modifier = Modifier
) {
    val colors = insightsPalette()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val strokeWidth = 15.dp.toPx()
            val inset = strokeWidth / 2f + 1.dp.toPx()

            val diameter = (
                    size.minDimension - inset * 2f
                    ).coerceAtLeast(0f)

            val arcTopLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )

            val arcSize = Size(
                width = diameter,
                height = diameter
            )

            if (slices.isEmpty() || total <= 0.0) {
                drawArc(
                    color = insightsPale,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth
                    )
                )
            } else {
                var startAngle = -90f

                slices.forEach { slice ->
                    val sweepAngle = (
                            slice.amount / total * 360.0
                            ).toFloat()

                    val gap = minOf(
                        1.1f,
                        sweepAngle * 0.2f
                    )

                    drawArc(
                        color = slice.color,
                        startAngle = startAngle + gap / 2f,
                        sweepAngle = (
                                sweepAngle - gap
                                ).coerceAtLeast(0f),
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidth
                        )
                    )

                    startAngle += sweepAngle
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = insightsMoney(total),
                fontSize = 26.sp,
                color = colors.foreground,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Text(
                text = "total spent",
                fontSize = 12.sp,
                color = colors.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

// COMPACT CATEGORY LEGEND
@Composable
private fun CategoryLegendRow(
    slice: SpendingSlice,
    total: Double,
    compact: Boolean
) {
    val colors = insightsPalette()
    val percentage = if (total > 0.0) {
        (slice.amount / total * 100.0).roundToInt()
    } else {
        0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(
                    slice.color,
                    CircleShape
                )
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = slice.category,
            color = colors.foreground,
            fontSize = if (compact) {
                11.sp
            } else {
                13.sp
            },
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(3.dp))

        Text(
            text = "$percentage%",
            color = colors.foreground,
            fontSize = if (compact) {
                11.sp
            } else {
                13.sp
            },
            maxLines = 1
        )
    }
}

// BAR CHART AXIS
// Use only whole-thousand increments.
private fun chartAxisMax(
    income: Double,
    expenses: Double
): Int {
    val largest = max(
        income.coerceAtLeast(0.0),
        expenses.coerceAtLeast(0.0)
    )

    val thousands = ceil(
        largest / 1000.0
    ).toInt().coerceAtLeast(1)

    // Give bars some headroom at an exact $1k boundary.
    val upperThousands = if (
        largest >= thousands * 1000.0
    ) {
        thousands + 1
    } else {
        thousands
    }

    return upperThousands * 1000
}

private fun axisLabel(value: Int): String {
    return if (value == 0) {
        "\$0"
    } else {
        "\$${value / 1000}k"
    }
}

// INCOME VS EXPENSES BAR CHART
// Original solid bars, with gridlines behind them.
// No white horizontal lines drawn across the bars.
@Composable
private fun InsightsBarChart(
    income: Double,
    expenses: Double,
    modifier: Modifier = Modifier
) {
    val colors = insightsPalette()
    val axisMax = chartAxisMax(
        income = income,
        expenses = expenses
    )

    val segments = axisMax / 1000

    Canvas(
        modifier = modifier
    ) {
        val chartLeft = 43.dp.toPx()
        val chartRight = size.width - 7.dp.toPx()

        val chartTop = 23.dp.toPx()
        val chartBottom = size.height - 32.dp.toPx()

        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        val nativeCanvas = drawContext.canvas.nativeCanvas

        val tickPaint = AndroidPaint(
            AndroidPaint.ANTI_ALIAS_FLAG
        ).apply {
            color = colors.muted.toArgb()
            textSize = 11.sp.toPx()
            textAlign = AndroidPaint.Align.RIGHT
            typeface = Typeface.create(
                "sans-serif",
                Typeface.NORMAL
            )
        }

        val amountPaint = AndroidPaint(
            AndroidPaint.ANTI_ALIAS_FLAG
        ).apply {
            color = colors.foreground.toArgb()
            textSize = 14.sp.toPx()
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
        }

        val namePaint = AndroidPaint(
            AndroidPaint.ANTI_ALIAS_FLAG
        ).apply {
            color = colors.muted.toArgb()
            textSize = 13.sp.toPx()
            textAlign = AndroidPaint.Align.CENTER
            typeface = Typeface.create(
                "sans-serif",
                Typeface.NORMAL
            )
        }

        // Draw gridlines FIRST, behind the bars.
        for (index in 0..segments) {
            val value = index * 1000

            val fraction =
                value.toFloat() / axisMax.toFloat()

            val y = chartBottom -
                    fraction * chartHeight

            drawLine(
                color = colors.grid,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx()
            )

            nativeCanvas.drawText(
                axisLabel(value),
                chartLeft - 9.dp.toPx(),
                y + 4.dp.toPx(),
                tickPaint
            )
        }

        val incomeCenter =
            chartLeft + chartWidth * 0.26f

        val expenseCenter =
            chartLeft + chartWidth * 0.76f

        val barWidth = minOf(
            76.dp.toPx(),
            chartWidth * 0.29f
        )

        // Both bars use the same axis and baseline.
        fun drawBar(
            value: Double,
            centerX: Float,
            color: Color,
            label: String
        ) {
            val safeValue = value.coerceAtLeast(0.0)

            val ratio = (
                    safeValue / axisMax.toDouble()
                    ).toFloat().coerceIn(0f, 1f)

            val barHeight = ratio * chartHeight
            val barTop = chartBottom - barHeight
            val barLeft = centerX - barWidth / 2f

            if (safeValue > 0.0 && barHeight > 0f) {
                val corner = minOf(
                    11.dp.toPx(),
                    barHeight / 2f
                )

                drawRoundRect(
                    color = color,
                    topLeft = Offset(
                        barLeft,
                        barTop
                    ),
                    size = Size(
                        barWidth,
                        barHeight
                    ),
                    cornerRadius = CornerRadius(
                        corner,
                        corner
                    )
                )

                // Square bottom matching the wireframe.
                if (barHeight > corner) {
                    drawRect(
                        color = color,
                        topLeft = Offset(
                            barLeft,
                            barTop + corner
                        ),
                        size = Size(
                            barWidth,
                            barHeight - corner
                        )
                    )
                }
            }

            // Draw amount above each bar.
            amountPaint.color = if (label == "Income") {
                colors.accent.toArgb()
            } else {
                colors.foreground.toArgb()
            }

            nativeCanvas.drawText(
                insightsMoney(safeValue),
                centerX,
                (barTop - 9.dp.toPx())
                    .coerceAtLeast(16.sp.toPx()),
                amountPaint
            )

            // Place labels under the $0 baseline.
            nativeCanvas.drawText(
                label,
                centerX,
                chartBottom + 23.dp.toPx(),
                namePaint
            )
        }

        drawBar(
            value = income,
            centerX = incomeCenter,
            color = colors.accent,
            label = "Income"
        )

        drawBar(
            value = expenses,
            centerX = expenseCenter,
            color = colors.foreground,
            label = "Expenses"
        )
    }
}

