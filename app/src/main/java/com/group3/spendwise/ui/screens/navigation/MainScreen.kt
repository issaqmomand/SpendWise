package com.group3.spendwise.ui.screens.navigation

import com.group3.spendwise.R
import com.group3.spendwise.ui.screens.budget.BudgetAlertObserver
import com.group3.spendwise.ui.screens.budget.BudgetScreen
import com.group3.spendwise.ui.screens.dashboard.DashboardScreen
import com.group3.spendwise.ui.screens.insights.InsightsScreen
import com.group3.spendwise.ui.screens.transactions.AddTransactionScreen
import com.group3.spendwise.ui.screens.transactions.TransactionDetailsScreen
import com.group3.spendwise.ui.screens.transactions.TransactionsScreen
import androidx.compose.ui.res.stringResource
import com.group3.spendwise.ui.components.localizedCategory

import com.group3.spendwise.model.SpendTransaction
import com.group3.spendwise.model.TransactionKind

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
import com.group3.spendwise.data.TransactionRepository
import kotlinx.coroutines.launch

private data class BottomTab(val label: String, val icon: ImageVector)

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

    val repository = remember(context) { TransactionRepository(context) }
    val ownerUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val transactionFlow = remember(repository, ownerUid) {
        repository.observeForUser(ownerUid)
    }
    val transactions by transactionFlow.collectAsState(initial = emptyList())

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
                    val deleted = repository.delete(ownerUid, transaction.id)
                    if (deleted > 0) {
                        transactionRoute = "tabs"
                        selectedTab = 1
                        selectedTransactionId = null
                        snackbarHostState.showSnackbar(context.getString(R.string.transaction_deleted))
                    } else {
                        snackbarHostState.showSnackbar(context.getString(R.string.transaction_not_found))
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(context.getString(R.string.transaction_delete_failed))
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
                    snackbarHostState.showSnackbar(context.getString(R.string.transaction_receipt_open_failed))
                }
            }
        }
    }

    // Original five-tab bottom bar, unchanged.
    val tabs = listOf(
        BottomTab(stringResource(R.string.nav_dashboard), Icons.Outlined.Dashboard),
        BottomTab(stringResource(R.string.nav_transactions), Icons.Outlined.SwapHoriz),
        BottomTab(stringResource(R.string.nav_add), Icons.Filled.Add),
        BottomTab(stringResource(R.string.nav_budget), Icons.Outlined.AccountBalanceWallet),
        BottomTab(stringResource(R.string.nav_insights), Icons.Outlined.Insights)
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
                            if (index == 2) {
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
                                        val updated = repository.update(ownerUid, updatedTransaction)
                                        if (updated > 0) {
                                            // Only play after Room confirms the update.
                                            successSound.play()
                                            transactionRoute = "details"
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.transaction_updated)
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(context.getString(R.string.transaction_not_found))
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.transaction_update_failed)
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
                        Text(stringResource(R.string.transaction_loading), color = colors.onBackground)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = {
                            transactionRoute = "tabs"
                            selectedTab = 1
                        }) { Text(stringResource(R.string.transaction_back_to_list)) }
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
                                        context.getString(R.string.transaction_auth_required)
                                    )
                                }
                            } else {
                                scope.launch {
                                    try {
                                        val savedId = repository.insert(ownerUid, newTransaction)
                                        if (savedId > 0L) {
                                            // Only play after Room confirms the insert.
                                            successSound.play()
                                            selectedTab = 1
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.transaction_saved)
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                context.getString(R.string.transaction_save_failed)
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.transaction_save_failed)
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
