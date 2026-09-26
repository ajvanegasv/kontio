package dev.ajvanegasv.kontio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ajvanegasv.kontio.domain.model.AiMetadata
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
@Entity(
    tableName = "transactions",
    indices = [
        Index("accountId"),
        Index("categoryId"),
        Index("timestamp")
    ]
)
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val accountId: String,
    val categoryId: String,
    val type: String, // TransactionType.name
    val amount: Double,
    val currency: String,
    val timestamp: Long,
    val note: String,
    val targetAccountId: String?,
    val aiMetadataJson: String?
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        accountId = accountId,
        categoryId = categoryId,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        amount = amount,
        currency = currency,
        timestamp = timestamp,
        note = note,
        targetAccountId = targetAccountId,
        aiMetadata = aiMetadataJson?.let {
            try { Json.decodeFromString<AiMetadata>(it) } catch (e: Exception) { null }
        }
    )

    companion object {
        fun fromDomain(tx: Transaction): TransactionEntity = TransactionEntity(
            id = tx.id,
            accountId = tx.accountId,
            categoryId = tx.categoryId,
            type = tx.type.name,
            amount = tx.amount,
            currency = tx.currency,
            timestamp = tx.timestamp,
            note = tx.note,
            targetAccountId = tx.targetAccountId,
            aiMetadataJson = tx.aiMetadata?.let {
                try { Json.encodeToString(it) } catch (e: Exception) { null }
            }
        )
    }
}
