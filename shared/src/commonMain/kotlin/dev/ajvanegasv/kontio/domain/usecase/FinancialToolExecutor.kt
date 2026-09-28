package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AiVisualReport
import dev.ajvanegasv.kontio.domain.model.CategorySpending
import dev.ajvanegasv.kontio.domain.model.ChartBarItem
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import kotlinx.coroutines.flow.first

/**
 * Ejecutor local de herramientas financieras para Kontio AI.
 * Permite buscar transacciones, generar gráficos de barras y estructurar reportes visuales.
 */
class FinancialToolExecutor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * Ejecuta una búsqueda de transacciones (ej. "Uber", "Mercado", "Netflix") con filtrado
     * temporal contextual (año, mes, rango de fechas) y genera un reporte visual con KPIs y gráfico Canvas.
     */
    suspend fun executeSearchTransactions(
        query: String,
        year: Int? = null,
        month: Int? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        type: TransactionType? = null,
        displayPeriodLabel: String? = null
    ): AiVisualReport {
        val cleanQuery = query.trim()
        val allMatches = transactionRepository.searchTransactions(cleanQuery)

        val periodSuffix = if (!displayPeriodLabel.isNullOrBlank()) " ($displayPeriodLabel)" else ""
        val reportTitle = "Reporte de Gastos: $cleanQuery$periodSuffix"

        if (allMatches.isEmpty()) {
            return AiVisualReport(
                query = cleanQuery,
                title = reportTitle,
                totalAmount = 0.0,
                transactionCount = 0,
                averageAmount = 0.0,
                chartBars = emptyList(),
                matchingTransactions = emptyList(),
                aiAdvice = "No se encontraron movimientos asociados a '$cleanQuery'. Intenta con otro término o registra transacciones nuevas para ver métricas aquí.",
                isAiGenerated = false
            )
        }

        // 1. Aplicar filtro temporal si se especificó rango de fechas
        val dateFiltered = if (startDate != null && endDate != null) {
            allMatches.filter { it.timestamp in startDate..endDate }
        } else {
            allMatches
        }

        // 2. Aplicar filtro de tipo si se especificó
        val targetTransactions = if (type != null) {
            dateFiltered.filter { it.type == type }
        } else {
            val expenses = dateFiltered.filter { it.type == TransactionType.EXPENSE }
            if (expenses.isNotEmpty()) expenses else dateFiltered
        }

        // Si hay coincidencias globales pero ninguna en el periodo solicitado (ej. Uber en 2026 cuando solo hay en 2025)
        if (targetTransactions.isEmpty() && allMatches.isNotEmpty()) {
            val label = displayPeriodLabel ?: "el periodo solicitado"
            return AiVisualReport(
                query = cleanQuery,
                title = reportTitle,
                totalAmount = 0.0,
                transactionCount = 0,
                averageAmount = 0.0,
                chartBars = emptyList(),
                matchingTransactions = emptyList(),
                aiAdvice = "No se encontraron movimientos de '$cleanQuery' en $label. Se detectaron ${allMatches.size} movimientos en otros periodos anteriores.",
                isAiGenerated = false
            )
        }

        val totalAmount = targetTransactions.sumOf { it.amount }
        val count = targetTransactions.size
        val avg = if (count > 0) totalAmount / count else 0.0
        val currency = targetTransactions.firstOrNull()?.currency ?: "USD"

        // 3. Generar barras visuales adaptativas
        val sortedChronological = targetTransactions.sortedBy { it.timestamp }
        val chartBars = if (year != null && month == null && sortedChronological.isNotEmpty()) {
            // Visualización anual: Agrupar por mes
            val shortMonths = listOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            val groupedByMonth = sortedChronological.groupBy { tx ->
                DateFormatter.toLocalDate(tx.timestamp).monthNumber
            }
            val maxGroup = groupedByMonth.values.maxOfOrNull { list -> list.sumOf { it.amount } } ?: 1.0
            groupedByMonth.map { (mNum, txList) ->
                val sum = txList.sumOf { it.amount }
                val ratio = if (maxGroup > 0) (sum / maxGroup).toFloat().coerceIn(0.12f, 1f) else 0.5f
                ChartBarItem(
                    label = shortMonths.getOrElse(mNum) { "M$mNum" },
                    amount = sum,
                    heightRatio = ratio,
                    timestamp = txList.first().timestamp
                )
            }
        } else if (sortedChronological.size <= 8) {
            val maxAmount = sortedChronological.maxOfOrNull { it.amount } ?: 1.0
            sortedChronological.map { tx ->
                val dateLabel = DateFormatter.formatShortDate(tx.timestamp)
                val ratio = if (maxAmount > 0) (tx.amount / maxAmount).toFloat().coerceIn(0.12f, 1f) else 0.5f
                ChartBarItem(
                    label = dateLabel,
                    amount = tx.amount,
                    heightRatio = ratio,
                    timestamp = tx.timestamp
                )
            }
        } else {
            // Agrupar por mes para gráficos con más de 8 movimientos
            val groupedByMonth = sortedChronological.groupBy { tx ->
                val localDate = DateFormatter.toLocalDate(tx.timestamp)
                "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}"
            }
            val maxGroup = groupedByMonth.values.maxOfOrNull { list -> list.sumOf { it.amount } } ?: 1.0
            groupedByMonth.map { (_, txList) ->
                val sum = txList.sumOf { it.amount }
                val ratio = if (maxGroup > 0) (sum / maxGroup).toFloat().coerceIn(0.12f, 1f) else 0.5f
                val sampleTimestamp = txList.first().timestamp
                val dateLabel = DateFormatter.formatShortDate(sampleTimestamp)
                ChartBarItem(
                    label = dateLabel,
                    amount = sum,
                    heightRatio = ratio,
                    timestamp = sampleTimestamp
                )
            }
        }

        val formattedTotal = CurrencyFormatter.format(totalAmount, currency)
        val formattedAvg = CurrencyFormatter.format(avg, currency)
        val periodIntro = if (!displayPeriodLabel.isNullOrBlank()) "Durante $displayPeriodLabel registraste" else "Registraste"

        val heuristicAdvice = buildString {
            append("$periodIntro $count movimientos relacionados con '$cleanQuery' con un total de $formattedTotal. ")
            append("El promedio por movimiento es de $formattedAvg. ")
            if (count >= 5) {
                append("Es un gasto recurrente importante: considera fijar un límite para optimizar tu flujo de caja.")
            } else {
                append("Tus consumos se mantienen dentro de un rango controlado.")
            }
        }

        return AiVisualReport(
            query = cleanQuery,
            title = reportTitle,
            totalAmount = totalAmount,
            transactionCount = count,
            averageAmount = avg,
            currency = currency,
            chartBars = chartBars,
            matchingTransactions = sortedChronological.reversed(),
            aiAdvice = heuristicAdvice,
            isAiGenerated = false
        )
    }

    /**
     * Agrupa gastos por categoría en un rango de fechas.
     */
    suspend fun getCategorySpendingSummary(startDate: Long, endDate: Long): List<CategorySpending> {
        val transactions = transactionRepository.getTransactionsInDateRangeDirect(startDate, endDate)
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenses = expenses.sumOf { it.amount }

        if (totalExpenses == 0.0) return emptyList()

        val grouped = expenses.groupBy { it.categoryId }
        val categories = categoryRepository.getCategories().first().associateBy { it.id }

        return grouped.map { (catId, list) ->
            val sum = list.sumOf { it.amount }
            val cat = categories[catId]
            val pct = ((sum / totalExpenses) * 100.0).toFloat()

            CategorySpending(
                categoryId = catId,
                categoryName = cat?.name ?: "Otros",
                iconName = cat?.iconName ?: "more_horiz",
                colorHex = cat?.colorHex ?: "#6B7280",
                totalAmount = sum,
                percentage = pct,
                transactionCount = list.size
            )
        }.sortedByDescending { it.totalAmount }
    }
}
