package com.group3.spendwise.model

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

