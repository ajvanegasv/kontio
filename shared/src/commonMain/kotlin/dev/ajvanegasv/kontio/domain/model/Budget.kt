package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

enum class BudgetPeriod {
    MONTHLY,
    WEEKLY,
    ANNUAL,
    ONE_TIME
}

@Serializable
data class Budget(
    val id: String,
    val name: String,
    val categoryId: String,
    val limitAmount: Double,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val currency: String = "USD",
    val note: String = "",
    val createdAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val category: Category? = null
)

data class BudgetWithProgress(
    val budget: Budget,
    val category: Category?,
    val spentAmount: Double,
    val limitAmount: Double,
    val remainingAmount: Double,
    val percentage: Float,
    val isExceeded: Boolean,
    val exceededAmount: Double,
    val transactionsCount: Int,
    val lastTransaction: Transaction? = null
)
