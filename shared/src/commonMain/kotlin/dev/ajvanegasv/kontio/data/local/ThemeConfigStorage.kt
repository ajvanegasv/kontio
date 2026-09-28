package dev.ajvanegasv.kontio.data.local

import dev.ajvanegasv.kontio.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Almacenamiento persistente de la preferencia de tema visual de la aplicación.
 */
interface ThemeConfigStorage {
    val themeModeFlow: Flow<ThemeMode>
    fun getThemeMode(): ThemeMode
    fun setThemeMode(themeMode: ThemeMode)
}

expect fun getThemeConfigStorage(): ThemeConfigStorage
