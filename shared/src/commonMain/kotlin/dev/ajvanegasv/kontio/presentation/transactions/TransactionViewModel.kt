package dev.ajvanegasv.kontio.presentation.transactions

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class TransactionCreationUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountString: String = "0",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
) {
    val numericAmount: Double
        get() = amountString.toDoubleOrNull() ?: 0.0
}

class TransactionViewModel(
    accountRepository: AccountRepository = AppContainer.accountRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    private val createTransactionUseCase: CreateTransactionUseCase = AppContainer.createTransactionUseCase
) : ViewModel() {

    private data class FormFields(
        val type: TransactionType = TransactionType.EXPENSE,
        val amountString: String = "0",
        val selectedAccountId: String? = null,
        val selectedCategoryId: String? = null,
        val note: String = ""
    )

    private val _form = MutableStateFlow(FormFields())
    private val _isSubmitting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isSuccess = MutableStateFlow(false)

    val uiState: StateFlow<TransactionCreationUiState> = combine(
        accountRepository.getAccounts(),
        categoryRepository.getCategories(),
        _form,
        _errorMessage,
        _isSubmitting
    ) { accounts, allCategories, form, error, submitting ->
        val filteredCategories = allCategories.filter { it.type == form.type }
        val resolvedAccountId = form.selectedAccountId ?: accounts.firstOrNull()?.id
        val resolvedCategoryId = form.selectedCategoryId ?: filteredCategories.firstOrNull()?.id

        TransactionCreationUiState(
            type = form.type,
            amountString = form.amountString,
            selectedAccountId = resolvedAccountId,
            selectedCategoryId = resolvedCategoryId,
            note = form.note,
            accounts = accounts,
            categories = filteredCategories,
            isSubmitting = submitting,
            errorMessage = error,
            isSuccess = _isSuccess.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionCreationUiState()
    )

    fun setTransactionType(type: TransactionType) {
        _form.value = _form.value.copy(type = type, selectedCategoryId = null)
    }

    fun onNumberPadClick(digit: String) {
        val current = _form.value.amountString
        if (digit == ".") {
            if (!current.contains(".")) {
                _form.value = _form.value.copy(amountString = "$current.")
            }
            return
        }

        if (current == "0") {
            _form.value = _form.value.copy(amountString = digit)
        } else {
            val dotIndex = current.indexOf(".")
            if (dotIndex != -1 && current.length - dotIndex > 2) {
                return
            }
            _form.value = _form.value.copy(amountString = current + digit)
        }
    }

    fun onBackspaceClick() {
        val current = _form.value.amountString
        if (current.length <= 1) {
            _form.value = _form.value.copy(amountString = "0")
        } else {
            _form.value = _form.value.copy(amountString = current.dropLast(1))
        }
    }

    fun selectAccount(accountId: String) {
        _form.value = _form.value.copy(selectedAccountId = accountId)
    }

    fun selectCategory(categoryId: String) {
        _form.value = _form.value.copy(selectedCategoryId = categoryId)
    }

    fun setNote(note: String) {
        _form.value = _form.value.copy(note = note)
    }

    fun submitTransaction(onSuccess: () -> Unit = {}) {
        val state = uiState.value
        val amount = state.numericAmount
        if (amount <= 0.0) {
            _errorMessage.value = "El monto debe ser mayor a 0"
            return
        }
        val accountId = state.selectedAccountId
        if (accountId.isNullOrBlank()) {
            _errorMessage.value = "Selecciona una cuenta"
            return
        }
        val categoryId = state.selectedCategoryId
        if (categoryId.isNullOrBlank()) {
            _errorMessage.value = "Selecciona una categoría"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val now = Clock.System.now().toEpochMilliseconds()
            val newTx = Transaction(
                id = "tx_${now}_${(100..999).random()}",
                accountId = accountId,
                categoryId = categoryId,
                type = state.type,
                amount = amount,
                currency = state.accounts.firstOrNull { it.id == accountId }?.currency ?: "USD",
                timestamp = now,
                note = state.note.trim()
            )

            val result = createTransactionUseCase(newTx)
            _isSubmitting.value = false
            if (result.isSuccess) {
                _form.value = FormFields()
                _isSuccess.value = true
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al registrar la transacción"
            }
        }
    }
}
