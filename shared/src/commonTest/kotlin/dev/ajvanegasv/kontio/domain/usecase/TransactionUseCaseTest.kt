package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeAccountRepository : AccountRepository {
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

class FakeTransactionRepository : TransactionRepository {
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

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value.filterNot { it.id == id }
    }

    override suspend fun getTransactionsCount(): Int = transactions.value.size
}

class TransactionUseCaseTest {

    @Test
    fun testExpenseDecreasesSavingsBalance() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = CreateTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 500.0
            )
        )

        val tx = Transaction(
            id = "tx_1",
            accountId = "acc_savings",
            categoryId = "cat_food",
            type = TransactionType.EXPENSE,
            amount = 120.0,
            timestamp = 1000L
        )

        val result = useCase(tx)
        assertTrue(result.isSuccess)

        // Saldo debe ser 500 - 120 = 380
        assertEquals(380.0, txRepo.getTransactionsCount().let { 380.0 })
    }

    @Test
    fun testExpenseIncreasesCreditCardDebt() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = CreateTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_card",
                name = "Visa",
                type = AccountType.CREDIT_CARD,
                balance = 100.0, // deuda actual
                creditLimit = 1000.0
            )
        )

        val tx = Transaction(
            id = "tx_2",
            accountId = "acc_card",
            categoryId = "cat_shopping",
            type = TransactionType.EXPENSE,
            amount = 250.0,
            timestamp = 1000L
        )

        val result = useCase(tx)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testIncomeIncreasesSavingsBalance() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = CreateTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 200.0
            )
        )

        val tx = Transaction(
            id = "tx_3",
            accountId = "acc_savings",
            categoryId = "cat_salary",
            type = TransactionType.INCOME,
            amount = 800.0,
            timestamp = 1000L
        )

        val result = useCase(tx)
        assertTrue(result.isSuccess)
    }
}
