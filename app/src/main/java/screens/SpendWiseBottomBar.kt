
package com.group3.spendwise.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val navGreen = Color(0xFF477A62)
private val navSage = Color(0xFFEAF1E9)
private val navMuted = Color(0xFF7A8880)
private val navDivider = Color(0xFFE7ECE6)

private data class SpendWiseTab(
    val title: String,
    val icon: ImageVector
)

@Composable
fun SpendWiseBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        SpendWiseTab("Dashboard", Icons.Outlined.Dashboard),
        SpendWiseTab("Transactions", Icons.Outlined.SwapHoriz),
        SpendWiseTab("Add", Icons.Filled.Add),
        SpendWiseTab("Budget", Icons.Outlined.AccountBalanceWallet),
        SpendWiseTab("Insights", Icons.Outlined.Insights)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        HorizontalDivider(
            color = navDivider,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val isAdd = index == 2

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onTabSelected(index)
                        }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(
                                if (isAdd) 54.dp else 42.dp
                            )
                            .background(
                                color = when {
                                    isAdd -> navGreen
                                    selected -> navSage
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(
                                    if (isAdd) 15.dp else 17.dp
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = when {
                                isAdd -> Color.White
                                selected -> navGreen
                                else -> navMuted
                            },
                            modifier = Modifier.size(
                                if (isAdd) 29.dp else 25.dp
                            )
                        )
                    }

                    Text(
                        text = tab.title,
                        color = if (selected)
                            navGreen
                        else
                            navMuted,
                        fontSize = 11.sp,
                        fontWeight = if (selected)
                            FontWeight.SemiBold
                        else
                            FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
