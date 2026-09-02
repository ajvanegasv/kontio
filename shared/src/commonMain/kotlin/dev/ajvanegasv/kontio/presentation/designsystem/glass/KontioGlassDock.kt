package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle

/**
 * Dock de navegación inferior flotante con forma de píldora y estética Glassmorphism.
 */
@Composable
fun KontioGlassDock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassTokens.DockCornerRadius),
    style: @Composable () -> HazeStyle = { GlassTokens.dockStyle() },
    borderBrush: (@Composable () -> Brush)? = { GlassTokens.specularBorderBrush() },
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .kontioGlass(
                shape = shape,
                style = style,
                borderBrush = borderBrush
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
