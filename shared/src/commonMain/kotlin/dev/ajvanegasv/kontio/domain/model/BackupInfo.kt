package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupMetadata(
    val backupId: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val accountCount: Int,
    val transactionCount: Int,
    val categoryCount: Int,
    val schemaVersion: Int = 1,
    val appVersion: String = "1.0",
    val deviceName: String = "Mobile Device"
)

sealed interface BackupState {
    data object Idle : BackupState
    data class Connecting(val message: String = "Conectando con Google...") : BackupState
    data class InProgress(val stage: String, val progress: Float? = null) : BackupState
    data class Success(val info: BackupMetadata, val message: String) : BackupState
    data class Error(val error: String) : BackupState
}
