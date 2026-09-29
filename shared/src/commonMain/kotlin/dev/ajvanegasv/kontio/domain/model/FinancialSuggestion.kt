package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

/**
 * Representa una sugerencia de consulta financiera generada por IA o el motor de reglas local,
 * basada en las herramientas de consulta y los datos reales del usuario.
 *
 * @param id Identificador único de la sugerencia.
 * @param label Texto corto y visual con emoji para el chip (ej. "💳 Cupo en Nu", "🍔 Gastos en Restaurantes").
 * @param query Pregunta en lenguaje natural para ser ejecutada por el agente o el buscador (ej. "¿Cuál es el saldo y cupo de mi tarjeta Nu?").
 * @param toolName Nombre de la herramienta del agente con la que se relaciona la consulta (opcional).
 */
@Serializable
data class FinancialSuggestion(
    val id: String,
    val label: String,
    val query: String,
    val toolName: String? = null
)
