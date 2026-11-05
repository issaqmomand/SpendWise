package com.group3.spendwise.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.group3.spendwise.TransactionSuccessSound
import com.group3.spendwise.data.local.SpendWiseDatabase
import com.group3.spendwise.data.local.TransactionEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private data class BottomTab(val label: String, val icon: ImageVector)

private fun TransactionEntity.toSpendTransaction() = SpendTransaction(
    id = id,
    description = description,
    amount = amount,
    category = category,
    date = date,
    kind = if (kind == TransactionKind.INCOME.name) {
        TransactionKind.INCOME
    } else {
        TransactionKind.EXPENSE
    },
    notes = notes,
    receiptUri = receiptUri,
    voiceNotePath = voiceNotePath
)

private fun SpendTransaction.toEntity(ownerUid: String) = TransactionEntity(
    id = id,
    ownerUid = ownerUid,
    description = description,
    amount = amount,
    category = category,
    date = date,
    kind = kind.name,
    notes = notes,
    receiptUri = receiptUri,
    voiceNotePath = voiceNotePath
)

@Composable
fun MainScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Preload the short confirmation chime once for this screen.
    val successSound = remember(context) { TransactionSuccessSound(context) }
    DisposableEffect(successSound) {
        onDispose { successSound.release() }
    }

    val transactionDao = remember(context) {
        SpendWiseDatabase.getInstance(context).transactionDao()
    }
    val ownerUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val transactionFlow = remember(transactionDao, ownerUid) {
        if (ownerUid.isNotBlank()) {
            transactionDao.observeForUser(ownerUid)
        } else {
            flowOf(emptyList<TransactionEntity>())
        }
    }
    val savedEntities by transactionFlow.collectAsState(initial = emptyList())
    val transactions = savedEntities.map { it.toSpendTransaction() }

    var selectedTab by rememberSaveable(ownerUid) { mutableIntStateOf(0) }
    var transactionRoute by rememberSaveable(ownerUid) { mutableStateOf("tabs") }
    var selectedTransactionId by rememberSaveable(ownerUid) {
        mutableStateOf<Long?>(null)
    }
    val selectedTransaction = transactions.firstOrNull {
        it.id == selectedTransactionId
    }

    // Monitor monthly spending and trigger budget notifications.
    BudgetAlertObserver(
        ownerUid = ownerUid,
        transactions = transactions,
        budgetTabVisible = selectedTab == 3 && transactionRoute == "tabs"
    )

    val deleteTransaction: (SpendTransaction) -> Unit = { transaction ->
        if (ownerUid.isNotBlank()) {
            scope.launch {
                try {
                    val deleted = transactionDao.delete(transaction.id, ownerUid)
                    if (deleted > 0) {
                        transactionRoute = "tabs"
                        selectedTab = 1
                        selectedTransactionId = null
                        snackbarHostState.showSnackbar("Transaction deleted")
                    } else {
                        snackbarHostState.showSnackbar("Transaction not found")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Unable to delete transaction")
                }
            }
        }
    }

    val viewReceipt: (SpendTransaction) -> Unit = { transaction ->
        transaction.receiptUri?.let { uriString ->
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(uriString), "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Unable to open receipt")
                }
            }
        }
    }

    // Original five-tab bottom bar, unchanged.
    val tabs = listOf(
        BottomTab("Dashboard", Icons.Outlined.Dashboard),
        BottomTab("Transactions", Icons.Outlined.SwapHoriz),
        BottomTab("Add", Icons.Filled.Add),
        BottomTab("Budget", Icons.Outlined.AccountBalanceWallet),
        BottomTab("Insights", Icons.Outlined.Insights)
    )

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = colors.surface) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index && transactionRoute == "tabs",
                        onClick = {
                            transactionRoute = "tabs"
                            selectedTransactionId = null
                            selectedTab = index
                        },
                        icon = {
                            if (tab.label == "Add") {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(colors.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = colors.onPrimary
                                    )
                                }
                            } else {
                                Icon(tab.icon, contentDescription = tab.label)
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary,
                            selectedTextColor = colors.primary,
                            indicatorColor = colors.secondaryContainer,
                            unselectedIconColor = colors.onSurfaceVariant,
                            unselectedTextColor = colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                transactionRoute == "details" && selectedTransaction != null -> {
                    TransactionDetailsScreen(
                        transaction = selectedTransaction,
                        onBack = {
                            transactionRoute = "tabs"
                            selectedTab = 1
                        },
                        onEdit = { transactionRoute = "edit" },
                        onDelete = { deleteTransaction(selectedTransaction) },
                        onViewReceipt = { viewReceipt(selectedTransaction) }
                    )
                }

                transactionRoute == "edit" && selectedTransaction != null -> {
                    AddTransactionScreen(
                        transactionToEdit = selectedTransaction,
                        onBack = { transactionRoute = "details" },
                        onDelete = { deleteTransaction(selectedTransaction) },
                        onSave = { updatedTransaction ->
                            if (ownerUid.isNotBlank()) {
                                scope.launch {
                                    try {
                                        val updated = transactionDao.updateWithVoiceNote(
                                            transactionId = updatedTransaction.id,
                                            ownerUid = ownerUid,
                                            description = updatedTransaction.description,
                                            amount = updatedTransaction.amount,
                                            category = updatedTransaction.category,
                                            date = updatedTransaction.date,
                                            kind = updatedTransaction.kind.name,
                                            notes = updatedTransaction.notes,
                                            receiptUri = updatedTransaction.receiptUri,
                                            voiceNotePath = updatedTransaction.voiceNotePath
                                        )
                                        if (updated > 0) {
                                            // Only play after Room confirms the update.
                                            successSound.play()
                                            transactionRoute = "details"
                                            snackbarHostState.showSnackbar(
                                                "Transaction updated successfully"
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar("Transaction not found")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            "Unable to update transaction"
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                transactionRoute != "tabs" -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Loading transaction...", color = colors.onBackground)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            transactionRoute = "tabs"
                            selectedTab = 1
                        }) { Text("Back to Transactions") }
                    }
                }

                else -> when (selectedTab) {
                    0 -> DashboardScreen(
                        userName = userName,
                        userEmail = userEmail,
                        onLogout = onLogout,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = onDarkModeChange,
                        transactions = transactions,
                        onViewTransactions = {
                            selectedTab = 1
                        },
                        onTransactionClick = { transaction ->
                            selectedTransactionId = transaction.id
                            transactionRoute = "details"
                        }
                    )
                    1 -> TransactionsScreen(
                        transactions = transactions,
                        onAddClick = { selectedTab = 2 },
                        onTransactionClick = { transaction ->
                            selectedTransactionId = transaction.id
                            transactionRoute = "details"
                        },
                        onEditClick = { transaction ->
                            selectedTransactionId = transaction.id
                            transactionRoute = "edit"
                        },
                        onDeleteClick = { transaction -> deleteTransaction(transaction) },
                        onReceiptClick = { transaction -> viewReceipt(transaction) }
                    )
                    2 -> AddTransactionScreen(
                        onBack = { selectedTab = 1 },
                        onSave = { newTransaction ->
                            if (ownerUid.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Please sign in to save transactions"
                                    )
                                }
                            } else {
                                scope.launch {
                                    try {
                                        val savedId = transactionDao.insert(
                                            newTransaction.toEntity(ownerUid).copy(id = 0L)
                                        )
                                        if (savedId > 0L) {
                                            // Only play after Room confirms the insert.
                                            successSound.play()
                                            selectedTab = 1
                                            snackbarHostState.showSnackbar(
                                                "Transaction saved successfully"
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "Unable to save transaction"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            "Unable to save transaction"
                                        )
                                    }
                                }
                            }
                        }
                    )
                    3 -> BudgetScreen(
                        transactions = transactions,
                        ownerUid = ownerUid
                    )
                    4 -> InsightsScreen(transactions = transactions)
                }
            }
        }
    }
}
