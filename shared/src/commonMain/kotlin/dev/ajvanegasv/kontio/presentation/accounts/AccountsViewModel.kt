package dev.ajvanegasv.kontio.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.ArchiveAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.ReassignTransactionsAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateAccountUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class AccountFormState(
    val isOpen: Boolean = false,
    val editingAccount: Account? = null
)

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val isAddAccountOpen: Boolean = false,
    val editingAccount: Account? = null,
    val selectedAccountId: String? = null,
    val selectedAccountTransactions: List<Transaction> = emptyList(),
    val accountTransactionCounts: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedAccount: Account?
        get() = accounts.firstOrNull { it.id == selectedAccountId }

    val isAccountFormOpen: Boolean
        get() = isAddAccountOpen || editingAccount != null
}

class AccountsViewModel(
    private val accountRepository: AccountRepository = AppContainer.accountRepository,
    private val transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    private val createAccountUseCase: CreateAccountUseCase = AppContainer.createAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase = AppContainer.updateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase = AppContainer.deleteAccountUseCase,
    private val archiveAccountUseCase: ArchiveAccountUseCase = AppContainer.archiveAccountUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase = AppContainer.deleteTransactionUseCase,
    private val reassignTransactionsAccountUseCase: ReassignTransactionsAccountUseCase = AppContainer.reassignTransactionsAccountUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _formState = MutableStateFlow(AccountFormState())
    private val _selectedAccountId = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAccounts(),
        transactionRepository.getAllTransactions(),
        _selectedAccountId,
        _formState,
        _errorMessage
    ) { accounts, allTransactions, selectedId, formState, error ->
        var assets = 0.0
        var liabilities = 0.0

        for (acc in accounts) {
            if (acc.type == AccountType.CREDIT_CARD) {
                liabilities += acc.balance
            } else {
                assets += acc.balance
            }
        }

        val accountTransactions = if (selectedId != null) {
            allTransactions.filter { it.accountId == selectedId }.sortedByDescending { it.timestamp }
        } else {
            emptyList()
        }

        val transactionCounts = allTransactions.groupingBy { it.accountId }.eachCount()

        AccountsUiState(
            accounts = accounts,
            totalAssets = assets,
            totalLiabilities = liabilities,
            isAddAccountOpen = formState.isOpen && formState.editingAccount == null,
            editingAccount = formState.editingAccount,
            selectedAccountId = selectedId,
            selectedAccountTransactions = accountTransactions,
            accountTransactionCounts = transactionCounts,
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState(isLoading = true)
    )

    fun selectAccountForDetail(accountId: String?) {
        _selectedAccountId.value = accountId
        _errorMessage.value = null
    }

    fun openAddAccount() {
        _formState.value = AccountFormState(isOpen = true, editingAccount = null)
        _errorMessage.value = null
    }

    fun openEditAccount(account: Account) {
        _formState.value = AccountFormState(isOpen = true, editingAccount = account)
        _errorMessage.value = null
    }

    fun closeAccountForm() {
        _formState.value = AccountFormState(isOpen = false, editingAccount = null)
        _errorMessage.value = null
    }

    fun closeAddAccount() {
        closeAccountForm()
    }

    fun createAccount(
        name: String,
        type: AccountType,
        initialBalance: Double,
        currency: String = "USD",
        colorHex: String = "#3B82F6",
        creditLimit: Double? = null,
        cutoffDay: Int? = null,
        dueDay: Int? = null
    ) {
        scope.launch {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val newAccount = Account(
                id = "acc_${now}_${(100..999).random()}",
                name = name,
                type = type,
                balance = initialBalance,
                currency = currency,
                colorHex = colorHex,
                iconName = when (type) {
                    AccountType.SAVINGS -> "account_balance"
                    AccountType.CHECKING -> "account_balance"
                    AccountType.CREDIT_CARD -> "credit_card"
                    AccountType.CASH -> "payments"
                    AccountType.DIGITAL_WALLET -> "account_balance_wallet"
                },
                creditLimit = creditLimit,
                cutoffDay = cutoffDay,
                dueDay = dueDay,
                createdAt = now,
                updatedAt = now
            )

            val result = createAccountUseCase(newAccount)
            if (result.isSuccess) {
                closeAccountForm()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al crear la cuenta"
            }
        }
    }

    fun updateAccount(account: Account, onComplete: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            val updated = account.copy(
                iconName = when (account.type) {
                    AccountType.SAVINGS -> "account_balance"
                    AccountType.CHECKING -> "account_balance"
                    AccountType.CREDIT_CARD -> "credit_card"
                    AccountType.CASH -> "payments"
                    AccountType.DIGITAL_WALLET -> "account_balance_wallet"
                },
                updatedAt = now
            )
            val result = updateAccountUseCase(updated)
            if (result.isSuccess) {
                closeAccountForm()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al actualizar la cuenta"
            }
            onComplete(result)
        }
    }

    fun deleteAccount(accountId: String, onComplete: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            val result = deleteAccountUseCase(accountId)
            if (result.isSuccess) {
                if (_selectedAccountId.value == accountId) {
                    _selectedAccountId.value = null
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al eliminar la cuenta"
            }
            onComplete(result)
        }
    }

    fun archiveAccount(accountId: String, onComplete: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            val result = archiveAccountUseCase(accountId)
            if (result.isSuccess) {
                if (_selectedAccountId.value == accountId) {
                    _selectedAccountId.value = null
                }
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al archivar la cuenta"
            }
            onComplete(result)
        }
    }

    fun deleteTransaction(transactionId: String, onComplete: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            val result = deleteTransactionUseCase(transactionId)
            onComplete(result)
        }
    }

    fun reassignAccountTransactions(
        fromAccountId: String,
        toAccountId: String,
        transactionIds: List<String>? = null,
        onComplete: (Result<Int>) -> Unit = {}
    ) {
        scope.launch {
            val result = reassignTransactionsAccountUseCase(
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                transactionIds = transactionIds
            )
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al reasignar transacciones"
            }
            onComplete(result)
        }
    }
}

