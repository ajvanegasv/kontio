package dev.ajvanegasv.kontio.domain.repository

import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun getConnectedGoogleAccount(): Flow<String?>
    fun getLatestLocalBackupMetadata(): Flow<BackupMetadata?>
    suspend fun connectGoogleAccount(): Result<String>
    suspend fun disconnectGoogleAccount(): Result<Unit>
    suspend fun createLocalBackup(): Result<ByteArray>
    suspend fun restoreFromLocalBackup(backupBytes: ByteArray): Result<Unit>
    suspend fun backupToGoogleDrive(): Result<BackupMetadata>
    suspend fun restoreFromGoogleDrive(): Result<Unit>
    suspend fun getDriveBackupMetadata(): Result<BackupMetadata?>
}
