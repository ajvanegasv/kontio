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

    private val _modelFlow: MutableStateFlow<String> = MutableStateFlow(
        defaults.stringForKey(KEY_MODEL).let {
            if (it == null || it.contains("1.5-flash") || it.contains("2.0-flash") || it.contains("2.5-flash")) {
                defaults.setObject(DEFAULT_MODEL, forKey = KEY_MODEL)
                DEFAULT_MODEL
            } else {
                it
            }
        }
    )
    override val modelFlow: Flow<String> = _modelFlow.asStateFlow()

    override fun getModel(): String {
        val stored = defaults.stringForKey(KEY_MODEL) ?: DEFAULT_MODEL
        if (stored.contains("1.5-flash") || stored.contains("2.0-flash") || stored.contains("2.5-flash")) {
            setModel(DEFAULT_MODEL)
            return DEFAULT_MODEL
        }
        return stored
    }

    override fun setModel(model: String) {
        val clean = model.removePrefix("models/").trim().ifBlank { DEFAULT_MODEL }
        val target = if (clean.contains("1.5-flash") || clean.contains("2.0-flash") || clean.contains("2.5-flash")) DEFAULT_MODEL else clean
        defaults.setObject(target, forKey = KEY_MODEL)
        _modelFlow.value = target
    }

    companion object {
        private const val KEY_API_KEY = "kontio_gemini_api_key"
        private const val KEY_MODEL = "kontio_gemini_model"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
    }
}

private val iosStorageInstance by lazy { IosAiConfigStorage() }

actual fun getAiConfigStorage(): AiConfigStorage = iosStorageInstance
