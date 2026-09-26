package dev.ajvanegasv.kontio.data.backup

import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlinx.coroutines.flow.Flow

expect class GoogleDriveBackupManager() {
    val connectedAccount: Flow<String?>
    suspend fun connectAccount(): Result<String>
    suspend fun disconnectAccount(): Result<Unit>
    suspend fun uploadBackup(payloadBytes: ByteArray, metadata: BackupMetadata): Result<BackupMetadata>
    suspend fun downloadLatestBackup(): Result<ByteArray>
    suspend fun getLatestBackupMetadata(): Result<BackupMetadata?>
}
