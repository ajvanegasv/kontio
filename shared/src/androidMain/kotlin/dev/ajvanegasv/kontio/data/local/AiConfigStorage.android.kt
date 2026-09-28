package dev.ajvanegasv.kontio.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidAiConfigStorage(context: Context) : AiConfigStorage {
    private val prefs = context.getSharedPreferences("kontio_ai_settings", Context.MODE_PRIVATE)
    private val _apiKeyFlow = MutableStateFlow<String?>(prefs.getString(KEY_API_KEY, null))

    override val apiKeyFlow: Flow<String?> = _apiKeyFlow.asStateFlow()

    override fun getApiKey(): String? {
        return prefs.getString(KEY_API_KEY, null)
    }

    override fun setApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        prefs.edit().putString(KEY_API_KEY, trimmed).apply()
        _apiKeyFlow.value = trimmed
    }

    override fun clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = null
    }

    private val _modelFlow: MutableStateFlow<String> = MutableStateFlow(
        prefs.getString(KEY_MODEL, DEFAULT_MODEL).let {
            if (it == null || it.contains("1.5-flash") || it.contains("2.0-flash") || it.contains("2.5-flash")) {
                prefs.edit().putString(KEY_MODEL, DEFAULT_MODEL).apply()
                DEFAULT_MODEL
            } else {
                it
            }
        }
    )
    override val modelFlow: Flow<String> = _modelFlow.asStateFlow()

    override fun getModel(): String {
        val stored = prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        if (stored.contains("1.5-flash") || stored.contains("2.0-flash") || stored.contains("2.5-flash")) {
            setModel(DEFAULT_MODEL)
            return DEFAULT_MODEL
        }
        return stored
    }

    override fun setModel(model: String) {
        val clean = model.removePrefix("models/").trim().ifBlank { DEFAULT_MODEL }
        val target = if (clean.contains("1.5-flash") || clean.contains("2.0-flash") || clean.contains("2.5-flash")) DEFAULT_MODEL else clean
        prefs.edit().putString(KEY_MODEL, target).apply()
        _modelFlow.value = target
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        const val DEFAULT_MODEL = "gemini-3.8-flash"
    }
}

private var androidStorageInstance: AiConfigStorage? = null

actual fun getAiConfigStorage(): AiConfigStorage {
    return androidStorageInstance ?: synchronized(AndroidAiConfigStorage::class) {
        androidStorageInstance ?: AndroidAiConfigStorage(getKontioAndroidContext()).also {
            androidStorageInstance = it
        }
    }
}
