package dev.ajvanegasv.kontio.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosAiConfigStorage : AiConfigStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _apiKeyFlow = MutableStateFlow<String?>(defaults.stringForKey(KEY_API_KEY))

    override val apiKeyFlow: Flow<String?> = _apiKeyFlow.asStateFlow()

    override fun getApiKey(): String? {
        return defaults.stringForKey(KEY_API_KEY)
    }

    override fun setApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        defaults.setObject(trimmed, forKey = KEY_API_KEY)
        _apiKeyFlow.value = trimmed
    }

    override fun clearApiKey() {
        defaults.removeObjectForKey(KEY_API_KEY)
        _apiKeyFlow.value = null
    }

    companion object {
        private const val KEY_API_KEY = "kontio_gemini_api_key"
    }
}

private val iosStorageInstance by lazy { IosAiConfigStorage() }

actual fun getAiConfigStorage(): AiConfigStorage = iosStorageInstance
