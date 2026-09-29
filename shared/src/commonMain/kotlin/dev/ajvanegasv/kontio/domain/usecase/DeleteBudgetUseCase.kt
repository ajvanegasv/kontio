package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.repository.BudgetRepository

class DeleteBudgetUseCase(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return budgetRepository.deleteBudget(id)
    }
}
