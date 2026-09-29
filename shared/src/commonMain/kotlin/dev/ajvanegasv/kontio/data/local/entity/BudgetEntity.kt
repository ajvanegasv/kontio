package dev.ajvanegasv.kontio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "budgets",
    indices = [
        Index("categoryId")
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val categoryId: String,
    val limitAmount: Double,
    val period: String, // BudgetPeriod.name
    val currency: String,
    val note: String,
    val createdAt: Long
) {
    fun toDomain(): Budget = Budget(
        id = id,
        name = name,
        categoryId = categoryId,
        limitAmount = limitAmount,
        period = try { BudgetPeriod.valueOf(period) } catch (e: Exception) { BudgetPeriod.MONTHLY },
        currency = currency,
        note = note,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(budget: Budget): BudgetEntity = BudgetEntity(
            id = budget.id,
            name = budget.name,
            categoryId = budget.categoryId,
            limitAmount = budget.limitAmount,
            period = budget.period.name,
            currency = budget.currency,
            note = budget.note,
            createdAt = budget.createdAt
        )
    }
}
