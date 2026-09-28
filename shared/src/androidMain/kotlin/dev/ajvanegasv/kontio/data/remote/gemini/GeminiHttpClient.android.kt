package dev.ajvanegasv.kontio.data.remote.gemini

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

actual fun createGeminiHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(60, TimeUnit.SECONDS)
            readTimeout(180, TimeUnit.SECONDS)
            writeTimeout(180, TimeUnit.SECONDS)
            callTimeout(180, TimeUnit.SECONDS)
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
