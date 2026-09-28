package dev.ajvanegasv.kontio.presentation.accounts

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.ReassignTransactionsAccountUseCase
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

private class FakeAccountRepository : AccountRepository {
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

private class FakeTransactionRepository : TransactionRepository {
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

class AccountsViewModelTest {

    @Test
    fun testSelectAccountForDetailAndFiltersTransactions() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createAccUseCase = CreateAccountUseCase(accountRepo)
        val deleteAccUseCase = DeleteAccountUseCase(accountRepo, txRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        val acc1 = Account(
            id = "card-visa",
            name = "Tarjeta Visa",
            type = AccountType.CREDIT_CARD,
            balance = 250.0,
            currency = "USD",
            colorHex = "#1E293B",
            iconName = "credit_card",
            creditLimit = 1000.0,
            cutoffDay = 15,
            dueDay = 5
        )
        val acc2 = Account(
            id = "acc-savings",
            name = "Ahorros",
            type = AccountType.SAVINGS,
            balance = 1200.0,
            currency = "USD",
            colorHex = "#10B981",
            iconName = "account_balance"
        )
        accountRepo.insertAccount(acc1)
        accountRepo.insertAccount(acc2)

        val txVisa1 = Transaction(
            id = "tx-1",
            accountId = "card-visa",
            categoryId = "cat-food",
            type = TransactionType.EXPENSE,
            amount = 50.0,
            timestamp = 1000L,
            note = "Cena"
        )
        val txVisa2 = Transaction(
            id = "tx-2",
            accountId = "card-visa",
            categoryId = "cat-uber",
            type = TransactionType.EXPENSE,
            amount = 20.0,
            timestamp = 2000L,
            note = "Uber"
        )
        val txSavings = Transaction(
            id = "tx-3",
            accountId = "acc-savings",
            categoryId = "cat-salary",
            type = TransactionType.INCOME,
            amount = 1200.0,
            timestamp = 1500L,
            note = "Salario"
        )

        txRepo.insertTransaction(txVisa1)
        txRepo.insertTransaction(txVisa2)
        txRepo.insertTransaction(txSavings)

        val viewModel = AccountsViewModel(
            accountRepository = accountRepo,
            transactionRepository = txRepo,
            createAccountUseCase = createAccUseCase,
            deleteAccountUseCase = deleteAccUseCase,
            deleteTransactionUseCase = deleteTxUseCase,
            reassignTransactionsAccountUseCase = ReassignTransactionsAccountUseCase(txRepo, accountRepo),
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        // Inicialmente sin detalle seleccionado
        val initialState = viewModel.uiState.first { it.accounts.size == 2 }
        assertNull(initialState.selectedAccountId)
        assertTrue(initialState.selectedAccountTransactions.isEmpty())

        // Seleccionar Tarjeta Visa
        viewModel.selectAccountForDetail("card-visa")
        val detailState = viewModel.uiState.first { it.selectedAccountId == "card-visa" }
        assertEquals("card-visa", detailState.selectedAccountId)
        assertEquals("Tarjeta Visa", detailState.selectedAccount?.name)
        assertEquals(2, detailState.selectedAccountTransactions.size)
        assertTrue(detailState.selectedAccountTransactions.all { it.accountId == "card-visa" })

        // Seleccionar Ahorros
        viewModel.selectAccountForDetail("acc-savings")
        val savingsDetailState = viewModel.uiState.first { it.selectedAccountId == "acc-savings" }
        assertEquals("acc-savings", savingsDetailState.selectedAccountId)
        assertEquals(1, savingsDetailState.selectedAccountTransactions.size)
        assertEquals("tx-3", savingsDetailState.selectedAccountTransactions.first().id)

        // Cerrar detalle
        viewModel.selectAccountForDetail(null)
        val closedState = viewModel.uiState.first { it.selectedAccountId == null }
        assertNull(closedState.selectedAccountId)
        assertTrue(closedState.selectedAccountTransactions.isEmpty())
    }

    @Test
    fun testReassignAccountTransactionsUpdatesBothBalancesAndReassigns() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createAccUseCase = CreateAccountUseCase(accountRepo)
        val deleteAccUseCase = DeleteAccountUseCase(accountRepo, txRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)
        val reassignUseCase = ReassignTransactionsAccountUseCase(txRepo, accountRepo)

        val acc1 = Account(
            id = "acc-wrong",
            name = "Cuenta Errónea",
            type = AccountType.SAVINGS,
            balance = 800.0,
            currency = "USD"
        )
        val acc2 = Account(
            id = "acc-correct",
            name = "Cuenta Correcta",
            type = AccountType.SAVINGS,
            balance = 500.0,
            currency = "USD"
        )
        accountRepo.insertAccount(acc1)
        accountRepo.insertAccount(acc2)

        val tx1 = Transaction(
            id = "tx-1",
            accountId = "acc-wrong",
            categoryId = "cat-food",
            type = TransactionType.EXPENSE,
            amount = 100.0,
            timestamp = 1000L
        )
        val tx2 = Transaction(
            id = "tx-2",
            accountId = "acc-wrong",
            categoryId = "cat-transport",
            type = TransactionType.EXPENSE,
            amount = 100.0,
            timestamp = 2000L
        )
        txRepo.insertTransaction(tx1)
        txRepo.insertTransaction(tx2)

        val viewModel = AccountsViewModel(
            accountRepository = accountRepo,
            transactionRepository = txRepo,
            createAccountUseCase = createAccUseCase,
            deleteAccountUseCase = deleteAccUseCase,
            deleteTransactionUseCase = deleteTxUseCase,
            reassignTransactionsAccountUseCase = reassignUseCase,
            coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        )

        var completedCount = 0
        viewModel.reassignAccountTransactions(
            fromAccountId = "acc-wrong",
            toAccountId = "acc-correct",
            onComplete = { result ->
                completedCount = result.getOrDefault(0)
            }
        )

        assertEquals(2, completedCount)

        // Verificar que acc-wrong restauró su balance (800 + 200 = 1000)
        val updatedAcc1 = accountRepo.getAccountById("acc-wrong").first()
        assertEquals(1000.0, updatedAcc1?.balance)

        // Verificar que acc-correct debitó (500 - 200 = 300)
        val updatedAcc2 = accountRepo.getAccountById("acc-correct").first()
        assertEquals(300.0, updatedAcc2?.balance)

        // Verificar que las transacciones ahora pertenecen a acc-correct
        val updatedTx1 = txRepo.getTransactionById("tx-1")
        val updatedTx2 = txRepo.getTransactionById("tx-2")
        assertEquals("acc-correct", updatedTx1?.accountId)
        assertEquals("acc-correct", updatedTx2?.accountId)
    }
}
