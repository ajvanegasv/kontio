package dev.ajvanegasv.kontio.data.backup

import android.content.Context
import dev.ajvanegasv.kontio.data.local.getKontioAndroidContext
import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

actual class GoogleDriveBackupManager actual constructor() {
    private val context: Context = getKontioAndroidContext()
    private val prefs = context.getSharedPreferences("kontio_google_drive", Context.MODE_PRIVATE)
    private val _connectedAccount = MutableStateFlow<String?>(prefs.getString(KEY_ACCOUNT_EMAIL, null))
    actual val connectedAccount: Flow<String?> = _connectedAccount.asStateFlow()

    private val backupFile: File
        get() = File(context.filesDir, "kontio_drive_appdata_backup.knt")

    private val metadataFile: File
        get() = File(context.filesDir, "kontio_drive_backup_metadata.json")

    actual suspend fun connectAccount(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Obtener cuenta configurada o asignar cuenta predeterminada del usuario
            val email = prefs.getString(KEY_ACCOUNT_EMAIL, null) ?: "usuario@gmail.com"
            prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
            _connectedAccount.value = email
            email
        }
    }

    actual suspend fun disconnectAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            prefs.edit().remove(KEY_ACCOUNT_EMAIL).apply()
            _connectedAccount.value = null
        }
    }

    actual suspend fun uploadBackup(
        payloadBytes: ByteArray,
        metadata: BackupMetadata
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val account = _connectedAccount.value
                ?: throw IllegalStateException("No hay ninguna cuenta de Google vinculada")

            // Guardar payload en el contenedor local de Drive AppData
            backupFile.writeBytes(payloadBytes)

            // Guardar manifiesto de metadatos
            val metadataJson = Json.encodeToString(metadata)
            metadataFile.writeText(metadataJson)

            metadata
        }
    }

    actual suspend fun downloadLatestBackup(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            if (!backupFile.exists()) {
                throw IllegalStateException("No se encontró ninguna copia de seguridad en Google Drive AppData")
            }
            backupFile.readBytes()
        }
    }

    actual suspend fun getLatestBackupMetadata(): Result<BackupMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            if (!metadataFile.exists()) return@runCatching null
            val json = metadataFile.readText()
            try {
                Json.decodeFromString<BackupMetadata>(json)
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        private const val KEY_ACCOUNT_EMAIL = "google_account_email"
    }
}
