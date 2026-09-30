package dev.ajvanegasv.kontio.data.backup

import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class GoogleDriveBackupManager actual constructor() {
    private val _connectedAccount = MutableStateFlow<String?>(null)
    actual val connectedAccount: Flow<String?> = _connectedAccount.asStateFlow()

    actual fun setConnectedAccount(email: String) {
        _connectedAccount.value = email
    }

    private var latestPayload: ByteArray? = null
    private var latestMetadata: BackupMetadata? = null

    actual suspend fun connectAccount(): Result<String> {
        val email = "apple_user@gmail.com"
        _connectedAccount.value = email
        return Result.success(email)
    }

    actual suspend fun disconnectAccount(): Result<Unit> {
        _connectedAccount.value = null
        return Result.success(Unit)
    }

    actual suspend fun uploadBackup(
        payloadBytes: ByteArray,
        metadata: BackupMetadata
    ): Result<BackupMetadata> {
        latestPayload = payloadBytes
        latestMetadata = metadata
        return Result.success(metadata)
    }

    actual suspend fun downloadLatestBackup(): Result<ByteArray> {
        val payload = latestPayload ?: return Result.failure(IllegalStateException("No backup found on iOS"))
        return Result.success(payload)
    }

    actual suspend fun getLatestBackupMetadata(): Result<BackupMetadata?> {
        return Result.success(latestMetadata)
    }
}
