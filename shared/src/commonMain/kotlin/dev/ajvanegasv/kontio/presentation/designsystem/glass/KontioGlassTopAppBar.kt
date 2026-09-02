package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeStyle

/**
 * Barra superior de navegación esmerilada con temática Glassmorphism.
 *
 * Flota sobre el contenido en desplazamiento, desenfocando los elementos financieros
 * que pasan por debajo y respetando los insets de la barra de estado del sistema operativo.
 */
@Composable
fun KontioGlassTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    style: @Composable () -> HazeStyle = { GlassTokens.topAppBarStyle() },
    borderBrush: (@Composable () -> Brush)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .kontioGlass(
                shape = RectangleShape,
                style = style,
                borderBrush = borderBrush
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (navigationIcon != null) {
                navigationIcon()
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = if (navigationIcon != null) 12.dp else 0.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                title()
            }
            actions()
        }
    }
}
