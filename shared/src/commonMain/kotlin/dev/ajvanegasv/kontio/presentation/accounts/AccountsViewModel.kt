package dev.ajvanegasv.kontio.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val isAddAccountOpen: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AccountsViewModel(
    private val accountRepository: AccountRepository = AppContainer.accountRepository,
    private val createAccountUseCase: CreateAccountUseCase = AppContainer.createAccountUseCase
) : ViewModel() {

    private val _isAddAccountOpen = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAccounts(),
        _isAddAccountOpen,
        _errorMessage
    ) { accounts, isAddOpen, error ->
        var assets = 0.0
        var liabilities = 0.0

        for (acc in accounts) {
            if (acc.type == AccountType.CREDIT_CARD) {
                liabilities += acc.balance
            } else {
                assets += acc.balance
            }
        }

        AccountsUiState(
            accounts = accounts,
            totalAssets = assets,
            totalLiabilities = liabilities,
            isAddAccountOpen = isAddOpen,
            errorMessage = error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState(isLoading = true)
    )

    fun openAddAccount() {
        _isAddAccountOpen.value = true
        _errorMessage.value = null
    }

    fun closeAddAccount() {
        _isAddAccountOpen.value = false
        _errorMessage.value = null
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
        viewModelScope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
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
                _isAddAccountOpen.value = false
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al crear la cuenta"
            }
        }
    }
}
