package dev.ajvanegasv.kontio.domain.agent.tools

import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionDeclaration
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionParameters
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionProperty
import dev.ajvanegasv.kontio.domain.agent.model.AgentToolResult
import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.FinancialQueryParser
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Herramienta de solo lectura para listar el catálogo de categorías (Gastos o Ingresos)
 * configuradas en la app Kontio.
 */
class GetCategoriesTool(
    private val categoryRepository: CategoryRepository
) : KontioAgentTool {

    override val name: String = "get_categories"

    override val description: String = """
        Lista las categorías financieras configuradas en la app (gastos e ingresos),
        con sus iconos y colores representativos.
        Úsalo cuando el usuario pregunte: ¿qué categorías tengo?, ¿en qué categorías puedo clasificar?, etc.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "type" to GeminiFunctionProperty(
                    type = "STRING",
                    description = "Filtro opcional por tipo de categoría: 'EXPENSE' (gastos), 'INCOME' (ingresos), o 'ALL'.",
                    enum = listOf("ALL", "EXPENSE", "INCOME")
                )
            ),
            required = emptyList()
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val typeFilter = args["type"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "ALL"
        val txType = when (typeFilter) {
            "EXPENSE" -> TransactionType.EXPENSE
            "INCOME" -> TransactionType.INCOME
            else -> null
        }

        val categories = categoryRepository.getCategories(txType).first()

        val jsonSummary = buildJsonObject {
            put("count", categories.size)
            putJsonArray("categories") {
                categories.forEach { cat ->
                    add(buildJsonObject {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("type", cat.type.name)
                        put("iconName", cat.iconName)
                        put("colorHex", cat.colorHex)
                    })
                }
            }
        }

        val naturalSummary = buildString {
            val label = when (typeFilter) {
                "EXPENSE" -> "de gastos"
                "INCOME" -> "de ingresos"
                else -> "registradas"
            }
            append("Tienes ${categories.size} categorías $label:\n\n")
            val expenses = categories.filter { it.type == TransactionType.EXPENSE }
            val incomes = categories.filter { it.type == TransactionType.INCOME }

            if (expenses.isNotEmpty() && typeFilter != "INCOME") {
                append("**Gastos:** ")
                append(expenses.joinToString(", ") { it.name })
                append("\n")
            }
            if (incomes.isNotEmpty() && typeFilter != "EXPENSE") {
                append("**Ingresos:** ")
                append(incomes.joinToString(", ") { it.name })
            }
        }.trimEnd()

        val speechSummary = "Tienes ${categories.size} categorías configuradas en la aplicación."

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = naturalSummary,
            speechSummary = speechSummary
        )
    }
}

/**
 * Herramienta de solo lectura para consultar el desglose y distribución del gasto por categoría
 * en un periodo temporal (ej. mes actual, mes pasado, año).
 */
class GetCategorySpendingTool(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : KontioAgentTool {

    override val name: String = "get_category_spending"

    override val description: String = """
        Calcula el ranking y desglose porcentual de gastos por cada categoría en un periodo temporal determinado.
        Úsalo cuando el usuario pregunte: ¿en qué categoría gasto más?, ¿cuánto gasté por categorías este mes?, etc.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "period" to GeminiFunctionProperty(
                    type = "STRING",
                    description = "Periodo temporal a evaluar.",
                    enum = listOf("CURRENT_MONTH", "PREVIOUS_MONTH", "CURRENT_YEAR", "LAST_3_MONTHS", "ALL_TIME")
                ),
                "year" to GeminiFunctionProperty(type = "INTEGER", description = "Año numérico específico (ej. 2026, 2025)."),
                "month" to GeminiFunctionProperty(type = "INTEGER", description = "Mes numérico del 1 al 12.")
            ),
            required = emptyList()
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val periodStr = args["period"]?.jsonPrimitive?.contentOrNull ?: "CURRENT_MONTH"
        val year = args["year"]?.jsonPrimitive?.intOrNull
        val month = args["month"]?.jsonPrimitive?.intOrNull

        // Resolver rango temporal usando FinancialQueryParser
        val simulatedQuery = buildString {
            when (periodStr) {
                "PREVIOUS_MONTH" -> append("el mes pasado")
                "CURRENT_YEAR" -> append("este año")
                "LAST_3_MONTHS" -> append("últimos 3 meses")
                "ALL_TIME" -> append("todo el tiempo")
                else -> append("este mes")
            }
            if (year != null) append(" $year")
            if (month != null) append(" mes $month")
        }

        val intent = FinancialQueryParser.parseQuery(simulatedQuery)
        val startDate = intent.startDateMillis ?: 0L
        val endDate = intent.endDateMillis ?: Long.MAX_VALUE
        val displayPeriod = intent.displayPeriodLabel ?: "el periodo solicitado"

        val transactions = if (startDate > 0L && endDate < Long.MAX_VALUE) {
            transactionRepository.getTransactionsInDateRangeDirect(startDate, endDate)
        } else {
            transactionRepository.getAllTransactions().first()
        }

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalExpenses = expenses.sumOf { it.amount }
        val currency = expenses.firstOrNull()?.currency ?: "USD"

        if (expenses.isEmpty() || totalExpenses == 0.0) {
            val emptyMsg = "No se registraron gastos en $displayPeriod."
            return AgentToolResult(
                toolName = name,
                success = true,
                dataSummary = buildJsonObject {
                    put("count", 0)
                    put("totalExpenses", 0.0)
                    put("period", displayPeriod)
                },
                naturalLanguageSummary = emptyMsg,
                speechSummary = emptyMsg,
                visualPayload = AgentVisualPayload.CategoryBreakdownPayload(
                    timeframeLabel = displayPeriod,
                    categories = emptyList(),
                    totalAmountFormatted = CurrencyFormatter.format(0.0, currency),
                    totalAmountRaw = 0.0,
                    currency = currency
                )
            )
        }

        val categoriesMap = categoryRepository.getCategories().first().associateBy { it.id }
        val grouped = expenses.groupBy { it.categoryId }

        val spendingItems = grouped.map { (catId, list) ->
            val sum = list.sumOf { it.amount }
            val cat = categoriesMap[catId]
            val pct = ((sum / totalExpenses) * 100.0).toFloat()

            AgentVisualPayload.CategorySpendingItem(
                categoryId = catId,
                name = cat?.name ?: "Otros",
                amountFormatted = CurrencyFormatter.format(sum, currency),
                amountRaw = sum,
                percentage = pct,
                colorHex = cat?.colorHex ?: "#6B7280",
                iconName = cat?.iconName ?: "more_horiz",
                transactionCount = list.size
            )
        }.sortedByDescending { it.amountRaw }

        val totalExpensesFormatted = CurrencyFormatter.format(totalExpenses, currency)

        val jsonSummary = buildJsonObject {
            put("period", displayPeriod)
            put("totalExpenses", totalExpenses)
            put("currency", currency)
            putJsonArray("categories") {
                spendingItems.forEach { item ->
                    add(buildJsonObject {
                        put("name", item.name)
                        put("amount", item.amountRaw)
                        put("percentage", item.percentage.toDouble())
                        put("transactionCount", item.transactionCount)
                    })
                }
            }
        }

        val topCategory = spendingItems.firstOrNull()
        val naturalSummary = buildString {
            append("Durante $displayPeriod, tu gasto total fue de $totalExpensesFormatted.\n\n")
            spendingItems.forEach { item ->
                val pctRounded = (item.percentage * 10).toInt() / 10f
                append("• **${item.name}**: ${item.amountFormatted} ($pctRounded%)\n")
            }
            if (topCategory != null) {
                append("\nTu categoría con mayor gasto fue **${topCategory.name}** representando el ${(topCategory.percentage * 10).toInt() / 10f}% del total.")
            }
        }.trimEnd()

        val speechSummary = if (topCategory != null) {
            "Tu gasto total en $displayPeriod fue de $totalExpensesFormatted. La categoría con mayor gasto fue ${topCategory.name}."
        } else {
            "Tu gasto total en $displayPeriod fue de $totalExpensesFormatted."
        }

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = naturalSummary,
            speechSummary = speechSummary,
            visualPayload = AgentVisualPayload.CategoryBreakdownPayload(
                timeframeLabel = displayPeriod,
                categories = spendingItems,
                totalAmountFormatted = totalExpensesFormatted,
                totalAmountRaw = totalExpenses,
                currency = currency
            )
        )
    }
}
