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
class TransactionRepository(context: Context) {
    private val dao = SpendWiseDatabase.getInstance(context.applicationContext).transactionDao()

    fun observeForUser(uid: String): Flow<List<SpendTransaction>> =
        if (uid.isBlank()) flowOf(emptyList()) else dao.observeForUser(uid).map { items ->
            items.map { it.toModel() }
        }

    suspend fun insert(uid: String, transaction: SpendTransaction): Long =
        dao.insert(transaction.toEntity(uid).copy(id = 0L))

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

    suspend fun delete(uid: String, id: Long): Int = dao.delete(id, uid)

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
