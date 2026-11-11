package com.group3.spendwise.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.group3.spendwise.R

/** Persist original category IDs/English labels; translate only what the user sees. */
object TransactionCategories {
    val expenses = listOf("Food", "Transportation", "Shopping", "Entertainment", "School", "Housing", "Subscriptions", "Other")
    val income = listOf("Salary", "Gift", "Refund", "Other")
}

@Composable
fun localizedCategory(value: String): String = when (value.lowercase(java.util.Locale.ROOT)) {
    "food" -> stringResource(R.string.category_food)
    "transportation" -> stringResource(R.string.category_transportation)
    "shopping" -> stringResource(R.string.category_shopping)
    "entertainment" -> stringResource(R.string.category_entertainment)
    "school" -> stringResource(R.string.category_school)
    "housing" -> stringResource(R.string.category_housing)
    "subscriptions" -> stringResource(R.string.category_subscriptions)
    "salary" -> stringResource(R.string.category_salary)
    "gift" -> stringResource(R.string.category_gift)
    "refund" -> stringResource(R.string.category_refund)
    "other" -> stringResource(R.string.category_other)
    else -> value // Custom/legacy category remains intact.
}
