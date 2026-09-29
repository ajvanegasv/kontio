package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow

class GetBudgetsWithProgressUseCase(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(): Flow<List<BudgetWithProgress>> {
        return budgetRepository.getBudgetsWithProgress()
    }
}
