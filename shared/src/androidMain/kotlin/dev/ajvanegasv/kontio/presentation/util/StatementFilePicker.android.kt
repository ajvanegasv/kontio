package dev.ajvanegasv.kontio.presentation.util

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.ajvanegasv.kontio.domain.model.StatementFile

@Composable
actual fun rememberStatementFilePicker(
    onFileSelected: (StatementFile) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            var displayName = "extracto_bancario"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }

            val mimeType = contentResolver.getType(uri) ?: when {
                displayName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                displayName.endsWith(".csv", ignoreCase = true) -> "text/csv"
                else -> "application/octet-stream"
            }

            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && bytes.isNotEmpty()) {
                onFileSelected(
                    StatementFile(
                        name = displayName,
                        mimeType = mimeType,
                        bytes = bytes
                    )
                )
            }
        }
    }

    return {
        launcher.launch(
            arrayOf(
                "application/pdf",
                "text/csv",
                "text/comma-separated-values",
                "text/plain"
            )
        )
    }
}
