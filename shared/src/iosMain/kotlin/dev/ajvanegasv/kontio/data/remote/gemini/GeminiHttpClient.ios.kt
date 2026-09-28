package dev.ajvanegasv.kontio.data.remote.gemini

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createGeminiHttpClient(): HttpClient = HttpClient(Darwin) {
    engine {
        configureRequest {
            setTimeoutInterval(180.0)
        }
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 180_000L
        connectTimeoutMillis = 60_000L
        socketTimeoutMillis = 180_000L
    }
}
