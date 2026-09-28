package dev.ajvanegasv.kontio.data.remote.gemini

import io.ktor.client.HttpClient

expect fun createGeminiHttpClient(): HttpClient
