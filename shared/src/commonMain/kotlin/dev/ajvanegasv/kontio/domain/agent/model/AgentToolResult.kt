package dev.ajvanegasv.kontio.domain.agent.model

import kotlinx.serialization.json.JsonObject

/**
 * Representa el resultado de la invocación de una herramienta de solo lectura por parte del agente.
 *
 * @property toolName Nombre único de la herramienta ejecutada.
 * @property success Indica si la consulta a la base de datos o cómputo se completó exitosamente.
 * @property dataSummary Objeto JSON estructurado que se devuelve al modelo Gemini en el `GeminiFunctionResponse`.
 * @property naturalLanguageSummary Resumen directo en lenguaje natural del resultado (usado para modo offline o fallback).
 * @property speechSummary Resumen optimizado para voz sin marcas tipográficas.
 * @property visualPayload Payload opcional para que la UI dibuje componentes interactivos basados en el resultado.
 */
data class AgentToolResult(
    val toolName: String,
    val success: Boolean,
    val dataSummary: JsonObject,
    val naturalLanguageSummary: String,
    val speechSummary: String = naturalLanguageSummary,
    val visualPayload: AgentVisualPayload? = null
)
