package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Tokens de diseño centralizados para la temática Glassmorphism de Kontio.
 * Mantiene la coherencia visual entre tarjetas financieras, barras flotantes y modales.
 */
@Immutable
object GlassTokens {
    // Geometría y bordes
    val BorderWidth: Dp = 1.dp
    val ThinBorderWidth: Dp = 0.5.dp
    val CardCornerRadius: Dp = 20.dp
    val DockCornerRadius: Dp = 32.dp
    val ChipCornerRadius: Dp = 12.dp

    // Parámetros de renderizado Haze
    const val NoiseFactor: Float = 0.08f
    val CardBlurRadius: Dp = 22.dp
    val DockBlurRadius: Dp = 18.dp
    val ModalBlurRadius: Dp = 30.dp

    /**
     * Estilo esmerilado para tarjetas de cuentas, saldos y métricas financieras.
     * Garantiza alto contraste en el texto numérico sin perder el efecto de profundidad.
     */
    @Composable
    fun cardStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val surface = MaterialTheme.colorScheme.surface
        val alpha = if (isDark) 0.60f else 0.75f
        return HazeStyle(
            backgroundColor = surface,
            blurRadius = CardBlurRadius,
            tints = listOf(HazeTint(color = surface.copy(alpha = alpha))),
            noiseFactor = NoiseFactor
        )
    }

    /**
     * Estilo más sutil y ligero para barras de navegación o docks flotantes.
     */
    @Composable
    fun dockStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val surface = MaterialTheme.colorScheme.surfaceContainer
        val alpha = if (isDark) 0.55f else 0.70f
        return HazeStyle(
            backgroundColor = surface,
            blurRadius = DockBlurRadius,
            tints = listOf(HazeTint(color = surface.copy(alpha = alpha))),
            noiseFactor = NoiseFactor
        )
    }

    /**
     * Estilo ligero y ultra translúcido para la barra superior (TopAppBar).
     */
    @Composable
    fun topAppBarStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val surface = MaterialTheme.colorScheme.surface
        val alpha = if (isDark) 0.50f else 0.65f
        return HazeStyle(
            backgroundColor = surface,
            blurRadius = 16.dp,
            tints = listOf(HazeTint(color = surface.copy(alpha = alpha))),
            noiseFactor = NoiseFactor
        )
    }

    /**
     * Borde especular que simula la refracción de la luz en los bordes del cristal pulido.
     */
    @Composable
    fun specularBorderBrush(): Brush {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.25f)
                )
            )
        }
    }
}
