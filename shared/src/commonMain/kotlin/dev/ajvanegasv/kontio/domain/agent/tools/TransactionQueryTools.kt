package dev.ajvanegasv.kontio.domain.agent.tools

import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionDeclaration
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionParameters
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionProperty
import dev.ajvanegasv.kontio.domain.agent.model.AgentToolResult
import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.FinancialQueryParser
import dev.ajvanegasv.kontio.domain.usecase.FinancialToolExecutor
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Herramienta de solo lectura para buscar transacciones por comercio, nota o concepto,
 * con soporte para filtrado temporal contextual y generación de gráficos de barras.
 */
class SearchTransactionsTool(
    private val financialToolExecutor: FinancialToolExecutor
) : KontioAgentTool {

    override val name: String = "search_transactions"

    override val description: String = """
        Busca transacciones financieras filtrando por comercio o concepto (ej. 'Uber', 'Mercado', 'Netflix', 'Restaurante')
        y opcionalmente por año, mes o periodo temporal.
        Ejemplo: para '¿cuánto he gastado en Uber en 2026?', usa query='Uber' y year=2026.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "query" to GeminiFunctionProperty(type = "STRING", description = "Nombre del comercio o concepto clave, ej. 'Uber'. NUNCA incluir años o fechas aquí."),
                "year" to GeminiFunctionProperty(type = "INTEGER", description = "Año numérico específico a filtrar (ej. 2026, 2025)."),
                "month" to GeminiFunctionProperty(type = "INTEGER", description = "Mes numérico del 1 al 12 a filtrar."),
                "relative_period" to GeminiFunctionProperty(type = "STRING", enum = listOf("CURRENT_MONTH", "PREVIOUS_MONTH", "CURRENT_YEAR", "LAST_3_MONTHS", "LAST_YEAR")),
                "type" to GeminiFunctionProperty(type = "STRING", enum = listOf("EXPENSE", "INCOME", "ALL"))
            ),
            required = listOf("query")
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val rawQuery = args["query"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val year = args["year"]?.jsonPrimitive?.intOrNull
        val month = args["month"]?.jsonPrimitive?.intOrNull
        val typeStr = args["type"]?.jsonPrimitive?.contentOrNull?.uppercase()

        val parsedIntent = FinancialQueryParser.parseQuery("$rawQuery ${year ?: ""} ${month ?: ""}")
        val targetQuery = if (parsedIntent.cleanKeyword.isNotBlank()) parsedIntent.cleanKeyword else rawQuery

        val txType = when (typeStr) {
            "EXPENSE" -> TransactionType.EXPENSE
            "INCOME" -> TransactionType.INCOME
            else -> parsedIntent.type
        }

        val report = financialToolExecutor.executeSearchTransactions(
            query = targetQuery,
            year = year ?: parsedIntent.year,
            month = month ?: parsedIntent.month,
            startDate = parsedIntent.startDateMillis,
            endDate = parsedIntent.endDateMillis,
            type = txType,
            displayPeriodLabel = parsedIntent.displayPeriodLabel
        )

        val jsonSummary = buildJsonObject {
            put("query", report.query)
            put("totalAmount", report.totalAmount)
            put("transactionCount", report.transactionCount)
            put("averageAmount", report.averageAmount)
            put("currency", report.currency)
            putJsonArray("transactions") {
                report.matchingTransactions.take(10).forEach { tx ->
                    add(buildJsonObject {
                        put("id", tx.id)
                        put("note", tx.note)
                        put("amount", tx.amount)
                        put("timestamp", tx.timestamp)
                        tx.category?.let { put("category", it.name) }
                    })
                }
            }
        }

        val speechSummary = if (report.transactionCount > 0) {
            val totalFormatted = CurrencyFormatter.format(report.totalAmount, report.currency)
            "Encontré ${report.transactionCount} movimientos de ${report.query} por un total de $totalFormatted."
        } else {
            "No encontré movimientos registrados para ${report.query}."
        }

        val visualPayload = AgentVisualPayload.TransactionListPayload(
            title = report.title,
            query = report.query,
            transactions = report.matchingTransactions,
            totalAmountRaw = report.totalAmount,
            totalAmountFormatted = CurrencyFormatter.format(report.totalAmount, report.currency),
            count = report.transactionCount,
            averageAmountFormatted = CurrencyFormatter.format(report.averageAmount, report.currency),
            currency = report.currency,
            chartBars = report.chartBars
        )

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = report.aiAdvice,
            speechSummary = speechSummary,
            visualPayload = visualPayload
        )
    }
}

/**
 * Herramienta de solo lectura para consultar las transacciones más recientes registradas.
 */
class GetRecentTransactionsTool(
    private val transactionRepository: TransactionRepository
) : KontioAgentTool {

    override val name: String = "get_recent_transactions"

    override val description: String = """
        Obtiene el listado de las transacciones financieras más recientes del usuario.
        Úsalo cuando pregunten: ¿cuáles fueron mis últimos movimientos?, ¿qué he pagado recientemente?, etc.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "limit" to GeminiFunctionProperty(type = "INTEGER", description = "Cantidad de movimientos a devolver (por defecto 10, máximo 30)."),
                "type" to GeminiFunctionProperty(type = "STRING", enum = listOf("ALL", "EXPENSE", "INCOME", "TRANSFER"))
            ),
            required = emptyList()
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val limit = (args["limit"]?.jsonPrimitive?.intOrNull ?: 10).coerceIn(1, 30)
        val typeStr = args["type"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "ALL"

        val rawTransactions = transactionRepository.getRecentTransactions(limit * 2).first()
        val filtered = if (typeStr != "ALL") {
            rawTransactions.filter { it.type.name == typeStr }.take(limit)
        } else {
            rawTransactions.take(limit)
        }

        if (filtered.isEmpty()) {
            val emptyMsg = "No tienes transacciones registradas recientemente."
            return AgentToolResult(
                toolName = name,
                success = true,
                dataSummary = buildJsonObject { put("count", 0) },
                naturalLanguageSummary = emptyMsg,
                speechSummary = emptyMsg
            )
        }

        val currency = filtered.firstOrNull()?.currency ?: "USD"
        val totalSum = filtered.sumOf { it.amount }

        val jsonSummary = buildJsonObject {
            put("count", filtered.size)
            putJsonArray("transactions") {
                filtered.forEach { tx ->
                    add(buildJsonObject {
                        put("id", tx.id)
                        put("note", tx.note)
                        put("amount", tx.amount)
                        put("type", tx.type.name)
                        put("timestamp", tx.timestamp)
                        tx.category?.let { put("category", it.name) }
                        tx.account?.let { put("account", it.name) }
                    })
                }
            }
        }

        val naturalSummary = buildString {
            append("Últimos ${filtered.size} movimientos registrados:\n\n")
            filtered.forEach { tx ->
                val sign = if (tx.type == TransactionType.INCOME) "+" else "-"
                val amountFormatted = CurrencyFormatter.format(tx.amount, tx.currency)
                val dateFormatted = DateFormatter.formatShortDate(tx.timestamp)
                val title = tx.note.ifBlank { tx.category?.name ?: "Movimiento" }
                val extra = tx.account?.name?.let { " ($it)" } ?: ""
                append("• **$title**: $sign$amountFormatted • $dateFormatted$extra\n")
            }
        }.trimEnd()

        val speechSummary = "Aquí tienes tus últimos ${filtered.size} movimientos registrados."

        val visualPayload = AgentVisualPayload.TransactionListPayload(
            title = "Movimientos Recientes",
            query = "recientes",
            transactions = filtered,
            totalAmountRaw = totalSum,
            totalAmountFormatted = CurrencyFormatter.format(totalSum, currency),
            count = filtered.size,
            averageAmountFormatted = CurrencyFormatter.format(totalSum / filtered.size, currency),
            currency = currency
        )

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = naturalSummary,
            speechSummary = speechSummary,
            visualPayload = visualPayload
        )
    }
}

