package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ParsedStatementItem(
    val id: String,
    val date: Long,
    val originalDescription: String,
    val cleanTitle: String,
    val amount: Double,
    val type: TransactionType,
    val suggestedCategoryId: String?,
    val confidenceScore: Float = 0.85f,
    val isDuplicate: Boolean = false,
    val isSelected: Boolean = true,
    val note: String = ""
)

@Serializable
data class ParsedStatementResult(
    val detectedBankName: String? = null,
    val detectedAccountNumber: String? = null,
    val suggestedAccountId: String? = null,
    val currency: String = "USD",
    val items: List<ParsedStatementItem> = emptyList()
) {
    val totalExpense: Double
        get() = items.filter { it.isSelected && it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    val totalIncome: Double
        get() = items.filter { it.isSelected && it.type == TransactionType.INCOME }.sumOf { it.amount }

    val selectedCount: Int
        get() = items.count { it.isSelected }
}
