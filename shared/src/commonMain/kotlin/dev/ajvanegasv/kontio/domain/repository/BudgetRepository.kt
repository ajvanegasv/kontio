package dev.ajvanegasv.kontio.domain.repository

import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgets(): Flow<List<Budget>>
    fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>>
    fun getBudgetById(id: String): Flow<Budget?>
    suspend fun getBudgetByIdDirect(id: String): Budget?
    suspend fun createBudget(budget: Budget): Result<Unit>
    suspend fun updateBudget(budget: Budget): Result<Unit>
    suspend fun deleteBudget(id: String): Result<Unit>
    suspend fun getBudgetsCount(): Int
    suspend fun linkTransactionToBudget(budgetId: String, transactionId: String): Result<Unit>
    suspend fun unlinkTransactionFromBudget(budgetId: String, transactionId: String): Result<Unit>
}
