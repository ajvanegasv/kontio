package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

@Serializable
data class Transaction(
    val id: String,
    val accountId: String,
    val categoryId: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String = "USD",
    val timestamp: Long,
    val note: String = "",
    val targetAccountId: String? = null,
    val aiMetadata: AiMetadata? = null,
    val category: Category? = null,
    val account: Account? = null
)
