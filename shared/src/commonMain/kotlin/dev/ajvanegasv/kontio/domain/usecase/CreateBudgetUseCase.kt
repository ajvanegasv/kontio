package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.firstOrNull

class CreateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        categoryId: String,
        limitAmount: Double,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        currency: String = "USD",
        note: String = ""
    ): Result<Budget> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("El nombre del presupuesto no puede estar vacío"))
        }
        if (limitAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("El monto límite debe ser mayor a 0"))
        }
        val category = categoryRepository.getCategoryById(categoryId).firstOrNull()
        if (category == null) {
            return Result.failure(IllegalArgumentException("La categoría seleccionada no existe"))
        }

        val id = "budget_${kotlin.time.Clock.System.now().toEpochMilliseconds()}_${(100..999).random()}"
        val budget = Budget(
            id = id,
            name = trimmedName,
            categoryId = categoryId,
            limitAmount = limitAmount,
            period = period,
            currency = currency,
            note = note.trim(),
            createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            category = category
        )

        val result = budgetRepository.createBudget(budget)
        return if (result.isSuccess) {
            Result.success(budget)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error al crear el presupuesto"))
        }
    }
}
