package dev.ajvanegasv.kontio.data.remote.gemini

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class GeminiApiClient(
    private val httpClient: HttpClient = createDefaultHttpClient()
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
        return runCatching {
            require(apiKey.isNotBlank()) { "Se requiere una API Key de Gemini válida" }
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val httpResponse = httpClient.post(url) {
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
    }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.0-flash"

        fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    })
                }
            }
        }
    }
}
