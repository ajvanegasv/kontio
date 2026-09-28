package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AnalyticsSummary
import dev.ajvanegasv.kontio.domain.model.AnalyticsTimeframe
import dev.ajvanegasv.kontio.domain.model.CategorySpending
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Caso de uso reactivo que genera el resumen estadístico de Analytics para el periodo seleccionado.
 */
class GetAnalyticsSummaryUseCase(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    operator fun invoke(timeframe: AnalyticsTimeframe): Flow<AnalyticsSummary> {
        val (startDateMillis, endDateMillis) = calculateTimeframeBounds(timeframe)

        return combine(
            transactionRepository.getAllTransactions(),
            categoryRepository.getCategories()
        ) { allTransactions, categories ->
            val catMap = categories.associateBy { it.id }

            // Filtrar por el rango de fechas seleccionado
            val filteredTransactions = if (timeframe == AnalyticsTimeframe.ALL_TIME) {
                allTransactions
            } else {
                allTransactions.filter { it.timestamp in startDateMillis..endDateMillis }
            }

            var incomeSum = 0.0
            var expensesSum = 0.0
            val expenseMap = mutableMapOf<String, MutableList<Double>>()
            val expenseCounts = mutableMapOf<String, Int>()

            filteredTransactions.forEach { tx ->
                when (tx.type) {
                    TransactionType.INCOME -> incomeSum += tx.amount
                    TransactionType.EXPENSE -> {
                        expensesSum += tx.amount
                        expenseMap.getOrPut(tx.categoryId) { mutableListOf() }.add(tx.amount)
                        expenseCounts[tx.categoryId] = (expenseCounts[tx.categoryId] ?: 0) + 1
                    }
                    TransactionType.TRANSFER -> {
                        // Las transferencias entre cuentas no son gastos netos
                    }
                }
            }

            val categorySpendings = expenseMap.map { (catId, amounts) ->
                val catTotal = amounts.sum()
                val pct = if (expensesSum > 0.0) ((catTotal / expensesSum) * 100.0).toFloat() else 0f
                val cat = catMap[catId]

                CategorySpending(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Otros Gastos",
                    iconName = cat?.iconName ?: "more_horiz",
                    colorHex = cat?.colorHex ?: "#6B7280",
                    totalAmount = catTotal,
                    percentage = pct,
                    transactionCount = expenseCounts[catId] ?: amounts.size
                )
            }.sortedByDescending { it.totalAmount }

            val netSavings = incomeSum - expensesSum
            val savingsRate = if (incomeSum > 0.0) {
                ((netSavings / incomeSum) * 100.0).toFloat().coerceIn(0f, 100f)
            } else 0f

            val currency = filteredTransactions.firstOrNull()?.currency ?: "USD"

            AnalyticsSummary(
                totalIncome = incomeSum,
                totalExpenses = expensesSum,
                netSavings = netSavings,
                savingsRate = savingsRate,
                categorySpendings = categorySpendings,
                timeframe = timeframe,
                currency = currency
            )
        }
    }

    private fun calculateTimeframeBounds(timeframe: AnalyticsTimeframe): Pair<Long, Long> {
        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date

        return when (timeframe) {
            AnalyticsTimeframe.CURRENT_MONTH -> {
                val start = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                start to now.toEpochMilliseconds()
            }
            AnalyticsTimeframe.LAST_3_MONTHS -> {
                val start = now.minus(90, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()
                start to now.toEpochMilliseconds()
            }
            AnalyticsTimeframe.CURRENT_YEAR -> {
                val start = LocalDate(today.year, 1, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                start to now.toEpochMilliseconds()
            }
            AnalyticsTimeframe.ALL_TIME -> {
                0L to now.toEpochMilliseconds()
            }
        }
    }
}
