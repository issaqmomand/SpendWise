
package com.group3.spendwise.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// Each transaction belongs to a Firebase user.
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["ownerUid"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val ownerUid: String,
    val description: String,
    val amount: Double,
    val category: String,
    val date: String,
    val kind: String,

    val notes: String = "",
    val receiptUri: String? = null,
    val voiceNotePath: String? = null
)

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT * FROM transactions
        WHERE ownerUid = :ownerUid
        ORDER BY date DESC, id DESC
        """
    )
    fun observeForUser(
        ownerUid: String
    ): Flow<List<TransactionEntity>>

    // Add a new transaction, including its voice note.
    @Insert
    suspend fun insert(
        transaction: TransactionEntity
    ): Long

    @Query(
        """
        DELETE FROM transactions
        WHERE id = :transactionId
        AND ownerUid = :ownerUid
        """
    )
    suspend fun delete(
        transactionId: Long,
        ownerUid: String
    ): Int

    // Original update function: kept for compatibility.
    @Query(
        """
        UPDATE transactions
        SET description = :description,
            amount = :amount,
            category = :category,
            date = :date,
            kind = :kind,
            notes = :notes,
            receiptUri = :receiptUri
        WHERE id = :transactionId
        AND ownerUid = :ownerUid
        """
    )
    suspend fun update(
        transactionId: Long,
        ownerUid: String,
        description: String,
        amount: Double,
        category: String,
        date: String,
        kind: String,
        notes: String,
        receiptUri: String?
    ): Int

    // New update function: includes voice note attachment.
    @Query(
        """
        UPDATE transactions
        SET description = :description,
            amount = :amount,
            category = :category,
            date = :date,
            kind = :kind,
            notes = :notes,
            receiptUri = :receiptUri,
            voiceNotePath = :voiceNotePath
        WHERE id = :transactionId
        AND ownerUid = :ownerUid
        """
    )
    suspend fun updateWithVoiceNote(
        transactionId: Long,
        ownerUid: String,
        description: String,
        amount: Double,
        category: String,
        date: String,
        kind: String,
        notes: String,
        receiptUri: String?,
        voiceNotePath: String?
    ): Int
}

@Database(
    entities = [TransactionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SpendWiseDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {

        @Volatile
        private var INSTANCE: SpendWiseDatabase? = null

        // Upgrade version 1 databases without deleting data.
        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(
                db: SupportSQLiteDatabase
            ) {
                db.execSQL(
                    """
                    ALTER TABLE transactions
                    ADD COLUMN voiceNotePath TEXT
                    """.trimIndent()
                )
            }
        }

        fun getInstance(
            context: Context
        ): SpendWiseDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SpendWiseDatabase::class.java,
                    "spendwise_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { database ->
                        INSTANCE = database
                    }
            }
        }
    }
}
