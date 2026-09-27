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

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
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
