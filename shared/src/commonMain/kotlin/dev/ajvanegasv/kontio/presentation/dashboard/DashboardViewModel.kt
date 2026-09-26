package dev.ajvanegasv.kontio.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.GetDashboardSummaryUseCase
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTransaction
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val balance: String = "$0.00",
    val incomeAmount: String = "$0.00",
    val expensesAmount: String = "$0.00",
    val transactions: List<DashboardTransaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false
)

class DashboardViewModel(
    getDashboardSummaryUseCase: GetDashboardSummaryUseCase = AppContainer.getDashboardSummaryUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getDashboardSummaryUseCase()
        .map { summary ->
            val currency = summary.currency
            val formattedTransactions = summary.recentTransactions.map { tx ->
                val isIncome = tx.type == TransactionType.INCOME
                val amountStr = CurrencyFormatter.format(tx.amount, currency, showSign = true)
                val categoryName = tx.category?.name ?: if (isIncome) "Ingreso" else "Gasto"
                val title = tx.note.ifBlank { categoryName }
                val icon = IconMapper.getIconForCategory(tx.category?.iconName ?: "", tx.type)

                DashboardTransaction(
                    id = tx.id,
                    title = title,
                    category = categoryName,
                    amount = amountStr,
                    isIncome = isIncome,
                    icon = icon
                )
            }

            DashboardUiState(
                balance = CurrencyFormatter.format(summary.totalBalance, currency),
                incomeAmount = CurrencyFormatter.format(summary.monthlyIncome, currency),
                expensesAmount = CurrencyFormatter.format(summary.monthlyExpenses, currency),
                transactions = formattedTransactions,
                accounts = summary.accounts,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true)
        )
}
