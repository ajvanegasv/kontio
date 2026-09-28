package dev.ajvanegasv.kontio.data.local

import kotlinx.coroutines.flow.Flow

interface AiConfigStorage {
    val apiKeyFlow: Flow<String?>
    fun getApiKey(): String?
    fun setApiKey(apiKey: String)
    fun clearApiKey()

    val modelFlow: Flow<String>
    fun getModel(): String
    fun setModel(model: String)
}

expect fun getAiConfigStorage(): AiConfigStorage
