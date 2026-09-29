package dev.ajvanegasv.kontio.domain.agent.service

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiContent
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionResponse
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiGenerationConfig
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiPart
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiRequest
import dev.ajvanegasv.kontio.domain.agent.model.AgentResponse
import dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry

/**
 * Caso de uso principal que orquesta el Mini Agente de IA de Kontio.
 * Implementa un ciclo completo de agente con Gemini Function Calling (modo online)
 * y un enrutador semántico de respaldo local (modo offline), garantizando siempre
 * el cumplimiento de la restricción de solo lectura y entregando respuestas optimizadas para voz y UI generativa.
 */
class KontioAgentUseCase(
    private val toolRegistry: KontioToolRegistry,
    private val aiConfigStorage: AiConfigStorage,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {

    private val systemInstruction = GeminiContent(
        role = "system",
        parts = listOf(
            GeminiPart(
                text = """
                    Eres Kontio AI, el asistente financiero personal inteligente de la aplicación Kontio.
                    Tu función es responder consultas de los usuarios sobre sus finanzas: saldos, cuentas bancarias,
                    tarjetas de crédito, transacciones, comercios y distribución del gasto por categorías.

                    REGLA ESTRICTA DE SEGURIDAD (SOLO LECTURA):
                    Operas única y exclusivamente en modo de CONSULTA y ANÁLISIS (Solo Lectura).
                    NO tienes permisos ni herramientas para crear, editar, modificar ni eliminar datos.
                    Si el usuario te solicita registrar, eliminar o alterar cualquier dato, explícale amablemente
                    que tu rol es informativo y oriéntalo a hacerlo desde la sección correspondiente de la app.

                    DIRECTRICES DE RESPUESTA:
                    1. Emplea siempre las herramientas provistas para fundamentar tus respuestas en los datos reales del usuario.
                    2. Responde en español de forma empática, clara, profesional y concisa (1 a 3 párrafos como máximo).
                    3. No inventes transacciones ni saldos que no figuren en los resultados de las herramientas.
                """.trimIndent()
            )
        )
    )

    suspend fun executeQuery(userQuery: String): AgentResponse {
        val cleanQuery = userQuery.trim()
        if (cleanQuery.isBlank()) {
            return AgentResponse(
                text = "Por favor escribe una consulta o selecciona una de las sugerencias para consultar tus finanzas.",
                speechText = "Por favor escribe una consulta para comenzar.",
                toolsUsed = emptyList(),
                visualPayload = null,
                isAiGenerated = false
            )
        }

        // 1. Verificación de seguridad de solo lectura inmediata (Semantic Guardrail)
        val routedIntent = SemanticIntentRouter.route(cleanQuery)
        if (routedIntent.isDisallowedMutation && routedIntent.rejectionMessage != null) {
            val rejection = routedIntent.rejectionMessage
            return AgentResponse(
                text = rejection,
                speechText = cleanForSpeech(rejection),
                toolsUsed = emptyList(),
                visualPayload = null,
                isAiGenerated = false
            )
        }

        val apiKey = aiConfigStorage.getApiKey()
        val isAiEnabled = aiConfigStorage.isAiEnabled()
        val model = aiConfigStorage.getModel().ifBlank { GeminiApiClient.DEFAULT_MODEL }

        // 2. Flujo Online: Gemini Function Calling Agentic Loop
        if (isAiEnabled && !apiKey.isNullOrBlank()) {
            try {
                val userContent = GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = cleanQuery))
                )

                val initialRequest = GeminiRequest(
                    systemInstruction = systemInstruction,
                    contents = listOf(userContent),
                    tools = listOf(toolRegistry.geminiTool),
                    generationConfig = GeminiGenerationConfig(temperature = 0.1f)
                )

                val candidateResult = geminiApiClient.generateCandidate(
                    apiKey = apiKey,
                    request = initialRequest,
                    model = model
                )

                if (candidateResult.isSuccess) {
                    val candidate = candidateResult.getOrThrow()
                    val candidateContent = candidate.content
                    val functionCall = candidateContent?.parts?.firstOrNull()?.functionCall

                    if (functionCall != null) {
                        // El LLM decidió invocar una herramienta
                        val toolResult = toolRegistry.execute(
                            name = functionCall.name,
                            args = functionCall.args.orEmpty()
                        )

                        // Enviar el resultado de la función de regreso a Gemini para la síntesis en lenguaje natural
                        val functionResponseContent = GeminiContent(
                            role = "function",
                            parts = listOf(
                                GeminiPart(
                                    functionResponse = GeminiFunctionResponse(
                                        name = functionCall.name,
                                        response = toolResult.dataSummary
                                    )
                                )
                            )
                        )

                        val synthesisRequest = GeminiRequest(
                            systemInstruction = systemInstruction,
                            contents = listOf(
                                userContent,
                                candidateContent,
                                functionResponseContent
                            ),
                            generationConfig = GeminiGenerationConfig(temperature = 0.3f)
                        )

                        val synthesisResult = geminiApiClient.generateContent(
                            apiKey = apiKey,
                            request = synthesisRequest,
                            model = model
                        )

                        val finalText = synthesisResult.getOrNull()?.trim() ?: toolResult.naturalLanguageSummary

                        return AgentResponse(
                            text = finalText,
                            speechText = cleanForSpeech(finalText),
                            toolsUsed = listOf(toolResult.toolName),
                            visualPayload = toolResult.visualPayload,
                            isAiGenerated = true
                        )
                    } else {
                        // El LLM respondió directamente sin herramientas (ej. saludo conversacional)
                        val directText = candidateContent?.parts?.firstOrNull()?.text
                        if (!directText.isNullOrBlank()) {
                            return AgentResponse(
                                text = directText,
                                speechText = cleanForSpeech(directText),
                                toolsUsed = emptyList(),
                                visualPayload = null,
                                isAiGenerated = true
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Si la red o el servicio de Gemini fallan, pasamos de inmediato al motor local
            }
        }

        // 3. Flujo Offline / Fallback Local Robusto
        val toolResult = toolRegistry.execute(routedIntent.toolName, routedIntent.arguments)
        return AgentResponse(
            text = toolResult.naturalLanguageSummary,
            speechText = toolResult.speechSummary,
            toolsUsed = listOf(toolResult.toolName),
            visualPayload = toolResult.visualPayload,
            isAiGenerated = false
        )
    }

    /**
     * Limpia etiquetas de markdown, viñetas y asteriscos para producir un texto fluido y natural
     * para sintetizadores de voz (Text-to-Speech) o la API de voz de Android.
     */
    private fun cleanForSpeech(markdownText: String): String {
        return markdownText
            .replace(Regex("""[*#_`~]"""), "")
            .replace("•", "")
            .replace(Regex("""\n+"""), ". ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
