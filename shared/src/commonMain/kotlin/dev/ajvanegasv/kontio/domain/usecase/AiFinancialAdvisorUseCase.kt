package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiContent
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionDeclaration
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionParameters
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionProperty
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiGenerationConfig
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiPart
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiRequest
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiTool
import dev.ajvanegasv.kontio.domain.model.AiVisualReport
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Caso de uso que orquesta el análisis financiero asistido por IA, la comprensión contextual
 * de lenguaje natural y la ejecución de tools (con soporte para Gemini Function Calling y Parser Semántico local).
 */
class AiFinancialAdvisorUseCase(
    private val toolExecutor: FinancialToolExecutor,
    private val aiConfigStorage: AiConfigStorage,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {

    private val searchTool = GeminiTool(
        functionDeclarations = listOf(
            GeminiFunctionDeclaration(
                name = "search_transactions",
                description = "Busca transacciones financieras filtrando por comercio o concepto, y opcionalmente por año, mes o periodo temporal. Ejemplo: para '¿cuánto he gastado en Uber en 2026?', usa query='Uber' y year=2026.",
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
        )
    )

    suspend operator fun invoke(userQuery: String): AiVisualReport {
        val cleanQuery = userQuery.trim()
        if (cleanQuery.isBlank()) {
            return AiVisualReport(
                query = "",
                title = "Consulta vacía",
                totalAmount = 0.0,
                transactionCount = 0,
                averageAmount = 0.0,
                chartBars = emptyList(),
                matchingTransactions = emptyList(),
                aiAdvice = "Por favor escribe una consulta o selecciona una sugerencia como 'Uber' o 'Comida'.",
                isAiGenerated = false
            )
        }

        // 1. Análisis semántico preliminar local (determina entidad y periodos)
        val localIntent = FinancialQueryParser.parseQuery(cleanQuery)

        val apiKey = aiConfigStorage.getApiKey()
        val isAiEnabled = aiConfigStorage.isAiEnabled()
        val model = aiConfigStorage.getModel().ifBlank { GeminiApiClient.DEFAULT_MODEL }

        // 2. Si la IA está disponible, intentamos el flujo agentic con Gemini Function Calling
        if (isAiEnabled && !apiKey.isNullOrBlank()) {
            try {
                val candidateResult = geminiApiClient.generateCandidate(
                    apiKey = apiKey,
                    request = GeminiRequest(
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = cleanQuery))
                            )
                        ),
                        tools = listOf(searchTool),
                        generationConfig = GeminiGenerationConfig(temperature = 0.1f)
                    ),
                    model = model
                )

                if (candidateResult.isSuccess) {
                    val candidate = candidateResult.getOrThrow()
                    val functionCall = candidate.content?.parts?.firstOrNull()?.functionCall

                    if (functionCall != null && functionCall.name == "search_transactions") {
                        val args = functionCall.args.orEmpty()
                        val geminiKeyword = args["query"]?.jsonPrimitive?.contentOrNull
                        val geminiYear = args["year"]?.jsonPrimitive?.intOrNull ?: localIntent.year
                        val geminiMonth = args["month"]?.jsonPrimitive?.intOrNull ?: localIntent.month

                        val targetKeyword = if (!geminiKeyword.isNullOrBlank()) geminiKeyword else localIntent.cleanKeyword

                        // Recalcular fechas con los parámetros refinados por Gemini
                        val resolvedIntent = if (geminiYear != null || geminiMonth != null) {
                            FinancialQueryParser.parseQuery("$targetKeyword ${geminiYear ?: ""} ${geminiMonth ?: ""}")
                        } else localIntent

                        val report = toolExecutor.executeSearchTransactions(
                            query = targetKeyword,
                            year = geminiYear,
                            month = geminiMonth,
                            startDate = resolvedIntent.startDateMillis,
                            endDate = resolvedIntent.endDateMillis,
                            type = resolvedIntent.type,
                            displayPeriodLabel = resolvedIntent.displayPeriodLabel
                        )

                        // Solicitar a Gemini el consejo financiero contextual
                        val advice = fetchGeminiAdvice(
                            apiKey = apiKey,
                            model = model,
                            userQuery = cleanQuery,
                            report = report,
                            periodLabel = resolvedIntent.displayPeriodLabel
                        )

                        return report.copy(
                            aiAdvice = advice ?: report.aiAdvice,
                            isAiGenerated = advice != null
                        )
                    }
                }
            } catch (_: Exception) {
                // Si falla la invocación con tools, continuamos con el parser semántico local
            }
        }

        // 3. Flujo local robusto con el parser semántico (offline / fallback)
        val localReport = toolExecutor.executeSearchTransactions(
            query = localIntent.cleanKeyword,
            year = localIntent.year,
            month = localIntent.month,
            startDate = localIntent.startDateMillis,
            endDate = localIntent.endDateMillis,
            type = localIntent.type,
            displayPeriodLabel = localIntent.displayPeriodLabel
        )

        // Si tenemos API Key activa y hay transacciones, intentamos generar el consejo financiero con Gemini
        if (isAiEnabled && !apiKey.isNullOrBlank() && localReport.transactionCount > 0) {
            val advice = fetchGeminiAdvice(
                apiKey = apiKey,
                model = model,
                userQuery = cleanQuery,
                report = localReport,
                periodLabel = localIntent.displayPeriodLabel
            )
            if (advice != null) {
                return localReport.copy(
                    aiAdvice = advice,
                    isAiGenerated = true
                )
            }
        }

        return localReport
    }

    private suspend fun fetchGeminiAdvice(
        apiKey: String,
        model: String,
        userQuery: String,
        report: AiVisualReport,
        periodLabel: String?
    ): String? = runCatching {
        val systemPrompt = """
            Eres el asesor financiero inteligente de Kontio.
            Brinda una recomendación concisa (máximo 2 a 3 oraciones), profesional y directa
            orientada a la optimización del dinero. Responde en español sin markdown complejo ni títulos.
        """.trimIndent()

        val periodText = if (!periodLabel.isNullOrBlank()) "en $periodLabel" else ""
        val promptData = """
            Pregunta del usuario: "$userQuery"
            Datos financieros encontrados:
            - Concepto/Comercio: ${report.query} $periodText
            - Total gastado: ${report.totalAmount} ${report.currency}
            - Cantidad de movimientos: ${report.transactionCount}
            - Promedio por movimiento: ${report.averageAmount} ${report.currency}
            - Movimientos: ${report.matchingTransactions.take(5).joinToString("; ") { "${it.note}: ${it.amount} ${it.currency}" }}
            
            Escribe un consejo financiero accionable para el usuario sobre este gasto.
        """.trimIndent()

        val request = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = promptData))
                )
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.4f)
        )

        geminiApiClient.generateContent(
            apiKey = apiKey,
            request = request,
            model = model
        ).getOrNull()?.trim()
    }.getOrNull()
}
