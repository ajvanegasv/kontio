package dev.ajvanegasv.kontio.domain.agent.tools

import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionDeclaration
import dev.ajvanegasv.kontio.domain.agent.model.AgentToolResult
import kotlinx.serialization.json.JsonElement

/**
 * Interfaz base para cualquier herramienta ejecutable por el agente de Kontio.
 * Todas las herramientas implementadoras DEBEN ser estrictamente de SOLO LECTURA.
 */
interface KontioAgentTool {
    /**
     * Nombre único de la función (ej. "get_accounts_summary").
     */
    val name: String

    /**
     * Descripción comprensible para el LLM sobre el propósito de la herramienta y cuándo usarla.
     */
    val description: String

    /**
     * Declaración formal de la función y sus parámetros para Gemini Function Calling.
     */
    val declaration: GeminiFunctionDeclaration

    /**
     * Ejecuta la herramienta de forma asíncrona recibiendo los argumentos provistos por el LLM o el enrutador local.
     */
    suspend fun execute(args: Map<String, JsonElement>): AgentToolResult
}
