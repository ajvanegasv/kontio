package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle

/**
 * Tarjeta financiera esmerilada reutilizable con temática Glassmorphism.
 *
 * Utiliza [kontioGlass] internamente para reflejar el contenido de fondo con desenfoque
 * en tiempo real y bordes especulares pulidos.
 */
@Composable
fun KontioGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTokens.CardCornerRadius),
    style: @Composable () -> HazeStyle = { GlassTokens.cardStyle() },
    borderBrush: (@Composable () -> Brush)? = { GlassTokens.specularBorderBrush() },
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .kontioGlass(
                shape = shape,
                style = style,
                borderBrush = borderBrush
            )
            .then(clickModifier)
            .padding(contentPadding),
        content = content
    )
}
