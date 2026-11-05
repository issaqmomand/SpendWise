package com.group3.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.group3.spendwise.VoiceNoteRecorder
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// Keep these models here so MainScreen and AddTransactionScreen can reuse them.
enum class TransactionKind { INCOME, EXPENSE }

data class SpendTransaction(
    val id: Long,
    val description: String,
    val amount: Double,
    val category: String,
    val date: String, // yyyy-MM-dd
    val kind: TransactionKind,
    val notes: String = "",
    val receiptUri: String? = null,
    val voiceNotePath: String? = null
)

// Theme-aware colors. Light mode intentionally keeps the existing appearance.
private data class TransactionsPalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val muted: Color,
    val divider: Color,
    val dateDivider: Color,
    val error: Color,
    val surface: Color,
    val iconBackground: Color,
    val selectedTabBackground: Color,
    val selectedTabText: Color
)

@Composable
private fun transactionsPalette(): TransactionsPalette {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) {
        TransactionsPalette(
            background = scheme.background,
            foreground = scheme.onSurface,
            accent = scheme.primary,
            muted = scheme.onSurfaceVariant,
            divider = scheme.outlineVariant,
            dateDivider = scheme.primary.copy(alpha = 0.65f),
            error = scheme.error,
            surface = scheme.surface,
            iconBackground = scheme.surfaceVariant,
            selectedTabBackground = scheme.primary,
            selectedTabText = scheme.onPrimary
        )
    } else {
        TransactionsPalette(
            background = Color(0xFFF6F7F4),
            foreground = Color(0xFF203846),
            accent = Color(0xFF477A62),
            muted = Color(0xFF7A8880),
            divider = Color(0xFFE7ECE6),
            dateDivider = Color(0xFF8EAF9A),
            error = Color(0xFFC7554E),
            surface = Color.White,
            iconBackground = Color(0xFFF6F7F4),
            selectedTabBackground = Color(0xFF203846),
            selectedTabText = Color.White
        )
    }
}

private enum class TransactionFilter { ALL, INCOME, EXPENSES }

