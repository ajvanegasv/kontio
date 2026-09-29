package dev.ajvanegasv.kontio.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetBudgetsWithProgressUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateBudgetUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BudgetsUiState(
    val budgets: List<BudgetWithProgress> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val totalBudgeted: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val globalPercentage: Float = 0f,
    val isAnyExceeded: Boolean = false,
    val isAddBudgetOpen: Boolean = false,
    val editingBudget: Budget? = null,
    val budgetPendingDelete: BudgetWithProgress? = null,
    val budgetForPayment: BudgetWithProgress? = null,
    val isSubmittingPayment: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

private data class DialogState(
    val isAddBudgetOpen: Boolean = false,
    val editingBudget: Budget? = null,
    val budgetPendingDelete: BudgetWithProgress? = null,
    val budgetForPayment: BudgetWithProgress? = null,
    val isSubmittingPayment: Boolean = false,
    val errorMessage: String? = null
)

class BudgetsViewModel(
    getBudgetsWithProgressUseCase: GetBudgetsWithProgressUseCase = AppContainer.getBudgetsWithProgressUseCase,
    categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    accountRepository: AccountRepository = AppContainer.accountRepository,
    private val createBudgetUseCase: CreateBudgetUseCase = AppContainer.createBudgetUseCase,
    private val updateBudgetUseCase: UpdateBudgetUseCase = AppContainer.updateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase = AppContainer.deleteBudgetUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase = AppContainer.createTransactionUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope
    private val _dialogState = MutableStateFlow(DialogState())

    val uiState: StateFlow<BudgetsUiState> = combine(
        getBudgetsWithProgressUseCase(),
        categoryRepository.getCategories(),
        accountRepository.getAccounts(),
        _dialogState
    ) { budgetsWithProgress, categories, accounts, dialog ->
        val expenseCategories = categories.filter { it.type == TransactionType.EXPENSE }
        val totalBudgeted = budgetsWithProgress.sumOf { it.limitAmount }
        val totalSpent = budgetsWithProgress.sumOf { it.spentAmount }
        val totalRemaining = (totalBudgeted - totalSpent).coerceAtLeast(0.0)
        val globalPercentage = if (totalBudgeted > 0.0) (totalSpent / totalBudgeted).toFloat() else 0f
        val isAnyExceeded = budgetsWithProgress.any { it.isExceeded }

        BudgetsUiState(
            budgets = budgetsWithProgress,
            categories = expenseCategories,
            accounts = accounts,
            totalBudgeted = totalBudgeted,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            globalPercentage = globalPercentage,
            isAnyExceeded = isAnyExceeded,
            isAddBudgetOpen = dialog.isAddBudgetOpen,
            editingBudget = dialog.editingBudget,
            budgetPendingDelete = dialog.budgetPendingDelete,
            budgetForPayment = dialog.budgetForPayment,
            isSubmittingPayment = dialog.isSubmittingPayment,
            isLoading = false,
            errorMessage = dialog.errorMessage
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState(isLoading = true)
    )

    fun openAddBudget() {
        _dialogState.value = _dialogState.value.copy(
            editingBudget = null,
            isAddBudgetOpen = true,
            errorMessage = null
        )
    }

    fun openEditBudget(budget: Budget) {
        _dialogState.value = _dialogState.value.copy(
            editingBudget = budget,
            isAddBudgetOpen = true,
            errorMessage = null
        )
    }

    fun closeAddBudget() {
        _dialogState.value = _dialogState.value.copy(
            isAddBudgetOpen = false,
            editingBudget = null,
            errorMessage = null
        )
    }

    fun saveBudget(
        name: String,
        categoryId: String,
        limitAmount: Double,
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        currency: String = "USD",
        note: String = "",
        onSuccess: () -> Unit = {}
    ) {
        scope.launch {
            val editing = _dialogState.value.editingBudget
            val result = if (editing != null) {
                updateBudgetUseCase(
                    id = editing.id,
                    name = name,
                    categoryId = categoryId,
                    limitAmount = limitAmount,
                    period = period,
                    currency = currency,
                    note = note
                )
            } else {
                createBudgetUseCase(
                    name = name,
                    categoryId = categoryId,
                    limitAmount = limitAmount,
                    period = period,
                    currency = currency,
                    note = note
                )
            }

            if (result.isSuccess) {
                _dialogState.value = _dialogState.value.copy(
                    isAddBudgetOpen = false,
                    editingBudget = null,
                    errorMessage = null
                )
                onSuccess()
            } else {
                _dialogState.value = _dialogState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Error al guardar presupuesto"
                )
            }
        }
    }

    fun requestDeleteBudget(budget: BudgetWithProgress) {
        _dialogState.value = _dialogState.value.copy(
            budgetPendingDelete = budget,
            errorMessage = null
        )
    }

    fun cancelDeleteBudget() {
        _dialogState.value = _dialogState.value.copy(budgetPendingDelete = null)
    }

    fun confirmDeleteBudget() {
        val target = _dialogState.value.budgetPendingDelete ?: return
        scope.launch {
            val result = deleteBudgetUseCase(target.budget.id)
            if (result.isSuccess) {
                _dialogState.value = _dialogState.value.copy(
                    budgetPendingDelete = null,
                    errorMessage = null
                )
            } else {
                _dialogState.value = _dialogState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Error al eliminar el presupuesto",
                    budgetPendingDelete = null
                )
            }
        }
    }

    fun openQuickPayment(budget: BudgetWithProgress) {
        _dialogState.value = _dialogState.value.copy(
            budgetForPayment = budget,
            errorMessage = null
        )
    }

    fun closeQuickPayment() {
        _dialogState.value = _dialogState.value.copy(
            budgetForPayment = null,
            errorMessage = null
        )
    }

    fun executeQuickPayment(
        amount: Double,
        accountId: String,
        dateMillis: Long,
        note: String,
        onSuccess: () -> Unit = {}
    ) {
        val budgetProgress = _dialogState.value.budgetForPayment ?: return
        if (amount <= 0.0) {
            _dialogState.value = _dialogState.value.copy(errorMessage = "El monto pagado debe ser mayor a 0")
            return
        }
        if (accountId.isBlank()) {
            _dialogState.value = _dialogState.value.copy(errorMessage = "Selecciona una cuenta de cargo")
            return
        }

        scope.launch {
            _dialogState.value = _dialogState.value.copy(isSubmittingPayment = true, errorMessage = null)

            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val txTimestamp = if (dateMillis > 0L) dateMillis else now
            val selectedAccount = uiState.value.accounts.firstOrNull { it.id == accountId }
            val currency = selectedAccount?.currency ?: budgetProgress.budget.currency

            val txNote = if (note.isNotBlank()) note.trim() else "Pago de presupuesto: ${budgetProgress.budget.name}"

            val newTx = Transaction(
                id = "tx_${now}_${(100..999).random()}",
                accountId = accountId,
                categoryId = budgetProgress.budget.categoryId,
                type = TransactionType.EXPENSE,
                amount = amount,
                currency = currency,
                timestamp = txTimestamp,
                note = txNote,
                budgetId = budgetProgress.budget.id
            )

            val result = createTransactionUseCase(newTx)
            _dialogState.value = _dialogState.value.copy(isSubmittingPayment = false)

            if (result.isSuccess) {
                _dialogState.value = _dialogState.value.copy(budgetForPayment = null)
                onSuccess()
            } else {
                _dialogState.value = _dialogState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Error al registrar el pago"
                )
            }
        }
    }

    fun clearError() {
        _dialogState.value = _dialogState.value.copy(errorMessage = null)
    }
}
