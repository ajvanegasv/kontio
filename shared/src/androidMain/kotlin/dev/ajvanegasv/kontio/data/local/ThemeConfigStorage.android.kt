package dev.ajvanegasv.kontio.data.local

import android.content.Context
import dev.ajvanegasv.kontio.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidThemeConfigStorage(context: Context) : ThemeConfigStorage {
    private val prefs = context.getSharedPreferences("kontio_theme_settings", Context.MODE_PRIVATE)
    private val _themeModeFlow = MutableStateFlow(readStoredThemeMode())

    override val themeModeFlow: Flow<ThemeMode> = _themeModeFlow.asStateFlow()

    private fun readStoredThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, null) ?: return ThemeMode.SYSTEM
        return try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    override fun getThemeMode(): ThemeMode {
        return readStoredThemeMode()
    }

    override fun setThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode.name).apply()
        _themeModeFlow.value = themeMode
    }

    companion object {
        private const val KEY_THEME_MODE = "kontio_theme_mode"
    }
}

private var androidThemeStorageInstance: ThemeConfigStorage? = null

actual fun getThemeConfigStorage(): ThemeConfigStorage {
    return androidThemeStorageInstance ?: synchronized(AndroidThemeConfigStorage::class) {
        androidThemeStorageInstance ?: AndroidThemeConfigStorage(getKontioAndroidContext()).also {
            androidThemeStorageInstance = it
        }
    }
}
