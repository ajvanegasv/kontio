package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable
import dev.ajvanegasv.kontio.domain.model.StatementFile

/**
 * Launcher multiplataforma para seleccionar archivos de extracto bancario (PDF o CSV).
 */
@Composable
expect fun rememberStatementFilePicker(
    onFileSelected: (StatementFile) -> Unit
): () -> Unit
