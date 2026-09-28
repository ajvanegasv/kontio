package dev.ajvanegasv.kontio.presentation.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal que indica si la aplicación se encuentra actualmente en tema oscuro.
 */
val LocalThemeIsDark = compositionLocalOf { false }

/**
 * Retorna true si la interfaz se encuentra renderizando en tema oscuro.
 */
@Composable
fun isKontioDarkTheme(): Boolean = LocalThemeIsDark.current

/**
 * Tema principal de Kontio que soporta conmutación automática entre modos Claro y Oscuro,
 * inyectando la paleta de colores de MaterialTheme y los colores ambientales de KontioMeshColors.
 */
@Composable
fun KontioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val meshColors = if (darkTheme) DarkMeshColors else LightMeshColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalKontioMeshColors provides meshColors,
        LocalThemeIsDark provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
