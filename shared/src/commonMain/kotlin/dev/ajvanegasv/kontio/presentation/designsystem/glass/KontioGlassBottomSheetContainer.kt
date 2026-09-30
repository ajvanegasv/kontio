package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.ajvanegasv.kontio.presentation.util.BackHandler
import dev.chrisbanes.haze.hazeEffect

/**
 * Contenedor modal desplegable (Bottom Sheet) con estética Glassmorphism y animación fluida.
 *
 * Características:
 * - Telón de fondo (Backdrop scrim) animado con fade y desenfoque Haze en tiempo real,
 *   evitando que los textos de fondo se visualicen y confundan al usuario.
 * - Despliegue y repliegue suave con animación vertical slide + fade.
 * - Interacción táctil segura: el tap en el telón de fondo dispara [onDismissRequest],
 *   mientras que los toques dentro de la tarjeta modal quedan aislados.
 * - Integración con el modo gestos / botón atrás: cuando [visible] es verdadero,
 *   intercepta el gesto atrás del sistema para cerrar la modal en lugar de minimizar la app.
 */
@Composable
fun KontioGlassBottomSheetContainer(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val isDark = isKontioDarkTheme()
    val scrimStyle = GlassTokens.scrimBlurStyle()

    BackHandler(enabled = visible) {
        onDismissRequest()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = scrimStyle
                        )
                    } else {
                        Modifier.background(Color.Black.copy(alpha = if (isDark) 0.65f else 0.45f))
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismissRequest() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
                        ) + fadeOut(animationSpec = tween(durationMillis = 150))
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* Consumir eventos para evitar cerrar al pulsar dentro */ }
            ) {
                content()
            }
        }
    }
}
