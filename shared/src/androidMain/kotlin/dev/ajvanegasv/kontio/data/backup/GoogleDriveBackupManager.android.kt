package dev.ajvanegasv.kontio.data.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dev.ajvanegasv.kontio.data.local.getKontioAndroidContext
import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

actual class GoogleDriveBackupManager actual constructor() {
    private val context: Context = getKontioAndroidContext()
    private val prefs = context.getSharedPreferences("kontio_google_drive", Context.MODE_PRIVATE)
    private val _connectedAccount = MutableStateFlow<String?>(prefs.getString(KEY_ACCOUNT_EMAIL, null))
    actual val connectedAccount: Flow<String?> = _connectedAccount.asStateFlow()

    private val localBackupFile: File
        get() = File(context.filesDir, "kontio_drive_appdata_backup.knt")

    private val localMetadataFile: File
        get() = File(context.filesDir, "kontio_drive_backup_metadata.json")

    actual fun setConnectedAccount(email: String) {
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email).apply()
        _connectedAccount.value = email
    }

    actual suspend fun connectAccount(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val savedEmail = prefs.getString(KEY_ACCOUNT_EMAIL, null)
            val signedInEmail = GoogleSignIn.getLastSignedInAccount(context)?.email
            val email = savedEmail ?: signedInEmail
                ?: throw IllegalStateException("Por favor vincula tu cuenta de Google.")
            setConnectedAccount(email)
            email
        }
    }

    actual suspend fun disconnectAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (_: Exception) {}
            prefs.edit().remove(KEY_ACCOUNT_EMAIL).apply()
            _connectedAccount.value = null
        }
    }

    actual suspend fun uploadBackup(
        payloadBytes: ByteArray,
        metadata: BackupMetadata
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val email = _connectedAccount.value
                ?: prefs.getString(KEY_ACCOUNT_EMAIL, null)
                ?: throw IllegalStateException("No hay ninguna cuenta de Google vinculada.")

            // 1. Guardar copia local en caché primero
            localBackupFile.writeBytes(payloadBytes)
            val metadataJson = Json.encodeToString(metadata)
            localMetadataFile.writeText(metadataJson)

            // 2. Subir a Google Drive (appDataFolder)
            val token = getAccessToken(email)

            // Subir payload binario de backup
            val existingBackupId = findFileInAppData(token, BACKUP_FILENAME)
            if (existingBackupId != null) {
                updateFileInAppData(token, existingBackupId, payloadBytes, "application/octet-stream")
            } else {
                createFileInAppData(token, BACKUP_FILENAME, payloadBytes, "application/octet-stream")
            }

            // Subir manifiesto de metadatos JSON
            val metadataBytes = metadataJson.encodeToByteArray()
            val existingMetaId = findFileInAppData(token, METADATA_FILENAME)
            if (existingMetaId != null) {
                updateFileInAppData(token, existingMetaId, metadataBytes, "application/json")
            } else {
                createFileInAppData(token, METADATA_FILENAME, metadataBytes, "application/json")
            }

            metadata
        }
    }

    actual suspend fun downloadLatestBackup(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val email = _connectedAccount.value
                ?: prefs.getString(KEY_ACCOUNT_EMAIL, null)
                ?: throw IllegalStateException("No hay ninguna cuenta de Google vinculada.")

            val token = getAccessToken(email)
            val backupFileId = findFileInAppData(token, BACKUP_FILENAME)

            val bytes = if (backupFileId != null) {
                val downloaded = downloadFileFromAppData(token, backupFileId)
                localBackupFile.writeBytes(downloaded)
                downloaded
            } else if (localBackupFile.exists()) {
                localBackupFile.readBytes()
            } else {
                throw IllegalStateException("No se encontró ninguna copia de seguridad en Google Drive ni en almacenamiento local.")
            }

            // Sincronizar metadatos si están disponibles
            val metaFileId = findFileInAppData(token, METADATA_FILENAME)
            if (metaFileId != null) {
                try {
                    val metaBytes = downloadFileFromAppData(token, metaFileId)
                    localMetadataFile.writeBytes(metaBytes)
                } catch (_: Exception) {}
            }

            bytes
        }
    }

    actual suspend fun getLatestBackupMetadata(): Result<BackupMetadata?> = withContext(Dispatchers.IO) {
        runCatching {
            // Intentar leer de Google Drive si hay cuenta vinculada
            val email = _connectedAccount.value ?: prefs.getString(KEY_ACCOUNT_EMAIL, null)
            if (email != null) {
                try {
                    val token = getAccessToken(email)
                    val metaId = findFileInAppData(token, METADATA_FILENAME)
                    if (metaId != null) {
                        val metaBytes = downloadFileFromAppData(token, metaId)
                        localMetadataFile.writeBytes(metaBytes)
                        return@runCatching Json.decodeFromString<BackupMetadata>(metaBytes.decodeToString())
                    }
                } catch (_: Exception) {
                    // Si falla la red, recurrir a la caché local
                }
            }

            // Recurrir a la caché local
            if (localMetadataFile.exists()) {
                try {
                    return@runCatching Json.decodeFromString<BackupMetadata>(localMetadataFile.readText())
                } catch (_: Exception) {}
            }

            null
        }
    }

    private fun getAccessToken(email: String): String {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: Account(email, "com.google")
        val scope = "oauth2:https://www.googleapis.com/auth/drive.appdata"
        return try {
            GoogleAuthUtil.getToken(context, googleAccount, scope)
        } catch (e: Exception) {
            throw IllegalStateException("Error al autenticar con Google Drive: ${e.message}", e)
        }
    }

    private fun findFileInAppData(token: String, filename: String): String? {
        val encodedQuery = URLEncoder.encode("name = '$filename' and trashed = false", "UTF-8")
        val url = URL("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&q=$encodedQuery&fields=files(id,name,size,modifiedTime)")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 15000
            readTimeout = 15000
        }

        val code = conn.responseCode
        if (code == 401) {
            GoogleAuthUtil.clearToken(context, token)
            throw IllegalStateException("Sesión de Google expirada. Por favor vuelve a vincular la cuenta.")
        }
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("Error de Google Drive ($code): $err")
        }

        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val files = Json.parseToJsonElement(body).jsonObject["files"]?.jsonArray ?: return null
        if (files.isEmpty()) return null
        return files[0].jsonObject["id"]?.jsonPrimitive?.content
    }

    private fun createFileInAppData(token: String, filename: String, content: ByteArray, mimeType: String): String {
        val boundary = "KontioDriveBoundary${System.currentTimeMillis()}"
        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            doOutput = true
            connectTimeout = 20000
            readTimeout = 30000
        }

        val metadataJson = """{"name":"$filename","parents":["appDataFolder"]}"""
        val os = conn.outputStream
        val writer = os.bufferedWriter(Charsets.UTF_8)

        writer.write("--$boundary\r\n")
        writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
        writer.write(metadataJson)
        writer.write("\r\n")
        writer.flush()

        writer.write("--$boundary\r\n")
        writer.write("Content-Type: $mimeType\r\n\r\n")
        writer.flush()

        os.write(content)
        os.flush()

        writer.write("\r\n--$boundary--\r\n")
        writer.flush()

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("Error al subir archivo a Google Drive ($code): $err")
        }

        val body = conn.inputStream.bufferedReader().use { it.readText() }
        return Json.parseToJsonElement(body).jsonObject["id"]?.jsonPrimitive?.content ?: ""
    }

    private fun updateFileInAppData(token: String, fileId: String, content: ByteArray, mimeType: String) {
        val url = URL("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", mimeType)
            doOutput = true
            connectTimeout = 20000
            readTimeout = 30000
            setFixedLengthStreamingMode(content.size)
        }

        conn.outputStream.use { it.write(content) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("Error al actualizar archivo en Google Drive ($code): $err")
        }
    }

    private fun downloadFileFromAppData(token: String, fileId: String): ByteArray {
        val url = URL("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 20000
            readTimeout = 30000
        }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw IllegalStateException("Error al descargar archivo de Google Drive ($code): $err")
        }

        return conn.inputStream.use { it.readBytes() }
    }

    companion object {
        private const val KEY_ACCOUNT_EMAIL = "google_account_email"
        private const val BACKUP_FILENAME = "kontio_drive_backup.knt"
        private const val METADATA_FILENAME = "kontio_drive_backup_metadata.json"
    }
}
