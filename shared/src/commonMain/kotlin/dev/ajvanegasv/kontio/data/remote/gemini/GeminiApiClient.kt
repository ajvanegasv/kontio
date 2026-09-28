package dev.ajvanegasv.kontio.data.remote.gemini

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class GeminiApiClient(
    private val httpClient: HttpClient = createGeminiHttpClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    suspend fun generateContent(
        apiKey: String,
        request: GeminiRequest,
        model: String = DEFAULT_MODEL
    ): Result<String> {
        val initialCandidates = buildList {
            val clean = model.removePrefix("models/").trim()
            if (clean.isNotBlank()) add(clean)
            STATIC_FALLBACK_MODELS.forEach { fallback ->
                val cleanFallback = fallback.removePrefix("models/").trim()
                if (!contains(cleanFallback)) add(cleanFallback)
            }
        }

        var lastException: Throwable = IllegalStateException("No hay modelos disponibles")

        for (candidateModel in initialCandidates) {
            val result = executeSingleGenerateContent(apiKey, request, candidateModel)
            if (result.isSuccess) {
                return result
            }
            val exception = result.exceptionOrNull() ?: continue
            val msg = exception.message.orEmpty()
            if (isModelAvailabilityError(msg)) {
                lastException = exception
                continue
            } else {
                return Result.failure(exception)
            }
        }

        // Si los modelos estáticos fallaron por disponibilidad (404/not found/deprecated),
        // consultamos dinámicamente los modelos autorizados para esta clave mediante ModelService.ListModels
        val dynamicModelsResult = fetchAvailableModels(apiKey)
        if (dynamicModelsResult.isSuccess) {
            val dynamicModels = dynamicModelsResult.getOrThrow()
            val remainingCandidates = dynamicModels.filter { it !in initialCandidates }

            for (candidateModel in remainingCandidates) {
                val result = executeSingleGenerateContent(apiKey, request, candidateModel)
                if (result.isSuccess) {
                    return result
                }
                val exception = result.exceptionOrNull() ?: continue
                val msg = exception.message.orEmpty()
                if (isModelAvailabilityError(msg)) {
                    lastException = exception
                    continue
                } else {
                    return Result.failure(exception)
                }
            }
        }

        return Result.failure(lastException)
    }

    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = runCatching {
        require(apiKey.isNotBlank()) { "Se requiere una API Key de Gemini válida" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"

        val httpResponse = httpClient.get(url) {
            timeout {
                requestTimeoutMillis = 30_000L
                socketTimeoutMillis = 30_000L
                connectTimeoutMillis = 15_000L
            }
        }
        val responseText = httpResponse.bodyAsText()

        if (!httpResponse.status.isSuccess()) {
            val errorMsg = try {
                val errorResp = json.decodeFromString<GeminiResponse>(responseText)
                errorResp.error?.message ?: "Error HTTP ${httpResponse.status.value}: $responseText"
            } catch (e: Exception) {
                "Error HTTP ${httpResponse.status.value}: $responseText"
            }
            throw IllegalStateException(errorMsg)
        }

        val parsed = json.decodeFromString<GeminiModelListResponse>(responseText)
        val models = parsed.models
            ?.filter { it.supportedGenerationMethods.contains("generateContent") }
            ?.map { it.name.removePrefix("models/").trim() }
            .orEmpty()

        if (models.isEmpty()) {
            throw IllegalStateException("No se encontraron modelos compatibles con generateContent para esta API Key")
        }

        // Priorizamos modelos Flash modernos, especialmente gemini-3.8-flash
        models.sortedWith(
            compareByDescending<String> { it.contains("flash", ignoreCase = true) }
                .thenByDescending { it.contains("3.8") }
                .thenByDescending { it.contains("3.5") || it.contains("3.") }
                .thenByDescending { it.contains("2.5") }
        )
    }

    private suspend fun executeSingleGenerateContent(
        apiKey: String,
        request: GeminiRequest,
        model: String
    ): Result<String> = runCatching {
        require(apiKey.isNotBlank()) { "Se requiere una API Key de Gemini válida" }
        val cleanModel = model.removePrefix("models/").trim()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"

        val httpResponse = httpClient.post(url) {
            timeout {
                requestTimeoutMillis = 180_000L
                socketTimeoutMillis = 180_000L
                connectTimeoutMillis = 60_000L
            }
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        val responseText = httpResponse.bodyAsText()

        if (!httpResponse.status.isSuccess()) {
            val errorMsg = try {
                val errorResp = json.decodeFromString<GeminiResponse>(responseText)
                errorResp.error?.message ?: "Error HTTP ${httpResponse.status.value}: $responseText"
            } catch (e: Exception) {
                "Error HTTP ${httpResponse.status.value}: $responseText"
            }
            throw IllegalStateException(errorMsg)
        }

        val parsedResponse = json.decodeFromString<GeminiResponse>(responseText)
        val candidate = parsedResponse.candidates?.firstOrNull()
            ?: throw IllegalStateException("Gemini no devolvió ninguna respuesta")

        val textPart = candidate.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("La respuesta de Gemini no contiene texto")

        textPart
    }

    private fun isModelAvailabilityError(msg: String): Boolean {
        return msg.contains("not found", ignoreCase = true) ||
                msg.contains("no longer available", ignoreCase = true) ||
                msg.contains("not supported", ignoreCase = true) ||
                msg.contains("deprecated", ignoreCase = true) ||
                msg.contains("ListModels", ignoreCase = true) ||
                msg.contains("404", ignoreCase = true)
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-3.8-flash"
        val STATIC_FALLBACK_MODELS = listOf(
            "gemini-3.8-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash"
        )
        val FALLBACK_MODELS = STATIC_FALLBACK_MODELS
    }
}
