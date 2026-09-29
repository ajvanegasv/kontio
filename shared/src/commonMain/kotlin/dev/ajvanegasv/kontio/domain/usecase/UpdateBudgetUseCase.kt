package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.firstOrNull

class UpdateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        id: String,
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
        val existing = budgetRepository.getBudgetByIdDirect(id)
            ?: return Result.failure(IllegalArgumentException("El presupuesto con ID $id no existe"))

        val category = categoryRepository.getCategoryById(categoryId).firstOrNull()
            ?: return Result.failure(IllegalArgumentException("La categoría seleccionada no existe"))

        val updated = existing.copy(
            name = trimmedName,
            categoryId = categoryId,
            limitAmount = limitAmount,
            period = period,
            currency = currency,
            note = note.trim(),
            category = category
        )

        val result = budgetRepository.updateBudget(updated)
        return if (result.isSuccess) {
            Result.success(updated)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Error al actualizar el presupuesto"))
        }
    }
}
