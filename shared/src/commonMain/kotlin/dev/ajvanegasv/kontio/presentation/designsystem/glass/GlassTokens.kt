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
    val ModalCornerRadius: Dp = 28.dp

    // Elevaciones
    val CardElevation: Dp = 6.dp
    val HeroCardElevation: Dp = 10.dp
    val DockElevation: Dp = 12.dp
    val TopAppBarElevation: Dp = 4.dp
    val ChipElevation: Dp = 2.dp
    val ModalElevation: Dp = 16.dp

    // Parámetros de renderizado Haze
    const val NoiseFactor: Float = 0.08f
    val CardBlurRadius: Dp = 22.dp
    val DockBlurRadius: Dp = 18.dp
    val ModalBlurRadius: Dp = 30.dp

    /**
     * Helpers de color para sombras adaptativas con matiz slate-900 en Light Mode.
     */
    @Composable
    fun shadowAmbientColor(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Color.Black.copy(alpha = 0.40f)
        } else {
            Color(0xFF0F172A).copy(alpha = 0.06f)
        }
    }

    @Composable
    fun shadowSpotColor(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Color.Black.copy(alpha = 0.60f)
        } else {
            Color(0xFF0F172A).copy(alpha = 0.12f)
        }
    }

    /**
     * Estilo esmerilado para tarjetas de cuentas, saldos y métricas financieras.
     * Garantiza alto contraste en el texto numérico sin perder el efecto de profundidad.
     */
    @Composable
    fun cardStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val surface = MaterialTheme.colorScheme.surface
        val alpha = if (isDark) 0.60f else 0.55f
        return HazeStyle(
            backgroundColor = Color.Transparent,
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
        val alpha = if (isDark) 0.55f else 0.60f
        return HazeStyle(
            backgroundColor = Color.Transparent,
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
        val alpha = if (isDark) 0.50f else 0.55f
        return HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 16.dp,
            tints = listOf(HazeTint(color = surface.copy(alpha = alpha))),
            noiseFactor = NoiseFactor
        )
    }

    /**
     * Estilo esmerilado profundo para modales y bottom sheets desplegables.
     * Mayor radio de desenfoque y opacidad superficial reforzada (88%-92%) para
     * difuminar y disolver completamente los textos y cifras de la pantalla subyacente,
     * garantizando alto contraste y evitando confusión visual al interactuar con el formulario.
     */
    @Composable
    fun modalStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val surface = MaterialTheme.colorScheme.surfaceContainerHigh
        val alpha = if (isDark) 0.88f else 0.92f
        return HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = ModalBlurRadius,
            tints = listOf(HazeTint(color = surface.copy(alpha = alpha))),
            noiseFactor = NoiseFactor
        )
    }

    /**
     * Estilo esmerilado para el telón de fondo (scrim) detrás de los modales desplegables.
     * Aplica desenfoque Gaussiano en tiempo real sobre toda la vista de fondo y la atenúa
     * suavemente para que los textos no distraigan ni compitan con el modal.
     */
    @Composable
    fun scrimBlurStyle(): HazeStyle {
        val isDark = isSystemInDarkTheme()
        val scrimAlpha = if (isDark) 0.60f else 0.40f
        return HazeStyle(
            backgroundColor = Color.Transparent,
            blurRadius = 24.dp,
            tints = listOf(
                HazeTint(color = Color.Black.copy(alpha = scrimAlpha))
            ),
            noiseFactor = 0.04f
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
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFE2E8F0).copy(alpha = 0.60f),
                    Color(0xFF94A3B8).copy(alpha = 0.35f)
                )
            )
        }
    }
}