@Composable
fun TransactionsScreen(
    transactions: List<SpendTransaction> = emptyList(),
    onAddClick: () -> Unit,
    onTransactionClick: ((SpendTransaction) -> Unit)? = null,
    onEditClick: ((SpendTransaction) -> Unit)? = null,
    onDeleteClick: ((SpendTransaction) -> Unit)? = null,
    onReceiptClick: ((SpendTransaction) -> Unit)? = null
) {
    val colors = transactionsPalette()
    var filter by rememberSaveable { mutableStateOf(TransactionFilter.ALL.name) }
    var category by rememberSaveable { mutableStateOf("All categories") }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    // A single player is shared across the list so only one note plays at a time.
    val context = LocalContext.current
    val audioPlayer = remember(context) { VoiceNoteRecorder(context) }
    var playingTransactionId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }

    val toggleVoiceNote: (SpendTransaction) -> Unit = { transaction ->
        val path = transaction.voiceNotePath
        if (!path.isNullOrBlank()) {
            if (playingTransactionId == transaction.id) {
                audioPlayer.stopPlayback()
                playingTransactionId = null
            } else {
                try {
                    audioPlayer.playRecording(path) {
                        playingTransactionId = null
                    }
                    playingTransactionId = transaction.id
                } catch (e: Exception) {
                    playingTransactionId = null
                    Toast.makeText(
                        context,
                        "Unable to play this voice note",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val now = YearMonth.now()
    val monthTransactions = transactions.filter { transaction ->
        runCatching { YearMonth.from(LocalDate.parse(transaction.date)) }.getOrNull() == now
    }
    val monthlyIncome = monthTransactions.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amount }
    val monthlyExpenses = monthTransactions.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.amount }
    val allCategories = transactions.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    val shownTransactions = transactions
        .filter { transaction ->
            val matchesType = when (filter) {
                TransactionFilter.INCOME.name -> transaction.kind == TransactionKind.INCOME
                TransactionFilter.EXPENSES.name -> transaction.kind == TransactionKind.EXPENSE
                else -> true
            }
            // Category filtering only applies to the All tab.
            matchesType && (
                    filter != TransactionFilter.ALL.name ||
                            category == "All categories" ||
                            transaction.category == category
                    )
        }
        .sortedWith(compareByDescending<SpendTransaction> { it.date }.thenByDescending { it.id })
    val grouped = shownTransactions.groupBy { it.date }.entries.toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Transactions", fontSize = 32.sp, fontWeight = FontWeight.Normal, color = colors.foreground)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${now.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))} · Your money, recorded",
                        fontSize = 14.sp,
                        color = colors.muted
                    )
                }
                Text("CAD", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.muted)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MonthlySummaryCard(
                    title = "Monthly Income", amount = monthlyIncome,
                    positive = true, modifier = Modifier.weight(1f)
                )
                MonthlySummaryCard(
                    title = "Monthly Expenses", amount = monthlyExpenses,
                    positive = false, modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            HorizontalDivider(color = colors.accent, thickness = 1.dp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    TransactionFilter.ALL to "All",
                    TransactionFilter.INCOME to "Income",
                    TransactionFilter.EXPENSES to "Expenses"
                ).forEach { (value, label) ->
                    val selected = filter == value.name
                    Box(
                        modifier = Modifier.weight(1f)
                            .background(if (selected) colors.selectedTabBackground else colors.surface, RoundedCornerShape(25.dp))
                            .clickable { filter = value.name }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selected) colors.selectedTabText else colors.muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Only the All tab shows the category dropdown.
        if (filter == TransactionFilter.ALL.name) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .background(colors.surface, RoundedCornerShape(15.dp))
                                .clickable { categoriesExpanded = true }
                                .padding(horizontal = 13.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.FilterList, null, tint = colors.accent, modifier = Modifier.size(21.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(category, fontSize = 14.sp, color = colors.foreground, maxLines = 1)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Outlined.KeyboardArrowDown, null, tint = colors.foreground, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = categoriesExpanded,
                            onDismissRequest = { categoriesExpanded = false }
                        ) {
                            (listOf("All categories") + allCategories).forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    category = option
                                    categoriesExpanded = false
                                })
                            }
                        }
                    }
                    Text("Recent activity", color = colors.muted, fontSize = 13.sp)
                }
            }
        }

        item {
            if (shownTransactions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(26.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.ReceiptLong, null, tint = colors.accent, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No transactions to show", color = colors.foreground, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Add a transaction or try another filter.", color = colors.muted, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onAddClick) { Text("+ Add transaction", color = colors.accent) }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(26.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    grouped.forEachIndexed { groupIndex, entry ->
                        if (groupIndex > 0) Spacer(Modifier.height(4.dp))
                        Text(
                            displayGroupDate(entry.key).uppercase(Locale.ENGLISH),
                            color = colors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        entry.value.forEachIndexed { index, transaction ->
                            TransactionRow(
                                transaction = transaction,
                                onOpen = { onTransactionClick?.invoke(transaction) },
                                onEdit = { onEditClick?.invoke(transaction) },
                                onDelete = { pendingDelete = transaction.id },
                                onReceipt = { onReceiptClick?.invoke(transaction) },
                                onVoiceNote = { toggleVoiceNote(transaction) },
                                isVoiceNotePlaying = playingTransactionId == transaction.id,
                                allowEdit = onEditClick != null,
                                allowDelete = onDeleteClick != null,
                                allowReceipt = onReceiptClick != null
                            )
                            if (index != entry.value.lastIndex || groupIndex != grouped.lastIndex) {
                                HorizontalDivider(
                                    color = if (index == entry.value.lastIndex) {
                                        colors.dateDivider
                                    } else {
                                        colors.divider
                                    },
                                    thickness = 1.dp
                                )
                                Spacer(Modifier.height(7.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    val toDelete = transactions.firstOrNull { it.id == pendingDelete }
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?", color = colors.foreground) },
            text = { Text("Remove \"${toDelete.description}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick?.invoke(toDelete)
                    pendingDelete = null
                }) { Text("Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel", color = colors.accent) }
            },
            containerColor = colors.background
        )
    }
}

@Composable
private fun MonthlySummaryCard(title: String, amount: Double, positive: Boolean, modifier: Modifier = Modifier) {
    val colors = transactionsPalette()
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(25.dp))
            .padding(horizontal = 15.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (positive) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(title, fontSize = 12.sp, color = colors.muted, maxLines = 1)
        }
        Spacer(Modifier.height(9.dp))
        Text(
            money(amount), fontSize = 24.sp,
            color = if (positive) colors.accent else colors.foreground,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun TransactionRow(
    transaction: SpendTransaction,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReceipt: () -> Unit,
    onVoiceNote: () -> Unit,
    isVoiceNotePlaying: Boolean,
    allowEdit: Boolean,
    allowDelete: Boolean,
    allowReceipt: Boolean
) {
    val colors = transactionsPalette()
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(46.dp)
                    .background(colors.iconBackground, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(categoryIcon(transaction.category), null, tint = colors.foreground, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.description,
                    color = colors.foreground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${transaction.category} · ${displayShortDate(transaction.date)}",
                    color = colors.muted,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "${if (transaction.kind == TransactionKind.INCOME) "+" else "-"}${money(transaction.amount)}",
                fontSize = 17.sp,
                color = if (transaction.kind == TransactionKind.INCOME) colors.accent else colors.foreground,
                maxLines = 1
            )
        }
        Row(modifier = Modifier.padding(start = 56.dp, top = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            if (allowEdit) {
                Row(
                    modifier = Modifier.clickable(onClick = onEdit).padding(end = 16.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Edit, null, tint = colors.accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                }
            }
            if (allowDelete) {
                Row(
                    modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, tint = colors.error, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 14.sp, color = colors.error)
                }
            }
        }
        if (!transaction.receiptUri.isNullOrBlank() && allowReceipt) {
            Row(
                modifier = Modifier
                    .padding(start = 56.dp, top = 2.dp, bottom = 1.dp)
                    .clickable(onClick = onReceipt),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "View Receipt",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }
        }

        // Show this action only for transactions that contain a voice note.
        if (!transaction.voiceNotePath.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .padding(start = 56.dp, top = 2.dp, bottom = 1.dp)
                    .clickable(onClick = onVoiceNote),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isVoiceNotePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isVoiceNotePlaying) "Stop Voice Note" else "Play Voice Note",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accent
                )
            }
        }
    }
}

private fun money(amount: Double): String = "$" + String.format(Locale.CANADA, "%,.2f", amount)

private fun displayGroupDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH))
}.getOrElse { value }

private fun displayShortDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
}.getOrElse { value }

private fun categoryIcon(category: String): ImageVector = when (category.lowercase(Locale.ENGLISH)) {
    "food", "groceries", "shopping" -> Icons.Outlined.ShoppingBasket
    "coffee" -> Icons.Outlined.Coffee
    "transportation", "transport" -> Icons.Outlined.DirectionsBus
    "salary", "work", "job" -> Icons.Outlined.WorkOutline
    "housing", "rent" -> Icons.Outlined.Home
    "school", "education" -> Icons.Outlined.School
    "subscriptions", "bills" -> Icons.Outlined.AccountBalanceWallet
    else -> Icons.Outlined.MoreHoriz
}

