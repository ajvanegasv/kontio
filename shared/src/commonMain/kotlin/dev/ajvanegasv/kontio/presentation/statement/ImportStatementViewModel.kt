package dev.ajvanegasv.kontio.presentation.statement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.ParsedStatementResult
import dev.ajvanegasv.kontio.domain.model.StatementFile
import dev.ajvanegasv.kontio.domain.usecase.AnalyzeBankStatementUseCase
import dev.ajvanegasv.kontio.domain.usecase.BatchImportTransactionsUseCase
import kotlinx.coroutines.CoroutineScope
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
    val model: String = "gemini-3.8-flash",
    val availableModels: List<String> = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.5-flash-lite", "gemini-2.5-flash"),
    val isLoadingModels: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: String? = null,
    val isAccountManuallySelected: Boolean = false,
    val parsedResult: ParsedStatementResult? = null,
    val analysisMessage: String = "Preparando lectura...",
    val importedCount: Int = 0,
    val errorMessage: String? = null
)

class ImportStatementViewModel(
    private val analyzeBankStatementUseCase: AnalyzeBankStatementUseCase = AppContainer.analyzeBankStatementUseCase,
    private val batchImportTransactionsUseCase: BatchImportTransactionsUseCase = AppContainer.batchImportTransactionsUseCase,
    private val aiConfigStorage: AiConfigStorage = AppContainer.aiConfigStorage,
    private val accountRepository: dev.ajvanegasv.kontio.domain.repository.AccountRepository = AppContainer.accountRepository,
    private val categoryRepository: dev.ajvanegasv.kontio.domain.repository.CategoryRepository = AppContainer.categoryRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope
    private val _uiState = MutableStateFlow(ImportStatementUiState())
    val uiState: StateFlow<ImportStatementUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        scope.launch {
            val key = aiConfigStorage.getApiKey()
            val currentModel = aiConfigStorage.getModel().removePrefix("models/").trim().ifBlank { "gemini-3.8-flash" }
            _uiState.update {
                it.copy(
                    apiKey = key,
                    isApiKeyConfigured = !key.isNullOrBlank(),
                    model = currentModel
                )
            }
            if (!key.isNullOrBlank()) {
                refreshAvailableModels(key)
            }
        }
        scope.launch {
            aiConfigStorage.apiKeyFlow.collect { key ->
                _uiState.update {
                    it.copy(
                        apiKey = key,
                        isApiKeyConfigured = !key.isNullOrBlank()
                    )
                }
            }
        }
        scope.launch {
            aiConfigStorage.modelFlow.collect { currentModel ->
                val clean = currentModel.removePrefix("models/").trim()
                _uiState.update { it.copy(model = clean) }
            }
        }
        scope.launch {
            accountRepository.getAccounts().collect { accs ->
                _uiState.update { current ->
                    current.copy(
                        accounts = accs,
                        selectedAccountId = current.selectedAccountId ?: accs.firstOrNull()?.id
                    )
                }
            }
        }
        scope.launch {
            categoryRepository.getCategories().collect { cats ->
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

    fun saveApiKey(key: String, model: String = _uiState.value.model) {
        val cleanModel = model.removePrefix("models/").trim().ifBlank { GeminiApiClient.DEFAULT_MODEL }
        aiConfigStorage.setApiKey(key)
        aiConfigStorage.setModel(cleanModel)
        _uiState.update {
            it.copy(
                apiKey = key,
                isApiKeyConfigured = key.isNotBlank(),
                model = cleanModel,
                errorMessage = null
            )
        }
        if (key.isNotBlank()) {
            refreshAvailableModels(key)
        }
    }

    fun setModel(model: String) {
        val cleanModel = model.removePrefix("models/").trim().ifBlank { GeminiApiClient.DEFAULT_MODEL }
        aiConfigStorage.setModel(cleanModel)
        _uiState.update { it.copy(model = cleanModel) }
    }

    fun refreshAvailableModels(keyToUse: String? = null) {
        val key = keyToUse ?: _uiState.value.apiKey ?: aiConfigStorage.getApiKey() ?: return
        if (key.isBlank()) return

        scope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            val client = GeminiApiClient()
            client.fetchAvailableModels(key)
                .onSuccess { models ->
                    val flashFirst = models.sortedWith(
                        compareByDescending<String> { it.contains("flash", ignoreCase = true) }
                            .thenByDescending { it.contains("2.5") || it.contains("3.") }
                    ).take(6)

                    _uiState.update { current ->
                        val currentValid = flashFirst.contains(current.model)
                        val newSelected = if (currentValid) current.model else flashFirst.firstOrNull() ?: current.model
                        if (!currentValid && flashFirst.isNotEmpty()) {
                            aiConfigStorage.setModel(newSelected)
                        }
                        current.copy(
                            availableModels = if (flashFirst.isNotEmpty()) flashFirst else current.availableModels,
                            model = newSelected,
                            isLoadingModels = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingModels = false) }
                }
        }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId, isAccountManuallySelected = true) }
    }

    fun analyzeStatement() {
        val file = _uiState.value.selectedFile ?: return
        scope.launch {
            _uiState.update {
                it.copy(
                    stage = ImportStage.ANALYZING,
                    analysisMessage = if (file.isPdf) "Extrayendo movimientos del PDF con Gemini (${_uiState.value.model})...\nEsto puede tomar entre 15 y 35 segundos." else "Analizando registros CSV con Gemini...",
                    errorMessage = null
                )
            }

            analyzeBankStatementUseCase(file)
                .onSuccess { result ->
                    val matchedAccId = if (_uiState.value.isAccountManuallySelected && _uiState.value.selectedAccountId != null) {
                        _uiState.value.selectedAccountId
                    } else {
                        result.suggestedAccountId
                            ?: _uiState.value.selectedAccountId
                            ?: _uiState.value.accounts.firstOrNull()?.id
                    }

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

        scope.launch {
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
                isAccountManuallySelected = false,
                parsedResult = null,
                importedCount = 0,
                errorMessage = null
            )
        }
    }
}
