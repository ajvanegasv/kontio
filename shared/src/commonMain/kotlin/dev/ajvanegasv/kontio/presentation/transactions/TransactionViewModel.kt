package dev.ajvanegasv.kontio.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateTransactionUseCase
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionCreationUiState(
    val isEditing: Boolean = false,
    val editingTransactionId: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountString: String = "0",
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedBudgetId: String? = null,
    val note: String = "",
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
) {
    val numericAmount: Double
        get() = amountString.replace(",", "").toDoubleOrNull() ?: 0.0

    val formattedDate: String
        get() = DateFormatter.formatDisplayDate(timestamp)
}

class TransactionViewModel(
    accountRepository: AccountRepository = AppContainer.accountRepository,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    private val budgetRepository: BudgetRepository = AppContainer.budgetRepository,
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    private val createTransactionUseCase: CreateTransactionUseCase = AppContainer.createTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase = AppContainer.updateTransactionUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private data class FormFields(
        val isEditing: Boolean = false,
        val editingTransactionId: String? = null,
        val type: TransactionType = TransactionType.EXPENSE,
        val amountString: String = "0",
        val selectedAccountId: String? = null,
        val selectedCategoryId: String? = null,
        val selectedBudgetId: String? = null,
        val note: String = "",
        val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    )

    private val _form = MutableStateFlow(FormFields())
    private val _isSubmitting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _isSuccess = MutableStateFlow(false)

    private val _staticDataFlow = combine(
        accountRepository.getAccounts(),
        categoryRepository.getCategories(),
        budgetRepository.getBudgets()
    ) { accounts, allCategories, allBudgets ->
        Triple(accounts, allCategories, allBudgets)
    }

    val uiState: StateFlow<TransactionCreationUiState> = combine(
        _staticDataFlow,
        _form,
        _errorMessage,
        _isSubmitting
    ) { (accounts, allCategories, allBudgets), form, error, submitting ->
        val filteredCategories = allCategories.filter { it.type == form.type }
        val resolvedAccountId = form.selectedAccountId ?: accounts.firstOrNull()?.id
        val resolvedCategoryId = form.selectedCategoryId ?: filteredCategories.firstOrNull()?.id
        val activeBudgets = if (form.type == TransactionType.EXPENSE) allBudgets else emptyList()

        TransactionCreationUiState(
            isEditing = form.isEditing,
            editingTransactionId = form.editingTransactionId,
            type = form.type,
            amountString = form.amountString,
            selectedAccountId = resolvedAccountId,
            selectedCategoryId = resolvedCategoryId,
            selectedBudgetId = form.selectedBudgetId,
            note = form.note,
            timestamp = form.timestamp,
            accounts = accounts,
            categories = filteredCategories,
            budgets = activeBudgets,
            isSubmitting = submitting,
            errorMessage = error,
            isSuccess = _isSuccess.value
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionCreationUiState()
    )

    fun prepareTransaction(type: TransactionType = TransactionType.EXPENSE, accountId: String? = null) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        _form.value = FormFields(
            isEditing = false,
            editingTransactionId = null,
            type = type,
            amountString = "0",
            selectedAccountId = accountId ?: _form.value.selectedAccountId,
            selectedCategoryId = null,
            selectedBudgetId = null,
            note = "",
            timestamp = now
        )
        _errorMessage.value = null
    }

    fun prepareEditTransaction(transaction: Transaction) {
        val amountStr = if (transaction.amount % 1.0 == 0.0) {
            transaction.amount.toLong().toString()
        } else {
            transaction.amount.toString()
        }
        _form.value = FormFields(
            isEditing = true,
            editingTransactionId = transaction.id,
            type = transaction.type,
            amountString = amountStr,
            selectedAccountId = transaction.accountId,
            selectedCategoryId = transaction.categoryId,
            selectedBudgetId = transaction.budgetId,
            note = transaction.note,
            timestamp = transaction.timestamp
        )
        _errorMessage.value = null
    }

    fun prepareEditTransactionById(transactionId: String) {
        scope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
            if (tx != null) {
                prepareEditTransaction(tx)
            }
        }
    }

    fun setDate(timestamp: Long) {
        _form.value = _form.value.copy(timestamp = timestamp)
    }

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

    fun selectBudget(budgetId: String?) {
        _form.value = _form.value.copy(selectedBudgetId = budgetId)
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

        scope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null

            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val txTimestamp = if (state.timestamp > 0L) state.timestamp else now
            val selectedCurrency = state.accounts.firstOrNull { it.id == accountId }?.currency ?: "USD"
            val selectedBudgetId = state.selectedBudgetId

            val result = if (state.isEditing && state.editingTransactionId != null) {
                val updatedTx = Transaction(
                    id = state.editingTransactionId,
                    accountId = accountId,
                    categoryId = categoryId,
                    type = state.type,
                    amount = amount,
                    currency = selectedCurrency,
                    timestamp = txTimestamp,
                    note = state.note.trim(),
                    budgetId = selectedBudgetId
                )
                val updateRes = updateTransactionUseCase(updatedTx)
                if (updateRes.isSuccess && selectedBudgetId != null) {
                    budgetRepository.linkTransactionToBudget(selectedBudgetId, updatedTx.id)
                }
                updateRes
            } else {
                val newTxId = "tx_${now}_${(100..999).random()}"
                val newTx = Transaction(
                    id = newTxId,
                    accountId = accountId,
                    categoryId = categoryId,
                    type = state.type,
                    amount = amount,
                    currency = selectedCurrency,
                    timestamp = txTimestamp,
                    note = state.note.trim(),
                    budgetId = selectedBudgetId
                )
                val createRes = createTransactionUseCase(newTx)
                if (createRes.isSuccess && selectedBudgetId != null) {
                    budgetRepository.linkTransactionToBudget(selectedBudgetId, newTx.id)
                }
                createRes
            }

            _isSubmitting.value = false
            if (result.isSuccess) {
                _form.value = FormFields()
                _isSuccess.value = true
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al guardar la transacción"
            }
        }
    }
}
