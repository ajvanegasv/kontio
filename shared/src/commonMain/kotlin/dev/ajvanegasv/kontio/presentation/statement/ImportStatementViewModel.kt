package dev.ajvanegasv.kontio.presentation.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.ParsedStatementResult
import dev.ajvanegasv.kontio.domain.model.StatementFile
import dev.ajvanegasv.kontio.domain.usecase.AnalyzeBankStatementUseCase
import dev.ajvanegasv.kontio.domain.usecase.BatchImportTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ImportStage {
    FILE_SELECTION,
    ANALYZING,
    REVIEWING,
    IMPORTING,
    SUCCESS,
    ERROR
}

data class ImportStatementUiState(
    val stage: ImportStage = ImportStage.FILE_SELECTION,
    val selectedFile: StatementFile? = null,
    val apiKey: String? = null,
    val isApiKeyConfigured: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val parsedResult: ParsedStatementResult? = null,
    val analysisMessage: String = "Preparando lectura...",
    val importedCount: Int = 0,
    val errorMessage: String? = null
)

class ImportStatementViewModel(
    private val analyzeBankStatementUseCase: AnalyzeBankStatementUseCase = AppContainer.analyzeBankStatementUseCase,
    private val batchImportTransactionsUseCase: BatchImportTransactionsUseCase = AppContainer.batchImportTransactionsUseCase,
    private val aiConfigStorage: AiConfigStorage = AppContainer.aiConfigStorage
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportStatementUiState())
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val key = aiConfigStorage.getApiKey()
            _uiState.update {
                it.copy(
                    apiKey = key,
                    isApiKeyConfigured = !key.isNullOrBlank()
                )
            }
        }
        viewModelScope.launch {
            AppContainer.accountRepository.getAccounts().collect { accs ->
                _uiState.update { current ->
                    current.copy(
                        accounts = accs,
                        selectedAccountId = current.selectedAccountId ?: accs.firstOrNull()?.id
                    )
                }
            }
        }
        viewModelScope.launch {
            AppContainer.categoryRepository.getCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun onFileSelected(file: StatementFile) {
        _uiState.update {
            it.copy(
                selectedFile = file,
                errorMessage = null
            )
        }
    }

    fun saveApiKey(key: String) {
        aiConfigStorage.setApiKey(key)
        _uiState.update {
            it.copy(
                apiKey = key,
                isApiKeyConfigured = key.isNotBlank(),
                errorMessage = null
            )
        }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun analyzeStatement() {
        val file = _uiState.value.selectedFile ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = ImportStage.ANALYZING,
                    analysisMessage = if (file.isPdf) "Extrayendo movimientos del PDF con Gemini..." else "Analizando registros CSV con Gemini...",
                    errorMessage = null
                )
            }

            analyzeBankStatementUseCase(file)
                .onSuccess { result ->
                    val matchedAccId = result.suggestedAccountId
                        ?: _uiState.value.selectedAccountId
                        ?: _uiState.value.accounts.firstOrNull()?.id

                    _uiState.update {
                        it.copy(
                            stage = ImportStage.REVIEWING,
                            parsedResult = result,
                            selectedAccountId = matchedAccId
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            stage = ImportStage.ERROR,
                            errorMessage = error.message ?: "Ocurrió un error al procesar el extracto bancario"
                        )
                    }
                }
        }
    }

    fun toggleItemSelection(itemId: String) {
        _uiState.update { current ->
            val result = current.parsedResult ?: return@update current
            val updatedItems = result.items.map { item ->
                if (item.id == itemId) item.copy(isSelected = !item.isSelected) else item
            }
            current.copy(parsedResult = result.copy(items = updatedItems))
        }
    }

    fun selectAll(select: Boolean) {
        _uiState.update { current ->
            val result = current.parsedResult ?: return@update current
            val updatedItems = result.items.map { it.copy(isSelected = select) }
            current.copy(parsedResult = result.copy(items = updatedItems))
        }
    }

    fun updateItemCategory(itemId: String, categoryId: String) {
        _uiState.update { current ->
            val result = current.parsedResult ?: return@update current
            val updatedItems = result.items.map { item ->
                if (item.id == itemId) item.copy(suggestedCategoryId = categoryId) else item
            }
            current.copy(parsedResult = result.copy(items = updatedItems))
        }
    }

    fun confirmImport(onSuccess: () -> Unit = {}) {
        val accountId = _uiState.value.selectedAccountId ?: return
        val items = _uiState.value.parsedResult?.items ?: return
        val bankName = _uiState.value.parsedResult?.detectedBankName

        viewModelScope.launch {
            _uiState.update { it.copy(stage = ImportStage.IMPORTING) }

            batchImportTransactionsUseCase(
                accountId = accountId,
                items = items,
                detectedBankName = bankName
            ).onSuccess { count ->
                _uiState.update {
                    it.copy(
                        stage = ImportStage.SUCCESS,
                        importedCount = count
                    )
                }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        stage = ImportStage.ERROR,
                        errorMessage = error.message ?: "Error al guardar las transacciones"
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                stage = ImportStage.FILE_SELECTION,
                selectedFile = null,
                parsedResult = null,
                importedCount = 0,
                errorMessage = null
            )
        }
    }
}
