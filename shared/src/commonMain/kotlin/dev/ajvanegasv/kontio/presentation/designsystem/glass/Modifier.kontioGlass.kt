package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeEffect

/**
 * Modificador reutilizable para convertir cualquier elemento Composable en una superficie de cristal esmerilado (Glassmorphism).
 *
 * Características:
 * - Se enlaza automáticamente con el [LocalHazeState] activo en la pantalla.
 * - Si no existe un [LocalHazeState] (ej. en Compose Previews o pruebas unitarias), usa un fallback translúcido elegante.
 * - Aplica sombra con elevación y tintes adaptativos antes del clip.
 * - Aplica el borde especular característico de superficies de cristal pulido.
 * - Recorta con [shape] para garantizar esquinas redondeadas en el desenfoque.
 */
fun Modifier.kontioGlass(
    shape: Shape,
    style: @Composable () -> HazeStyle = { GlassTokens.cardStyle() },
    borderBrush: (@Composable () -> Brush)? = { GlassTokens.specularBorderBrush() },
    borderWidth: Dp = GlassTokens.BorderWidth,
    elevation: Dp = 0.dp,
    shadowAmbientColor: Color? = null,
    shadowSpotColor: Color? = null
): Modifier = composed {
    val hazeState = LocalHazeState.current
    val resolvedStyle = style()
    val resolvedBorder = borderBrush?.invoke()
    val ambient = shadowAmbientColor ?: GlassTokens.shadowAmbientColor()
    val spot = shadowSpotColor ?: GlassTokens.shadowSpotColor()

    var modifier = this
    if (elevation > 0.dp) {
        modifier = modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = ambient,
            spotColor = spot
        )
    }
    modifier = modifier.clip(shape)

    if (hazeState != null) {
        modifier = modifier.hazeEffect(
            state = hazeState,
            style = resolvedStyle
        )
    } else {
        // Fallback elegante para Compose Preview o entornos sin contexto Haze
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
            shape = shape
        )
    }

    if (resolvedBorder != null) {
        modifier = modifier.border(
            width = borderWidth,
            brush = resolvedBorder,
            shape = shape
        )
    }

    modifier
}