/**
 * Herramienta de solo lectura para obtener el balance general (ingresos vs gastos vs tasa de ahorro).
 */
class GetFinancialOverviewTool(
    private val transactionRepository: TransactionRepository
) : KontioAgentTool {

    override val name: String = "get_financial_overview"

    override val description: String = """
        Proporciona un resumen global de ingresos, gastos totales, ahorro neto y porcentaje de ahorro
        en un periodo determinado (ej. este mes, el mes pasado, este año).
        Úsalo cuando pregunten: ¿cómo van mis finanzas este mes?, resumen de ingresos y gastos, etc.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "period" to GeminiFunctionProperty(
                    type = "STRING",
                    description = "Periodo a evaluar.",
                    enum = listOf("CURRENT_MONTH", "PREVIOUS_MONTH", "CURRENT_YEAR", "LAST_3_MONTHS")
                )
            ),
            required = emptyList()
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val periodStr = args["period"]?.jsonPrimitive?.contentOrNull ?: "CURRENT_MONTH"
        val queryPeriod = when (periodStr) {
            "PREVIOUS_MONTH" -> "el mes pasado"
            "CURRENT_YEAR" -> "este año"
            "LAST_3_MONTHS" -> "últimos 3 meses"
            else -> "este mes"
        }

        val intent = FinancialQueryParser.parseQuery(queryPeriod)
        val startDate = intent.startDateMillis ?: 0L
        val endDate = intent.endDateMillis ?: Long.MAX_VALUE
        val label = intent.displayPeriodLabel ?: "el periodo solicitado"

        val transactions = if (startDate > 0L && endDate < Long.MAX_VALUE) {
            transactionRepository.getTransactionsInDateRangeDirect(startDate, endDate)
        } else {
            transactionRepository.getAllTransactions().first()
        }

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpenses
        val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100.0).toFloat().coerceIn(-100f, 100f) else 0f
        val currency = transactions.firstOrNull()?.currency ?: "USD"

        val incomeFormatted = CurrencyFormatter.format(totalIncome, currency)
        val expensesFormatted = CurrencyFormatter.format(totalExpenses, currency)
        val savingsFormatted = CurrencyFormatter.format(netSavings, currency)

        val jsonSummary = buildJsonObject {
            put("period", label)
            put("income", totalIncome)
            put("expenses", totalExpenses)
            put("netSavings", netSavings)
            put("savingsRate", savingsRate.toDouble())
            put("currency", currency)
        }

        val naturalSummary = buildString {
            append("Resumen financiero de **$label**:\n\n")
            append("• **Ingresos:** $incomeFormatted\n")
            append("• **Gastos:** $expensesFormatted\n")
            append("• **Ahorro Neto:** $savingsFormatted")
            if (totalIncome > 0) {
                val rateRounded = (savingsRate * 10).toInt() / 10f
                append(" (Tasa de ahorro: $rateRounded%)")
            }
        }

        val speechSummary = "En $label tuviste ingresos de $incomeFormatted y gastos de $expensesFormatted, para un ahorro neto de $savingsFormatted."

        val visualPayload = AgentVisualPayload.FinancialOverviewPayload(
            periodLabel = label,
            incomeFormatted = incomeFormatted,
            expensesFormatted = expensesFormatted,
            savingsFormatted = savingsFormatted,
            savingsRate = savingsRate,
            currency = currency
        )

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = naturalSummary,
            speechSummary = speechSummary,
            visualPayload = visualPayload
        )
    }
}
