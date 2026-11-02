package com.group3.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.group3.spendwise.ui.theme.SpendBackground
import com.group3.spendwise.ui.theme.SpendGreen
import com.group3.spendwise.ui.theme.SpendLightGreen
import com.group3.spendwise.ui.theme.SpendMuted
import com.group3.spendwise.ui.theme.SpendNavy

private data class BottomTab(val label: String, val icon: ImageVector)

/**
 * Screen shown after login. Holds the bottom navigation bar.
 * Only the Dashboard is built for the progress report; the other tabs show a placeholder.
 */
@Composable
fun MainScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        BottomTab("Dashboard", Icons.Outlined.Dashboard),
        BottomTab("Transactions", Icons.Outlined.SwapHoriz),
        BottomTab("Add", Icons.Filled.Add),
        BottomTab("Budget", Icons.Outlined.AccountBalanceWallet),
        BottomTab("Insights", Icons.Outlined.Insights)
    )

    Scaffold(
        containerColor = SpendBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            if (tab.label == "Add") {
                                // Green circle in the middle, like the wireframe
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SpendGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            } else {
                                Icon(imageVector = tab.icon, contentDescription = null)
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
                            selectedIconColor = SpendGreen,
                            selectedTextColor = SpendGreen,
                            indicatorColor = SpendLightGreen,
                            unselectedIconColor = SpendMuted,
                            unselectedTextColor = SpendMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    userName = userName,
                    userEmail = userEmail,
                    onLogout = onLogout
                )

                else -> PlaceholderScreen(
                    title = if (tabs[selectedTab].label == "Add") {
                        "Add Transaction"
                    } else {
                        tabs[selectedTab].label
                    }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SpendNavy
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Coming in the next phase of development.",
            style = MaterialTheme.typography.bodyMedium,
            color = SpendMuted
        )
    }
}