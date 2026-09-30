package dev.ajvanegasv.kontio.presentation.transactions.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.ParseVoiceTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VoiceStep {
    DICTATING,
    ANALYZING,
    PREVIEW_EDITABLE
}

data class VoiceTransactionDraft(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountString: String = "0",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val note: String = "",
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val rawVoiceText: String = "",
    val confidence: Float = 1.0f
) {
    val numericAmount: Double
        get() = amountString.replace(",", "").toDoubleOrNull() ?: 0.0
}

data class VoiceTransactionUiState(
    val step: VoiceStep = VoiceStep.DICTATING,
    val draft: VoiceTransactionDraft = VoiceTransactionDraft(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val activeCategories: List<Category>
        get() = categories.filter { it.type == draft.type }
}

class VoiceTransactionViewModel(
    private val parseVoiceTransactionUseCase: ParseVoiceTransactionUseCase = AppContainer.parseVoiceTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase = AppContainer.createTransactionUseCase,
    private val accountRepository: AccountRepository = AppContainer.accountRepository,
    private val categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private data class FormState(
        val step: VoiceStep = VoiceStep.DICTATING,
        val draft: VoiceTransactionDraft = VoiceTransactionDraft(),
        val isSubmitting: Boolean = false,
        val isSuccess: Boolean = false,
        val errorMessage: String? = null
    )

    private val _form = MutableStateFlow(FormState())

    val uiState: StateFlow<VoiceTransactionUiState> = combine(
        accountRepository.getAccounts(),
        categoryRepository.getCategories(),
        _form
    ) { accounts, allCategories, form ->
        val filteredCategories = allCategories.filter { it.type == form.draft.type }
        val resolvedAccountId = form.draft.selectedAccountId ?: accounts.firstOrNull()?.id
        val resolvedCategoryId = form.draft.selectedCategoryId ?: filteredCategories.firstOrNull()?.id

        val resolvedDraft = form.draft.copy(
            selectedAccountId = resolvedAccountId,
            selectedCategoryId = resolvedCategoryId
        )

        VoiceTransactionUiState(
            step = form.step,
            draft = resolvedDraft,
            accounts = accounts,
            categories = allCategories,
            isSubmitting = form.isSubmitting,
            isSuccess = form.isSuccess,
            errorMessage = form.errorMessage
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoiceTransactionUiState()
    )

    fun startDictation() {
        _form.value = _form.value.copy(
            step = VoiceStep.DICTATING,
            errorMessage = null,
            isSuccess = false
        )
    }

    fun processVoiceText(voiceText: String) {
        val trimmed = voiceText.trim()
        if (trimmed.isBlank()) {
            _form.value = _form.value.copy(errorMessage = "Por favor dicta o escribe un movimiento")
            return
        }

        scope.launch {
            _form.value = _form.value.copy(
                step = VoiceStep.ANALYZING,
                errorMessage = null
            )

            val currentAccounts = accountRepository.getAccounts().first()
            val currentCategories = categoryRepository.getCategories().first()
            val result = parseVoiceTransactionUseCase(
                voiceText = trimmed,
                accounts = currentAccounts,
                categories = currentCategories
            )

            if (result.isSuccess) {
                val parsed = result.getOrThrow()
                val amountStr = if (parsed.amount % 1.0 == 0.0) {
                    parsed.amount.toLong().toString()
                } else {
                    parsed.amount.toString()
                }

                _form.value = _form.value.copy(
                    step = VoiceStep.PREVIEW_EDITABLE,
                    draft = VoiceTransactionDraft(
                        type = parsed.type,
                        amountString = amountStr,
                        selectedAccountId = parsed.suggestedAccountId,
                        selectedCategoryId = parsed.suggestedCategoryId,
                        note = parsed.note,
                        timestamp = parsed.timestamp,
                        rawVoiceText = parsed.rawVoiceText,
                        confidence = parsed.confidence
                    ),
                    errorMessage = null
                )
            } else {
                _form.value = _form.value.copy(
                    step = VoiceStep.DICTATING,
                    errorMessage = result.exceptionOrNull()?.message ?: "Error al analizar la voz"
                )
            }
        }
    }

    fun setDraftType(type: TransactionType) {
        val currentCategories = uiState.value.categories.filter { it.type == type }
        val newCategory = currentCategories.firstOrNull()?.id
        _form.value = _form.value.copy(
            draft = _form.value.draft.copy(
                type = type,
                selectedCategoryId = newCategory
            )
        )
    }

    fun setDraftAmount(amountString: String) {
        _form.value = _form.value.copy(
            draft = _form.value.draft.copy(amountString = amountString)
        )
    }

    fun setDraftAccount(accountId: String) {
        _form.value = _form.value.copy(
            draft = _form.value.draft.copy(selectedAccountId = accountId)
        )
    }

    fun setDraftCategory(categoryId: String) {
        _form.value = _form.value.copy(
            draft = _form.value.draft.copy(selectedCategoryId = categoryId)
        )
    }

    fun setDraftDate(timestamp: Long) {
        _form.value = _form.value.copy(
            draft = _form.value.draft.copy(timestamp = timestamp)
        )
    }

    fun setDraftNote(note: String) {
        _form.value = _form.value.copy(
            draft = _form.value.draft.copy(note = note)
        )
    }

    fun retryDictation() {
        _form.value = _form.value.copy(
            step = VoiceStep.DICTATING,
            errorMessage = null
        )
    }

    fun submitDraft(onSuccess: () -> Unit) {
        val state = uiState.value
        val draft = state.draft
        val amount = draft.numericAmount

        if (amount <= 0.0) {
            _form.value = _form.value.copy(errorMessage = "El monto debe ser mayor a 0")
            return
        }

        val accountId = draft.selectedAccountId
        if (accountId.isNullOrBlank()) {
            _form.value = _form.value.copy(errorMessage = "Selecciona una cuenta")
            return
        }

        val categoryId = draft.selectedCategoryId
        if (categoryId.isNullOrBlank()) {
            _form.value = _form.value.copy(errorMessage = "Selecciona una categoría")
            return
        }

        scope.launch {
            _form.value = _form.value.copy(
                isSubmitting = true,
                errorMessage = null
            )

            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val txTimestamp = if (draft.timestamp > 0L) draft.timestamp else now
            val selectedCurrency = state.accounts.firstOrNull { it.id == accountId }?.currency ?: "USD"

            val transaction = Transaction(
                id = "tx_${now}_${(100..999).random()}",
                accountId = accountId,
                categoryId = categoryId,
                type = draft.type,
                amount = amount,
                currency = selectedCurrency,
                timestamp = txTimestamp,
                note = draft.note.trim()
            )

            val result = createTransactionUseCase(transaction)
            if (result.isSuccess) {
                _form.value = _form.value.copy(
                    isSubmitting = false,
                    isSuccess = true
                )
                onSuccess()
            } else {
                _form.value = _form.value.copy(
                    isSubmitting = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Error al guardar la transacción"
                )
            }
        }
    }

    fun reset() {
        _form.value = FormState()
    }
}
