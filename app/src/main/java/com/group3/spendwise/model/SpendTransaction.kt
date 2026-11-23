package com.group3.spendwise.model

/** Whether a transaction adds money (INCOME) or removes money (EXPENSE). */
enum class TransactionKind { INCOME, EXPENSE }

/**
 * App-wide model for a single income or expense entry.
 * The UI works with this class; the repository converts it to/from the Room entity.
 * @property id Room primary key (0 for a transaction that has not been saved yet).
 * @property description Short name shown in lists, e.g. "Groceries".
 * @property amount Always stored as a positive number; [kind] decides the sign.
 * @property category Stable English category ID (translated only when displayed).
 * @property date Date of the transaction in yyyy-MM-dd format.
 * @property kind Income or expense.
 * @property notes Optional free-text note.
 * @property receiptUri Optional content URI of the attached receipt photo.
 * @property voiceNotePath Optional file path of an attached voice note recording.
 */
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

