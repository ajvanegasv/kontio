package dev.ajvanegasv.kontio.presentation.transactions

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TransactionFilter {
    ALL,
    INCOME,
    EXPENSE
}

data class TransactionItemUiModel(
    val id: String,
    val title: String,
    val categoryName: String,
    val accountName: String,
    val amountFormatted: String,
    val amount: Double,
    val type: TransactionType,
    val isIncome: Boolean,
    val timestamp: Long,
    val dateGroup: String,
    val timeFormatted: String,
    val icon: ImageVector,
    val currency: String,
    val originalTransaction: Transaction
)

data class TransactionsUiState(
    val allTransactions: List<TransactionItemUiModel> = emptyList(),
    val filteredTransactions: List<TransactionItemUiModel> = emptyList(),
    val groupedTransactions: Map<String, List<TransactionItemUiModel>> = emptyMap(),
    val filter: TransactionFilter = TransactionFilter.ALL,
    val searchQuery: String = "",
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0,
    val currency: String = "USD",
    val isLoading: Boolean = false,
    val transactionPendingDelete: TransactionItemUiModel? = null,
    val totalCount: Int = 0
)

class TransactionsViewModel(
    transactionRepository: TransactionRepository = AppContainer.transactionRepository,
    private val deleteTransactionUseCase: DeleteTransactionUseCase = AppContainer.deleteTransactionUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _filter = MutableStateFlow(TransactionFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _transactionPendingDelete = MutableStateFlow<TransactionItemUiModel?>(null)

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _filter,
        _searchQuery,
        _transactionPendingDelete
    ) { domainList, filter, query, pendingDelete ->
        var incomeSum = 0.0
        var expenseSum = 0.0
        val defaultCurrency = domainList.firstOrNull()?.currency ?: "USD"

        val uiModels = domainList.map { tx ->
            val isIncome = tx.type == TransactionType.INCOME
            if (isIncome) {
                incomeSum += tx.amount
            } else if (tx.type == TransactionType.EXPENSE) {
                expenseSum += tx.amount
            }

            val categoryName = tx.category?.name ?: if (isIncome) "Ingreso" else "Gasto"
            val title = tx.note.ifBlank { categoryName }
            val accountName = tx.account?.name ?: "Cuenta"
            val icon = IconMapper.getIconForCategory(tx.category?.iconName ?: "", tx.type)

            TransactionItemUiModel(
                id = tx.id,
                title = title,
                categoryName = categoryName,
                accountName = accountName,
                amountFormatted = CurrencyFormatter.format(tx.amount, tx.currency, showSign = true),
                amount = tx.amount,
                type = tx.type,
                isIncome = isIncome,
                timestamp = tx.timestamp,
                dateGroup = DateFormatter.formatDateGroup(tx.timestamp),
                timeFormatted = DateFormatter.formatTime(tx.timestamp),
                icon = icon,
                currency = tx.currency,
                originalTransaction = tx
            )
        }

        val filtered = uiModels.filter { item ->
            val matchesFilter = when (filter) {
                TransactionFilter.ALL -> true
                TransactionFilter.INCOME -> item.isIncome
                TransactionFilter.EXPENSE -> item.type == TransactionType.EXPENSE
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                    item.categoryName.contains(query, ignoreCase = true) ||
                    item.accountName.contains(query, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }

        // Agrupar preservando el orden cronológico
        val grouped = linkedMapOf<String, MutableList<TransactionItemUiModel>>()
        for (item in filtered) {
            val list = grouped.getOrPut(item.dateGroup) { mutableListOf() }
            list.add(item)
        }

        TransactionsUiState(
            allTransactions = uiModels,
            filteredTransactions = filtered,
            groupedTransactions = grouped,
            filter = filter,
            searchQuery = query,
            totalIncome = incomeSum,
            totalExpenses = expenseSum,
            netBalance = incomeSum - expenseSum,
            currency = defaultCurrency,
            isLoading = false,
            transactionPendingDelete = pendingDelete,
            totalCount = filtered.size
        )
    }.stateIn(
        scope = scope,
        started = if (coroutineScope != null) SharingStarted.Eagerly else SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(isLoading = true)
    )

    fun setFilter(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun requestDelete(transaction: TransactionItemUiModel) {
        _transactionPendingDelete.value = transaction
    }

    fun cancelDelete() {
        _transactionPendingDelete.value = null
    }

    fun confirmDelete(onComplete: (Result<Unit>) -> Unit = {}) {
        val tx = _transactionPendingDelete.value ?: return
        _transactionPendingDelete.value = null
        scope.launch {
            val result = deleteTransactionUseCase(tx.id)
            onComplete(result)
        }
    }
}
