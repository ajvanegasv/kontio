package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable

/**
 * Launcher multiplataforma para autenticación con Google Drive.
 */
@Composable
expect fun rememberGoogleDriveAuthLauncher(
    onAccountConnected: (email: String) -> Unit,
    onError: (String) -> Unit
): () -> Unit
