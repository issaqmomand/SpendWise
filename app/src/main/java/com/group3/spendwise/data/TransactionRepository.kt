package com.group3.spendwise.data

import android.content.Context
import com.group3.spendwise.data.local.SpendWiseDatabase
import com.group3.spendwise.data.local.TransactionEntity
import com.group3.spendwise.model.SpendTransaction
import com.group3.spendwise.model.TransactionKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Database mapping and persistence stay outside Compose screen files. */
// Room DAO used for every database call in this repository.
class TransactionRepository(context: Context) {
    private val dao = SpendWiseDatabase.getInstance(context.applicationContext).transactionDao()

    /**
     * Streams the signed-in user's transactions. Room emits a new list every time the table changes,
     * so screens collecting this Flow refresh automatically. A blank uid returns an empty list.
     */
    fun observeForUser(uid: String): Flow<List<SpendTransaction>> =
        if (uid.isBlank()) flowOf(emptyList()) else dao.observeForUser(uid).map { items ->
            items.map { it.toModel() }
        }

    /** Saves a new transaction for [uid] and returns its generated ID. The id is reset to 0 so Room auto-generates it. */
    suspend fun insert(uid: String, transaction: SpendTransaction): Long =
        dao.insert(transaction.toEntity(uid).copy(id = 0L))

    /** Updates an existing transaction owned by [uid]. Returns the number of rows changed (0 if it was not found). */
    suspend fun update(uid: String, transaction: SpendTransaction): Int =
        dao.updateWithVoiceNote(
            transactionId = transaction.id,
            ownerUid = uid,
            description = transaction.description,
            amount = transaction.amount,
            category = transaction.category,
            date = transaction.date,
            kind = transaction.kind.name,
            notes = transaction.notes,
            receiptUri = transaction.receiptUri,
            voiceNotePath = transaction.voiceNotePath
        )

    /** Deletes transaction [id] only if it belongs to [uid]. Returns the number of rows removed. */
    suspend fun delete(uid: String, id: Long): Int = dao.delete(id, uid)

    // Converts a database row into the app model. Any unknown kind value falls back to EXPENSE.
    private fun TransactionEntity.toModel() = SpendTransaction(
        id = id,
        description = description,
        amount = amount,
        category = category,
        date = date,
        kind = if (kind == TransactionKind.INCOME.name) TransactionKind.INCOME else TransactionKind.EXPENSE,
        notes = notes,
        receiptUri = receiptUri,
        voiceNotePath = voiceNotePath
    )

    // Converts the app model into a database row tied to the owner's uid.
    private fun SpendTransaction.toEntity(uid: String) = TransactionEntity(
        id = id,
        ownerUid = uid,
        description = description,
        amount = amount,
        category = category,
        date = date,
        kind = kind.name,
        notes = notes,
        receiptUri = receiptUri,
        voiceNotePath = voiceNotePath
    )
}
