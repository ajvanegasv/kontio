package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable

/**
 * Composable multiplataforma para interceptar la navegación hacia atrás del sistema
 * (botón atrás físico o gesto de deslizar hacia atrás en Android).
 *
 * En Android delega en [androidx.activity.compose.BackHandler].
 * En iOS es un no-op seguro o se integra con la interacción UI correspondiente.
 *
 * @param enabled Indica si el interceptor está activo.
 * @param onBack Acción a ejecutar cuando se recibe el evento de ir hacia atrás.
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)
