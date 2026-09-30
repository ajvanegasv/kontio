package dev.ajvanegasv.kontio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "budget_transactions",
    indices = [
        Index("budgetId"),
        Index("transactionId")
    ]
)
data class BudgetTransactionEntity(
    @PrimaryKey
    val id: String,
    val budgetId: String,
    val transactionId: String,
    val createdAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
)
