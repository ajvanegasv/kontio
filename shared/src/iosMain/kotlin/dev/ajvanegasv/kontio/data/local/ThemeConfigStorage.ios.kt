package dev.ajvanegasv.kontio.data.local

import dev.ajvanegasv.kontio.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

class IosThemeConfigStorage : ThemeConfigStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val _themeModeFlow = MutableStateFlow(readStoredThemeMode())

    override val themeModeFlow: Flow<ThemeMode> = _themeModeFlow.asStateFlow()

    private fun readStoredThemeMode(): ThemeMode {
        val raw = defaults.stringForKey(KEY_THEME_MODE) ?: return ThemeMode.SYSTEM
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
        defaults.setObject(themeMode.name, forKey = KEY_THEME_MODE)
        _themeModeFlow.value = themeMode
    }

    companion object {
        private const val KEY_THEME_MODE = "kontio_theme_mode"
    }
}

private val iosThemeStorageInstance by lazy { IosThemeConfigStorage() }

actual fun getThemeConfigStorage(): ThemeConfigStorage = iosThemeStorageInstance
