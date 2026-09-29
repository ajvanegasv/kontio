package dev.ajvanegasv.kontio.data.repository

import dev.ajvanegasv.kontio.data.local.dao.BudgetDao
import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.dao.TransactionDao
import dev.ajvanegasv.kontio.data.local.entity.BudgetEntity
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getBudgets(): Flow<List<Budget>> {
        return budgetDao.getAllBudgets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>> {
        return combine(
            budgetDao.getAllBudgets(),
            categoryDao.getAllCategories(),
            transactionDao.getAllTransactions()
        ) { budgetEntities, categoryEntities, transactionEntities ->
            val catMap = categoryEntities.associate { it.id to it.toDomain() }
            val (startOfMonthMillis, endOfMonthMillis) = calculateCurrentMonthBounds()

            // Filtramos las transacciones de tipo gasto dentro del mes actual
            val currentMonthExpenses = transactionEntities.filter { tx ->
                val isExpense = try {
                    TransactionType.valueOf(tx.type) == TransactionType.EXPENSE
                } catch (e: Exception) {
                    tx.type == "EXPENSE"
                }
                isExpense && tx.timestamp in startOfMonthMillis..endOfMonthMillis
            }

            budgetEntities.map { entity ->
                val budget = entity.toDomain()
                val category = catMap[budget.categoryId]

                // Transacciones asociadas: coinciden en categoryId o explícitamente en budgetId
                val matchingTransactions = currentMonthExpenses.filter { tx ->
                    tx.categoryId == budget.categoryId || tx.budgetId == budget.id
                }

                val spent = matchingTransactions.sumOf { it.amount }
                val limit = budget.limitAmount
                val remaining = (limit - spent).coerceAtLeast(0.0)
                val isExceeded = spent > limit
                val exceededAmount = if (isExceeded) spent - limit else 0.0
                val percentage = if (limit > 0.0) (spent / limit).toFloat() else 0f

                BudgetWithProgress(
                    budget = budget.copy(category = category),
                    category = category,
                    spentAmount = spent,
                    limitAmount = limit,
                    remainingAmount = remaining,
                    percentage = percentage,
                    isExceeded = isExceeded,
                    exceededAmount = exceededAmount,
                    transactionsCount = matchingTransactions.size
                )
            }
        }
    }

    override fun getBudgetById(id: String): Flow<Budget?> {
        return budgetDao.getBudgetById(id).map { it?.toDomain() }
    }

    override suspend fun getBudgetByIdDirect(id: String): Budget? {
        return budgetDao.getBudgetByIdDirect(id)?.toDomain()
    }

    override suspend fun createBudget(budget: Budget): Result<Unit> {
        return try {
            budgetDao.insertBudget(BudgetEntity.fromDomain(budget))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> {
        return try {
            budgetDao.updateBudget(BudgetEntity.fromDomain(budget))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        return try {
            budgetDao.deleteBudget(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBudgetsCount(): Int {
        return budgetDao.getBudgetsCount()
    }

    private fun calculateCurrentMonthBounds(): Pair<Long, Long> {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        val start = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
        return start to Long.MAX_VALUE
    }
}
