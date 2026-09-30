package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // No-op en iOS ya que los eventos globales de retroceso son propios del sistema Android.
}
