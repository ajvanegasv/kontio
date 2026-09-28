package dev.ajvanegasv.kontio.presentation.transactions

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeTestAccountRepo : AccountRepository {
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

private class FakeTestCategoryRepo : CategoryRepository {
    private val categories = MutableStateFlow<List<Category>>(emptyList())

    override fun getCategories(type: TransactionType?): Flow<List<Category>> =
        categories.map { list -> if (type == null) list else list.filter { it.type == type } }

    override fun getCategoryById(id: String): Flow<Category?> =
        categories.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun insertCategory(category: Category) {
        categories.value = categories.value + category
    }

    override suspend fun deleteCategory(id: String) {
        categories.value = categories.value.filterNot { it.id == id }
    }

    override suspend fun seedDefaultCategoriesIfEmpty() {}

    override suspend fun getCategoriesCount(): Int = categories.value.size
}

private class FakeTestTxRepo : TransactionRepository {
    val savedTransactions = mutableListOf<Transaction>()

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        MutableStateFlow(savedTransactions)

    override fun getAllTransactions(): Flow<List<Transaction>> =
        MutableStateFlow(savedTransactions)

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        MutableStateFlow(savedTransactions.filter { it.accountId == accountId })

    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        MutableStateFlow(savedTransactions.filter { it.timestamp in startDate..endDate })

    override suspend fun insertTransaction(transaction: Transaction) {
        val existingIndex = savedTransactions.indexOfFirst { it.id == transaction.id }
        if (existingIndex >= 0) {
            savedTransactions[existingIndex] = transaction
        } else {
            savedTransactions.add(transaction)
        }
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        transactions.forEach { insertTransaction(it) }
    }

    override suspend fun deleteTransaction(id: String) {
        savedTransactions.removeAll { it.id == id }
    }

    override suspend fun getTransactionById(id: String): Transaction? =
        savedTransactions.firstOrNull { it.id == id }

    override suspend fun deleteTransactionsByAccountId(accountId: String) {
        savedTransactions.removeAll { it.accountId == accountId }
    }

    override suspend fun getTransactionsCount(): Int = savedTransactions.size

    override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
        savedTransactions.count { it.categoryId == categoryId }
}

class TransactionViewModelTest {

    @Test
    fun testPrepareTransactionPreSelectsAccountAndSetsCustomDate() = runBlocking {
        val accountRepo = FakeTestAccountRepo()
        val catRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTxRepo()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)

        val account1 = Account(
            id = "acc-main",
            name = "Principal",
            type = AccountType.CHECKING,
            balance = 500.0,
            currency = "USD",
            colorHex = "#3B82F6",
            iconName = "account_balance"
        )
        val account2 = Account(
            id = "acc-target-card",
            name = "Tarjeta Target",
            type = AccountType.CREDIT_CARD,
            balance = 100.0,
            currency = "USD",
            colorHex = "#1E293B",
            iconName = "credit_card"
        )
        accountRepo.insertAccount(account1)
        accountRepo.insertAccount(account2)

        val category = Category(
            id = "cat-food",
            name = "Comida",
            iconName = "restaurant",
            colorHex = "#EF4444",
            type = TransactionType.EXPENSE
        )
        catRepo.insertCategory(category)

        val updateTxUseCase = UpdateTransactionUseCase(txRepo, accountRepo)
        val viewModel = TransactionViewModel(
            accountRepository = accountRepo,
            categoryRepository = catRepo,
            transactionRepository = txRepo,
            createTransactionUseCase = createTxUseCase,
            updateTransactionUseCase = updateTxUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        // 1. Preparar transacción con cuenta específica pre-seleccionada
        viewModel.prepareTransaction(type = TransactionType.EXPENSE, accountId = "acc-target-card")
        val preparedState = viewModel.uiState.first { it.accounts.size == 2 && it.selectedAccountId == "acc-target-card" }
        assertEquals("acc-target-card", preparedState.selectedAccountId)
        assertEquals(TransactionType.EXPENSE, preparedState.type)

        // 2. Modificar la fecha a una fecha personalizada (ej: 1700000000000L)
        val customDate = 1700000000000L
        viewModel.setDate(customDate)
        val dateState = viewModel.uiState.first { it.timestamp == customDate }
        assertEquals(customDate, dateState.timestamp)

        // 3. Digitar monto y enviar transacción
        viewModel.onNumberPadClick("4")
        viewModel.onNumberPadClick("5")
        viewModel.selectCategory("cat-food")

        viewModel.submitTransaction()

        // Verificar que la transacción guardada tenga la cuenta preseleccionada y la fecha personalizada
        assertEquals(1, txRepo.savedTransactions.size)
        val saved = txRepo.savedTransactions.first()
        assertEquals("acc-target-card", saved.accountId)
        assertEquals("cat-food", saved.categoryId)
        assertEquals(45.0, saved.amount)
        assertEquals(customDate, saved.timestamp)
    }

    @Test
    fun testPrepareEditTransactionAndSubmitUpdatesTransactionAndReconciles() = runBlocking {
        val accountRepo = FakeTestAccountRepo()
        val catRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTxRepo()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val updateTxUseCase = UpdateTransactionUseCase(txRepo, accountRepo)

        val account1 = Account(
            id = "acc-1",
            name = "Cuenta 1",
            type = AccountType.SAVINGS,
            balance = 500.0
        )
        val account2 = Account(
            id = "acc-2",
            name = "Cuenta 2",
            type = AccountType.SAVINGS,
            balance = 300.0
        )
        accountRepo.insertAccount(account1)
        accountRepo.insertAccount(account2)

        val catFood = Category(
            id = "cat-food",
            name = "Comida",
            iconName = "restaurant",
            colorHex = "#EF4444",
            type = TransactionType.EXPENSE
        )
        catRepo.insertCategory(catFood)

        // Transacción inicial en acc-1 con monto 50
        val initialTx = Transaction(
            id = "tx-original",
            accountId = "acc-1",
            categoryId = "cat-food",
            type = TransactionType.EXPENSE,
            amount = 50.0,
            timestamp = 1000L,
            note = "Almuerzo"
        )
        createTxUseCase(initialTx)

        val viewModel = TransactionViewModel(
            accountRepository = accountRepo,
            categoryRepository = catRepo,
            transactionRepository = txRepo,
            createTransactionUseCase = createTxUseCase,
            updateTransactionUseCase = updateTxUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        // Cargar transacción existente en modo edición
        viewModel.prepareEditTransaction(initialTx)

        val editState = viewModel.uiState.first { it.isEditing }
        assertTrue(editState.isEditing)
        assertEquals("tx-original", editState.editingTransactionId)
        assertEquals("50", editState.amountString)
        assertEquals("acc-1", editState.selectedAccountId)
        assertEquals("cat-food", editState.selectedCategoryId)
        assertEquals("Almuerzo", editState.note)

        // Cambiar la cuenta de cargo a acc-2 y la nota
        viewModel.selectAccount("acc-2")
        viewModel.setNote("Almuerzo Reasignado")

        var successCallbackCalled = false
        viewModel.submitTransaction(onSuccess = { successCallbackCalled = true })

        assertTrue(successCallbackCalled)

        // Verificar que acc-1 restauró su saldo (500) y acc-2 debitó 50 (250)
        val updatedAcc1 = accountRepo.getAccountById("acc-1").first()
        val updatedAcc2 = accountRepo.getAccountById("acc-2").first()
        assertEquals(500.0, updatedAcc1?.balance)
        assertEquals(250.0, updatedAcc2?.balance)

        // Verificar que la transacción en el repositorio tiene accountId = acc-2
        val updatedTx = txRepo.getTransactionById("tx-original")
        assertEquals("acc-2", updatedTx?.accountId)
        assertEquals("Almuerzo Reasignado", updatedTx?.note)
    }
}
