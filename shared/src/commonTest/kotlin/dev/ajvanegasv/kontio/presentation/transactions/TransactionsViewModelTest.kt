package dev.ajvanegasv.kontio.presentation.transactions

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeTestAccountRepository : AccountRepository {
    private val accounts = MutableStateFlow<Map<String, Account>>(emptyMap())

    override fun getAccounts(): Flow<List<Account>> = accounts.map { it.values.toList() }
    override fun getAccountById(id: String): Flow<Account?> = accounts.map { it[id] }

    override suspend fun insertAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateBalance(accountId: String, newBalance: Double) {
        val current = accounts.value[accountId] ?: return
        accounts.value = accounts.value + (accountId to current.copy(balance = newBalance))
    }

    override suspend fun deleteAccount(id: String) {
        accounts.value = accounts.value - id
    }

    override suspend fun getAccountsCount(): Int = accounts.value.size
}

private class FakeTestTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactions.map { it.take(limit) }

    override fun getAllTransactions(): Flow<List<Transaction>> = transactions

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.accountId == accountId } }

    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.timestamp in startDate..endDate } }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactions.value = listOf(transaction) + transactions.value
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        this.transactions.value = transactions + this.transactions.value
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value.filterNot { it.id == id }
    }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactions.value.firstOrNull { it.id == id }

    override suspend fun deleteTransactionsByAccountId(accountId: String) {
        transactions.value = transactions.value.filterNot {
            it.accountId == accountId || it.targetAccountId == accountId
        }
    }

    override suspend fun getTransactionsCount(): Int = transactions.value.size

    override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
        transactions.value.count { it.categoryId == categoryId }
}

class TransactionsViewModelTest {

