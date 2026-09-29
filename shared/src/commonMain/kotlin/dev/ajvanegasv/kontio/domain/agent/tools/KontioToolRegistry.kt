package dev.ajvanegasv.kontio.domain.agent.tools

import dev.ajvanegasv.kontio.data.remote.gemini.GeminiTool
import dev.ajvanegasv.kontio.domain.agent.model.AgentToolResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Registro central de herramientas de solo lectura para el Agente de IA de Kontio.
 * Mantiene el catálogo unificado de tools y despacha de manera segura las llamadas
 * a funciones generadas por Gemini Function Calling o el enrutador local.
 */
class KontioToolRegistry(
    private val tools: List<KontioAgentTool>
) {
    /**
     * Objeto GeminiTool que expone todas las declaraciones de funciones al modelo LLM.
     */
    val geminiTool: GeminiTool = GeminiTool(
        functionDeclarations = tools.map { it.declaration }
    )

    /**
     * Lista de nombres de todas las herramientas registradas.
     */
    val registeredToolNames: List<String> = tools.map { it.name }

    /**
     * Ejecuta una herramienta por su nombre de forma segura.
     * Si la herramienta no existe, retorna un resultado con error amigable sin interrumpir la app.
     */
    suspend fun execute(name: String, args: Map<String, JsonElement>): AgentToolResult {
        val tool = tools.firstOrNull { it.name == name }
        if (tool == null) {
            val errorMsg = "La herramienta solicitada '$name' no está permitida o no existe. Kontio AI solo admite consultas financieras de solo lectura."
            return AgentToolResult(
                toolName = name,
                success = false,
                dataSummary = buildJsonObject {
                    put("error", "TOOL_NOT_FOUND")
                    put("message", errorMsg)
                },
                naturalLanguageSummary = errorMsg,
                speechSummary = "Lo siento, esa acción no está disponible en las consultas de Kontio."
            )
        }

        return try {
            tool.execute(args)
        } catch (e: Exception) {
            val failureMsg = "Ocurrió un error al consultar los datos con '$name': ${e.message}"
            AgentToolResult(
                toolName = name,
                success = false,
                dataSummary = buildJsonObject {
                    put("error", "EXECUTION_ERROR")
                    put("message", failureMsg)
                },
                naturalLanguageSummary = failureMsg,
                speechSummary = "Ocurrió un inconveniente al consultar tu información financiera."
            )
        }
    }

    fun getTool(name: String): KontioAgentTool? = tools.firstOrNull { it.name == name }
}
