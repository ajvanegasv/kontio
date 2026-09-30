package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGoogleDriveAuthLauncher(
    onAccountConnected: (email: String) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    return {
        onAccountConnected("apple_user@gmail.com")
    }
}
