package dev.ajvanegasv.kontio.domain.agent.model

import kotlinx.serialization.Serializable

/**
 * Respuesta estructurada generada por el agente de IA de Kontio.
 *
 * @property text Respuesta conversacional completa formateada en Markdown para presentación en pantalla.
 * @property speechText Versión limpia y concisa de la respuesta, optimizada para sintetizadores de voz (Text-to-Speech)
 *                     y respuestas de la API de Voz de Android (sin markdown ni tablas complejas).
 * @property toolsUsed Lista de nombres de las herramientas que fueron ejecutadas para construir esta respuesta.
 * @property visualPayload Payload estructurado para renderizado de UI Generativo (tarjetas de cuentas, gráficos, transacciones).
 * @property isAiGenerated Indica si la respuesta fue generada o sintetizada con un LLM o con el motor semántico local.
 */
@Serializable
data class AgentResponse(
    val text: String,
    val speechText: String,
    val toolsUsed: List<String> = emptyList(),
    val visualPayload: AgentVisualPayload? = null,
    val isAiGenerated: Boolean = true
)
