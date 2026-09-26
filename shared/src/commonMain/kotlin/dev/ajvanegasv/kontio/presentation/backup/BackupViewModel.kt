package dev.ajvanegasv.kontio.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import dev.ajvanegasv.kontio.domain.model.BackupState
import dev.ajvanegasv.kontio.domain.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupUiState(
    val connectedAccount: String? = null,
    val latestBackup: BackupMetadata? = null,
    val state: BackupState = BackupState.Idle,
    val statusMessage: String? = null
)

class BackupViewModel(
    private val backupRepository: BackupRepository = AppContainer.backupRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BackupUiState> = combine(
        backupRepository.getConnectedGoogleAccount(),
        backupRepository.getLatestLocalBackupMetadata(),
        _state,
        _statusMessage
    ) { account, latestBackup, state, message ->
        BackupUiState(
            connectedAccount = account,
            latestBackup = latestBackup,
            state = state,
            statusMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BackupUiState()
    )

    init {
        // Cargar metadatos si ya existen en disco
        viewModelScope.launch {
            backupRepository.getDriveBackupMetadata().onSuccess { meta ->
                // metadata cached
            }
        }
    }

    fun connectGoogleAccount() {
        viewModelScope.launch {
            _state.value = BackupState.Connecting()
            backupRepository.connectGoogleAccount()
                .onSuccess { email ->
                    _state.value = BackupState.Idle
                    _statusMessage.value = "Conectado a Google como $email"
                }
                .onFailure { error ->
                    _state.value = BackupState.Error(error.message ?: "No se pudo conectar")
                }
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            backupRepository.disconnectGoogleAccount()
            _statusMessage.value = "Cuenta de Google desconectada"
        }
    }

    fun performBackup() {
        viewModelScope.launch {
            _state.value = BackupState.InProgress("Generando copia de seguridad...")
            backupRepository.backupToGoogleDrive()
                .onSuccess { meta ->
                    _state.value = BackupState.Success(meta, "Copia guardada en Google Drive")
                    _statusMessage.value = "¡Copia de seguridad guardada exitosamente!"
                }
                .onFailure { error ->
                    _state.value = BackupState.Error(error.message ?: "Error al guardar la copia")
                }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            _state.value = BackupState.InProgress("Restaurando datos desde Google Drive...")
            backupRepository.restoreFromGoogleDrive()
                .onSuccess {
                    _state.value = BackupState.Idle
                    _statusMessage.value = "¡Datos restaurados con éxito!"
                }
                .onFailure { error ->
                    _state.value = BackupState.Error(error.message ?: "Error al restaurar la copia")
                }
        }
    }
}