    @Test
    fun testInitialStateLoadsAllTransactionsAndCalculatesTotals() = runBlocking {
        val accountRepo = FakeTestAccountRepository()
        val txRepo = FakeTestTransactionRepository()
        val deleteUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        val account = Account(
            id = "acc-1",
            name = "Ahorros Principal",
            type = AccountType.SAVINGS,
            balance = 1000.0,
            currency = "USD",
            colorHex = "#10B981",
            iconName = "account_balance"
        )
        accountRepo.insertAccount(account)

        val tx1 = Transaction(
            id = "tx-1",
            accountId = "acc-1",
            categoryId = "cat-salary",
            type = TransactionType.INCOME,
            amount = 1200.0,
            currency = "USD",
            timestamp = 1000L,
            note = "Sueldo",
            category = Category(id = "cat-salary", name = "Salario", iconName = "salary", colorHex = "#10B981", type = TransactionType.INCOME),
            account = account
        )

        val tx2 = Transaction(
            id = "tx-2",
            accountId = "acc-1",
            categoryId = "cat-food",
            type = TransactionType.EXPENSE,
            amount = 150.0,
            currency = "USD",
            timestamp = 2000L,
            note = "Supermercado",
            category = Category(id = "cat-food", name = "Comida", iconName = "food", colorHex = "#F59E0B", type = TransactionType.EXPENSE),
            account = account
        )

        txRepo.insertTransaction(tx1)
        txRepo.insertTransaction(tx2)

        val viewModel = TransactionsViewModel(
            transactionRepository = txRepo,
            deleteTransactionUseCase = deleteUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        // Esperar primer estado
        val state = viewModel.uiState.first { it.allTransactions.isNotEmpty() }

        assertEquals(2, state.allTransactions.size)
        assertEquals(2, state.filteredTransactions.size)
        assertEquals(1200.0, state.totalIncome)
        assertEquals(150.0, state.totalExpenses)
        assertEquals(1050.0, state.netBalance)
    }

    @Test
    fun testFilteringByIncomeAndExpense() = runBlocking {
        val accountRepo = FakeTestAccountRepository()
        val txRepo = FakeTestTransactionRepository()
        val deleteUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        val account = Account(
            id = "acc-1",
            name = "Efectivo",
            type = AccountType.CASH,
            balance = 500.0,
            currency = "USD",
            colorHex = "#10B981",
            iconName = "payments"
        )
        accountRepo.insertAccount(account)

        txRepo.insertTransaction(
            Transaction(
                id = "tx-income",
                accountId = "acc-1",
                categoryId = "cat-1",
                type = TransactionType.INCOME,
                amount = 300.0,
                timestamp = 1000L,
                note = "Venta bicicleta"
            )
        )

        txRepo.insertTransaction(
            Transaction(
                id = "tx-expense",
                accountId = "acc-1",
                categoryId = "cat-2",
                type = TransactionType.EXPENSE,
                amount = 50.0,
                timestamp = 2000L,
                note = "Almuerzo"
            )
        )

        val viewModel = TransactionsViewModel(
            transactionRepository = txRepo,
            deleteTransactionUseCase = deleteUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        // Filtrar solo ingresos
        viewModel.setFilter(TransactionFilter.INCOME)
        val incomeState = viewModel.uiState.first { it.filter == TransactionFilter.INCOME && it.filteredTransactions.size == 1 }
        assertEquals(1, incomeState.filteredTransactions.size)
        assertEquals("tx-income", incomeState.filteredTransactions.first().id)

        // Filtrar solo egresos
        viewModel.setFilter(TransactionFilter.EXPENSE)
        val expenseState = viewModel.uiState.first { it.filter == TransactionFilter.EXPENSE && it.filteredTransactions.size == 1 }
        assertEquals(1, expenseState.filteredTransactions.size)
        assertEquals("tx-expense", expenseState.filteredTransactions.first().id)

        // Volver a todas
        viewModel.setFilter(TransactionFilter.ALL)
        val allState = viewModel.uiState.first { it.filter == TransactionFilter.ALL && it.filteredTransactions.size == 2 }
        assertEquals(2, allState.filteredTransactions.size)
    }

    @Test
    fun testSearchFiltering() = runBlocking {
        val accountRepo = FakeTestAccountRepository()
        val txRepo = FakeTestTransactionRepository()
        val deleteUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        txRepo.insertTransaction(
            Transaction(
                id = "tx-1",
                accountId = "acc-1",
                categoryId = "cat-1",
                type = TransactionType.EXPENSE,
                amount = 20.0,
                timestamp = 1000L,
                note = "Café y donas"
            )
        )

        txRepo.insertTransaction(
            Transaction(
                id = "tx-2",
                accountId = "acc-1",
                categoryId = "cat-2",
                type = TransactionType.EXPENSE,
                amount = 80.0,
                timestamp = 2000L,
                note = "Gasolina"
            )
        )

        val viewModel = TransactionsViewModel(
            transactionRepository = txRepo,
            deleteTransactionUseCase = deleteUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        viewModel.setSearchQuery("café")
        val searchState = viewModel.uiState.first { it.searchQuery == "café" && it.filteredTransactions.size == 1 }
        assertEquals("tx-1", searchState.filteredTransactions.first().id)
    }

    @Test
    fun testDeleteTransactionRecalculatesAccountBalance() = runBlocking {
        val accountRepo = FakeTestAccountRepository()
        val txRepo = FakeTestTransactionRepository()
        val deleteUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        val account = Account(
            id = "acc-1",
            name = "Efectivo",
            type = AccountType.CASH,
            balance = 100.0, // Después de un gasto de $50, el saldo quedó en $100
            currency = "USD",
            colorHex = "#10B981",
            iconName = "payments"
        )
        accountRepo.insertAccount(account)

        val tx = Transaction(
            id = "tx-to-delete",
            accountId = "acc-1",
            categoryId = "cat-1",
            type = TransactionType.EXPENSE,
            amount = 50.0,
            timestamp = 1000L,
            note = "Cena"
        )
        txRepo.insertTransaction(tx)

        val viewModel = TransactionsViewModel(
            transactionRepository = txRepo,
            deleteTransactionUseCase = deleteUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )
        val state = viewModel.uiState.first { it.allTransactions.isNotEmpty() }
        val itemToDelete = state.allTransactions.first()

        // Solicitar eliminación
        viewModel.requestDelete(itemToDelete)
        val deleteState = viewModel.uiState.first { it.transactionPendingDelete != null }
        assertEquals(itemToDelete, deleteState.transactionPendingDelete)

        // Cancelar eliminación
        viewModel.cancelDelete()
        val canceledState = viewModel.uiState.first { it.transactionPendingDelete == null }
        assertNull(canceledState.transactionPendingDelete)

        // Confirmar eliminación
        val deferred = kotlinx.coroutines.CompletableDeferred<Result<Unit>>()
        viewModel.requestDelete(itemToDelete)
        viewModel.confirmDelete { deferred.complete(it) }
        val deleteResult = deferred.await()
        assertTrue(deleteResult.isSuccess, "Delete failed: ${deleteResult.exceptionOrNull()?.message}")

        // Verificar que la transacción se eliminó del repositorio
        val updatedState = viewModel.uiState.first { it.allTransactions.isEmpty() }
        assertEquals(0, updatedState.allTransactions.size)

        // Verificar que el balance de la cuenta se incrementó en $50 (100 + 50 = 150)
        val updatedAccount = accountRepo.getAccountById("acc-1").first { it?.balance == 150.0 }
        assertNotNull(updatedAccount)
        assertEquals(150.0, updatedAccount.balance)
    }
}
