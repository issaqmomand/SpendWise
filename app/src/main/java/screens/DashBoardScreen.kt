package com.group3.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group3.spendwise.ui.theme.SpendBorder
import com.group3.spendwise.ui.theme.SpendError
import com.group3.spendwise.ui.theme.SpendGreen
import com.group3.spendwise.ui.theme.SpendLightGreen
import com.group3.spendwise.ui.theme.SpendMuted
import com.group3.spendwise.ui.theme.SpendNavy
import com.group3.spendwise.ui.theme.SpendWarning
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

// ---------------------------------------------------------------------------
// Sample data for the prototype. In the next phase this will come from Room.
// ---------------------------------------------------------------------------

private data class SampleTransaction(
    val name: String,
    val category: String,
    val date: String,
    val amount: Double, // negative = expense, positive = income
    val icon: ImageVector
)

private val sampleTransactions = listOf(
    SampleTransaction("Groceries", "Food", "Oct 24", -85.50, Icons.Outlined.ShoppingBasket),
    SampleTransaction("Coffee", "Food", "Oct 24", -6.75, Icons.Outlined.Coffee),
    SampleTransaction("Part-Time Job", "Salary", "Oct 23", 950.00, Icons.Outlined.Work),
    SampleTransaction("Transportation", "Transportation", "Oct 22", -35.00, Icons.Outlined.DirectionsBus)
)

private fun money(value: Double): String = String.format(Locale.US, "\$%,.2f", value)

@Composable
fun DashboardScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    // UR-2: user can hide / show the balance
    var balanceVisible by rememberSaveable { mutableStateOf(true) }

    // Sample monthly figures
    val monthlyIncome = 3200.0
    val monthlyExpenses = 750.0
    val monthlyBudget = 1200.0

    // Data processing (UR-1, UR-4, UR-5)
    val balance = monthlyIncome - monthlyExpenses
    val remaining = monthlyBudget - monthlyExpenses
    val percentUsed = monthlyExpenses / monthlyBudget * 100
    val progress = (monthlyExpenses / monthlyBudget).toFloat().coerceIn(0f, 1f)
    val percentText = String.format(Locale.US, "%.1f", percentUsed)

    // UR-18: bar changes colour near the limit and when exceeded
    val barColor = when {
        progress >= 1f -> SpendError
        progress >= 0.8f -> SpendWarning
        else -> SpendGreen
    }

    // UR-5: when over budget, show how much (also as text, not just colour)
    val overBy = -remaining
    val statusText = if (remaining >= 0) {
        "${money(remaining)} remaining"
    } else {
        "Over budget by ${money(overBy)}, ${String.format(Locale.US, "%.1f", percentUsed - 100)}%"
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning!"
        hour < 18 -> "Good Afternoon!"
        else -> "Good Evening!"
    }
    val displayName = userName.ifBlank { userEmail }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------------------------------------------------------- Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SpendNavy
                )
                Text(
                    text = "Signed in as $displayName",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpendMuted
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Log out",
                    tint = SpendNavy
                )
            }
        }

        // --------------------------------------------------- Balance card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SpendNavy)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { balanceVisible = !balanceVisible }) {
                        Icon(
                            imageVector = if (balanceVisible) {
                                Icons.Outlined.Visibility
                            } else {
                                Icons.Outlined.VisibilityOff
                            },
                            contentDescription = if (balanceVisible) "Hide balance" else "Show balance",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = if (balanceVisible) money(balance) else "\u2022\u2022\u2022\u2022\u2022\u2022",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tracked by you. No bank connection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // --------------------------------------- Monthly income / expenses
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Monthly Income",
                value = money(monthlyIncome),
                valueColor = SpendGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Monthly Expenses",
                value = money(monthlyExpenses),
                valueColor = SpendNavy,
                modifier = Modifier.weight(1f)
            )
        }

        // ------------------------------------------------- Budget overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Monthly Budget Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SpendNavy,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$percentText% used",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SpendNavy
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${money(monthlyExpenses)} spent of ${money(monthlyBudget)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SpendMuted
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = barColor,
                    trackColor = SpendLightGreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (remaining >= 0) SpendGreen else SpendError
                )
            }
        }

        // -------------------------------------------- Recent transactions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Recent transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SpendNavy
                )
                sampleTransactions.forEachIndexed { index, transaction ->
                    if (index > 0) HorizontalDivider(color = SpendBorder)
                    TransactionRow(transaction)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Prototype sample data. Real transactions will come from the Room database next.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpendMuted
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = SpendMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun TransactionRow(transaction: SampleTransaction) {
    val isIncome = transaction.amount >= 0
    val sign = if (isIncome) "+" else "-"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(SpendLightGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = transaction.icon,
                contentDescription = null,
                tint = SpendGreen
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = SpendNavy
            )
            Text(
                text = "${transaction.category} \u00B7 ${transaction.date}",
                style = MaterialTheme.typography.bodySmall,
                color = SpendMuted
            )
        }
        Text(
            text = "$sign${money(abs(transaction.amount))}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isIncome) SpendGreen else SpendNavy
        )
    }
}