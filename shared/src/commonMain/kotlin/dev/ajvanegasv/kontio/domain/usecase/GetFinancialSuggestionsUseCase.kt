package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiContent
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiGenerationConfig
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiPart
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiRequest
import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.FinancialSuggestion
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

/**
 * Caso de uso que genera sugerencias de consulta dinámicas y personalizadas para el usuario.
 *
 * Utiliza las herramientas de consulta del agente (KontioToolRegistry) para extraer un snapshot
 * real de sus finanzas (cuentas, tarjetas, categorías principales, comercios frecuentes y balance).
 *
 * - Modo Online: Si Gemini está activo y configurado con API Key, solicita al modelo redactar de
 *   4 a 6 sugerencias naturales y contextuales en formato JSON.
 * - Modo Offline / Fallback Local: Si no hay conexión o no hay API Key, un generador heurístico
 *   construye sugerencias personalizadas a partir de los datos exactos devueltos por las tools.
 */
class GetFinancialSuggestionsUseCase(
    private val toolRegistry: KontioToolRegistry,
    private val aiConfigStorage: AiConfigStorage,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var cachedSuggestions: List<FinancialSuggestion>? = null

    suspend operator fun invoke(forceRefresh: Boolean = false): List<FinancialSuggestion> {
        if (!forceRefresh && cachedSuggestions != null) {
            return cachedSuggestions!!
        }

        // 1. Recopilar datos reales de las finanzas del usuario ejecutando las tools
        val accountsPayload = toolRegistry.execute("get_accounts_summary", emptyMap()).visualPayload
                as? AgentVisualPayload.AccountsSummaryPayload

        val categoryPayload = toolRegistry.execute(
            "get_category_spending",
            mapOf("period" to JsonPrimitive("CURRENT_MONTH"))
        ).visualPayload as? AgentVisualPayload.CategoryBreakdownPayload

        val recentTxPayload = toolRegistry.execute(
            "get_recent_transactions",
            mapOf("limit" to JsonPrimitive(10))
        ).visualPayload as? AgentVisualPayload.TransactionListPayload

        val overviewPayload = toolRegistry.execute(
            "get_financial_overview",
            mapOf("period" to JsonPrimitive("CURRENT_MONTH"))
        ).visualPayload as? AgentVisualPayload.FinancialOverviewPayload

        // 2. Intentar generación inteligente con Gemini (Modo Online)
        val isAiEnabled = aiConfigStorage.isAiEnabled()
        val apiKey = aiConfigStorage.getApiKey()
        val model = aiConfigStorage.getModel().ifBlank { GeminiApiClient.DEFAULT_MODEL }

        if (isAiEnabled && !apiKey.isNullOrBlank()) {
            val aiGenerated = generateWithGemini(
                apiKey = apiKey,
                model = model,
                accounts = accountsPayload,
                categories = categoryPayload,
                recentTxs = recentTxPayload,
                overview = overviewPayload
            )
            if (aiGenerated.isNotEmpty()) {
                cachedSuggestions = aiGenerated
                return aiGenerated
            }
        }

        // 3. Fallback: Generador Heurístico Local basado en los datos de las herramientas
        val localSuggestions = generateLocalSuggestions(
            accounts = accountsPayload,
            categories = categoryPayload,
            recentTxs = recentTxPayload,
            overview = overviewPayload
        )

        cachedSuggestions = localSuggestions
        return localSuggestions
    }

    private suspend fun generateWithGemini(
        apiKey: String,
        model: String,
        accounts: AgentVisualPayload.AccountsSummaryPayload?,
        categories: AgentVisualPayload.CategoryBreakdownPayload?,
        recentTxs: AgentVisualPayload.TransactionListPayload?,
        overview: AgentVisualPayload.FinancialOverviewPayload?
    ): List<FinancialSuggestion> {
        return try {
            val accountsInfo = accounts?.accounts?.joinToString("; ") { acc ->
                val credit = acc.availableCreditFormatted?.let { ", cupo disponible: $it" }.orEmpty()
                "${acc.name} (${acc.type.name}): saldo ${acc.balanceFormatted}$credit"
            } ?: "Sin cuentas registradas"

            val topCats = categories?.categories?.take(3)?.joinToString("; ") {
                "${it.name}: ${it.amountFormatted} (${it.percentage}%)"
            } ?: "Sin gastos registrados este mes"

            val merchants = recentTxs?.transactions?.mapNotNull { tx ->
                tx.note.takeIf { it.isNotBlank() } ?: tx.category?.name
            }?.distinct()?.take(4)?.joinToString(", ") ?: "Sin transacciones recientes"

            val overviewInfo = overview?.let {
                "Ingresos: ${it.incomeFormatted}, Gastos: ${it.expensesFormatted}, Ahorro: ${it.savingsFormatted} (${it.savingsRate}%)"
            } ?: "Sin datos de balance"

            val prompt = """
                Eres el motor de sugerencias analíticas de Kontio AI.
                Tu tarea es generar entre 4 y 6 sugerencias de consultas financieras personalizadas y útiles,
                basándote estrictamente en los datos financieros reales del usuario y en las herramientas disponibles.

                HERRAMIENTAS DE CONSULTA DISPONIBLES:
                - get_accounts_summary: Consultar saldos y cuentas bancarias.
                - get_account_detail: Consultar el saldo o cupo de una cuenta o tarjeta específica por su nombre.
                - get_category_spending: Desglose de gastos por categoría en un periodo.
                - search_transactions: Consultar gastos en comercios específicos (ej. Uber, Rappi, Netflix) o por año/mes.
                - get_recent_transactions: Consultar los últimos movimientos financieros.
                - get_financial_overview: Balance de ingresos vs gastos y tasa de ahorro.

                DATOS FINANCIEROS REALES DEL USUARIO:
                - Cuentas y Tarjetas: $accountsInfo
                - Categorías con mayor gasto este mes: $topCats
                - Comercios o conceptos recientes: $merchants
                - Balance general: $overviewInfo

                DIRECTRICES:
                1. Genera preguntas directas y relevantes que el usuario querría consultar sobre SUS datos.
                2. Si el usuario tiene tarjetas de crédito con cupo, sugiere consultar su saldo o cupo.
                3. Si tiene comercios frecuentes (ej. Rappi, Uber, Mercado), sugiere consultar gastos en ese comercio específico.
                4. Si tiene una categoría con mayor gasto (ej. Restaurantes), sugiere consultar el gasto en esa categoría.
                5. Cada sugerencia debe tener:
                   - "id": string único (ej. "sug_1")
                   - "label": texto muy corto para el chip con un emoji inicial representativo (máximo 4 palabras, ej. "💳 Cupo en Nu", "🍔 Gastos en Restaurantes", "🚗 Gastos en Uber")
                   - "query": la pregunta natural completa para ejecutar (ej. "¿Cuál es el saldo y cupo de mi tarjeta Nu?", "¿Cuánto he gastado en Restaurantes este mes?")
                   - "toolName": el nombre de la herramienta de consulta asociada.

                Responde ÚNICAMENTE con un JSON array válido de objetos.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.3f
                )
            )

            val responseResult = geminiApiClient.generateContent(
                apiKey = apiKey,
                request = request,
                model = model
            )

            val rawJson = responseResult.getOrNull()?.trim().orEmpty()
            if (rawJson.isBlank()) return emptyList()

            val cleanJson = rawJson
                .removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            json.decodeFromString<List<FinancialSuggestion>>(cleanJson)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun generateLocalSuggestions(
        accounts: AgentVisualPayload.AccountsSummaryPayload?,
        categories: AgentVisualPayload.CategoryBreakdownPayload?,
        recentTxs: AgentVisualPayload.TransactionListPayload?,
        overview: AgentVisualPayload.FinancialOverviewPayload?
    ): List<FinancialSuggestion> {
        val suggestions = mutableListOf<FinancialSuggestion>()

        val userAccounts = accounts?.accounts.orEmpty()
        val creditCards = userAccounts.filter { it.type == AccountType.CREDIT_CARD }
        val bankAccounts = userAccounts.filter { it.type != AccountType.CREDIT_CARD && it.type != AccountType.CASH }

        // 1. Sugerencia de Tarjeta de Crédito (si tiene alguna tarjeta registrada)
        if (creditCards.isNotEmpty()) {
            val card = creditCards.first()
            suggestions.add(
                FinancialSuggestion(
                    id = "sug_cc_${card.id}",
                    label = "💳 Cupo en ${card.name}",
                    query = "¿Cuál es el saldo y cupo de mi tarjeta ${card.name}?",
                    toolName = "get_account_detail"
                )
            )
        }

        // 2. Sugerencia de Banco / Cuentas
        if (bankAccounts.isNotEmpty()) {
            val bank = bankAccounts.first()
            suggestions.add(
                FinancialSuggestion(
                    id = "sug_bank_${bank.id}",
                    label = "💰 Saldo en ${bank.name}",
                    query = "¿Cuánto dinero tengo en ${bank.name}?",
                    toolName = "get_account_detail"
                )
            )
        } else {
            suggestions.add(
                FinancialSuggestion(
                    id = "sug_accounts_all",
                    label = "🏦 Mis Bancos y Saldos",
                    query = "¿Cuáles son mis cuentas y cuánto dinero tengo?",
                    toolName = "get_accounts_summary"
                )
            )
        }

        // 3. Sugerencia de la Categoría con mayor gasto
        val topCategory = categories?.categories?.maxByOrNull { it.amountRaw }
        if (topCategory != null && topCategory.amountRaw > 0) {
            suggestions.add(
                FinancialSuggestion(
                    id = "sug_cat_${topCategory.categoryId}",
                    label = "📊 Gastos en ${topCategory.name}",
                    query = "¿Cuánto he gastado en ${topCategory.name} este mes?",
                    toolName = "get_category_spending"
                )
            )
        } else {
            suggestions.add(
                FinancialSuggestion(
                    id = "sug_cat_ranking",
                    label = "📊 Gastos por Categoría",
                    query = "¿En qué categorías gasto más este mes?",
                    toolName = "get_category_spending"
                )
            )
        }

        // 4. Sugerencia de Comercio específico detectado en movimientos recientes
        val candidateMerchants = recentTxs?.transactions?.mapNotNull { tx ->
            val note = tx.note.trim()
            if (note.length >= 3 && !note.all { it.isDigit() }) note else null
        }?.distinct().orEmpty()

        val topMerchant = candidateMerchants.firstOrNull()
        if (topMerchant != null) {
            suggestions.add(
                FinancialSuggestion(
                    id = "sug_merchant_$topMerchant",
                    label = "🏷️ Gastos en $topMerchant",
                    query = "¿Cuánto he gastado en $topMerchant este año?",
                    toolName = "search_transactions"
                )
            )
        }

        // 5. Sugerencia de Balance / Resumen Financiero
        suggestions.add(
            FinancialSuggestion(
                id = "sug_overview",
                label = "💡 Balance del Mes",
                query = "¿Cómo van mis finanzas este mes?",
                toolName = "get_financial_overview"
            )
        )

        // 6. Sugerencia de Movimientos Recientes
        suggestions.add(
            FinancialSuggestion(
                id = "sug_recent_txs",
                label = "🕒 Últimos Movimientos",
                query = "¿Cuáles fueron mis últimas transacciones?",
                toolName = "get_recent_transactions"
            )
        )

        // Limitar a máximo 6 sugerencias
        return suggestions.distinctBy { it.label }.take(6)
    }

    fun clearCache() {
        cachedSuggestions = null
    }
}
