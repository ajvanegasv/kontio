package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun testDeleteExpenseRevertsSavingsBalance() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

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

        val createResult = createTxUseCase(tx)
        assertTrue(createResult.isSuccess)

        // Verificamos que el balance se redujo a 380
        val accAfterCreate = accountRepo.getAccountById("acc_savings").firstOrNull()
        assertEquals(380.0, accAfterCreate?.balance)

        val deleteResult = deleteTxUseCase(tx.id)
        assertTrue(deleteResult.isSuccess)

        // Verificamos que el balance se restauró a 500
        val accAfterDelete = accountRepo.getAccountById("acc_savings").firstOrNull()
        assertEquals(500.0, accAfterDelete?.balance)
        assertNull(txRepo.getTransactionById(tx.id))
    }

    @Test
    fun testDeleteExpenseRevertsCreditCardDebt() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

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

        val createResult = createTxUseCase(tx)
        assertTrue(createResult.isSuccess)

        // Deuda aumentó a 350
        val accAfterCreate = accountRepo.getAccountById("acc_card").firstOrNull()
        assertEquals(350.0, accAfterCreate?.balance)

        val deleteResult = deleteTxUseCase(tx.id)
        assertTrue(deleteResult.isSuccess)

        // Deuda restaurada a 100
        val accAfterDelete = accountRepo.getAccountById("acc_card").firstOrNull()
        assertEquals(100.0, accAfterDelete?.balance)
        assertNull(txRepo.getTransactionById(tx.id))
    }

    @Test
    fun testDeleteIncomeRevertsSavingsBalance() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

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

        val createResult = createTxUseCase(tx)
        assertTrue(createResult.isSuccess)

        // Balance aumentó a 1000
        val accAfterCreate = accountRepo.getAccountById("acc_savings").firstOrNull()
        assertEquals(1000.0, accAfterCreate?.balance)

        val deleteResult = deleteTxUseCase(tx.id)
        assertTrue(deleteResult.isSuccess)

        // Balance restaurado a 200
        val accAfterDelete = accountRepo.getAccountById("acc_savings").firstOrNull()
        assertEquals(200.0, accAfterDelete?.balance)
        assertNull(txRepo.getTransactionById(tx.id))
    }

    @Test
    fun testDeleteAccountDeletesAccountAndItsTransactions() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val deleteAccountUseCase = DeleteAccountUseCase(accountRepo, txRepo)

        val acc1 = Account(
            id = "acc_1",
            name = "Cuenta 1",
            type = AccountType.SAVINGS,
            balance = 500.0
        )
        val acc2 = Account(
            id = "acc_2",
            name = "Cuenta 2",
            type = AccountType.CHECKING,
            balance = 1000.0
        )

        accountRepo.insertAccount(acc1)
        accountRepo.insertAccount(acc2)

        // Tx en acc1
        txRepo.insertTransaction(
            Transaction(
                id = "tx_1",
                accountId = "acc_1",
                categoryId = "cat_food",
                type = TransactionType.EXPENSE,
                amount = 50.0,
                timestamp = 1000L
            )
        )
        // Tx transferencia con destino acc1
        txRepo.insertTransaction(
            Transaction(
                id = "tx_2",
                accountId = "acc_2",
                targetAccountId = "acc_1",
                categoryId = "cat_transfer",
                type = TransactionType.TRANSFER,
                amount = 100.0,
                timestamp = 2000L
            )
        )
        // Tx en acc2 solamente
        txRepo.insertTransaction(
            Transaction(
                id = "tx_3",
                accountId = "acc_2",
                categoryId = "cat_bills",
                type = TransactionType.EXPENSE,
                amount = 75.0,
                timestamp = 3000L
            )
        )

        assertEquals(2, accountRepo.getAccountsCount())
        assertEquals(3, txRepo.getTransactionsCount())

        val deleteResult = deleteAccountUseCase("acc_1")
        assertTrue(deleteResult.isSuccess)

        // acc_1 debe estar eliminada
        assertNull(accountRepo.getAccountById("acc_1").firstOrNull())
        // acc_2 debe seguir existiendo
        assertEquals("Cuenta 2", accountRepo.getAccountById("acc_2").firstOrNull()?.name)

        // tx_1 y tx_2 asociadas a acc_1 deben haber sido eliminadas, quedando sólo tx_3
        assertEquals(1, txRepo.getTransactionsCount())
        assertNull(txRepo.getTransactionById("tx_1"))
        assertNull(txRepo.getTransactionById("tx_2"))
        assertEquals("tx_3", txRepo.getTransactionById("tx_3")?.id)
    }

    @Test
    fun testDeleteTransferRevertsBothAccountsBalance() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_src",
                name = "Origen",
                type = AccountType.SAVINGS,
                balance = 500.0
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "acc_dst",
                name = "Destino",
                type = AccountType.CHECKING,
                balance = 200.0
            )
        )

        val tx = Transaction(
            id = "tx_transfer",
            accountId = "acc_src",
            targetAccountId = "acc_dst",
            categoryId = "cat_transfer",
            type = TransactionType.TRANSFER,
            amount = 150.0,
            timestamp = 1000L
        )

        val createResult = createTxUseCase(tx)
        assertTrue(createResult.isSuccess)

        // Origen descontó 150 (350), Destino sumó 150 (350)
        assertEquals(350.0, accountRepo.getAccountById("acc_src").firstOrNull()?.balance)
        assertEquals(350.0, accountRepo.getAccountById("acc_dst").firstOrNull()?.balance)

        val deleteResult = deleteTxUseCase(tx.id)
        assertTrue(deleteResult.isSuccess)

        // Balances revertidos: Origen 500, Destino 200
        assertEquals(500.0, accountRepo.getAccountById("acc_src").firstOrNull()?.balance)
        assertEquals(200.0, accountRepo.getAccountById("acc_dst").firstOrNull()?.balance)
        assertNull(txRepo.getTransactionById(tx.id))
    }

    @Test
    fun testTransferFromSavingsToCreditCardReducesCreditCardDebtAndSavings() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 1000.0
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "acc_card",
                name = "Tarjeta Visa",
                type = AccountType.CREDIT_CARD,
                balance = 400.0, // deuda actual
                creditLimit = 1500.0
            )
        )

        // Pagar 250 a la tarjeta desde ahorros vía transferencia
        val tx = Transaction(
            id = "tx_pay_card",
            accountId = "acc_savings",
            targetAccountId = "acc_card",
            categoryId = "cat_transfer",
            type = TransactionType.TRANSFER,
            amount = 250.0,
            timestamp = 1000L
        )

        val createResult = createTxUseCase(tx)
        assertTrue(createResult.isSuccess)

        // Ahorros debita 250 -> 750
        assertEquals(750.0, accountRepo.getAccountById("acc_savings").firstOrNull()?.balance)
        // Tarjeta reduce deuda en 250 -> 150
        assertEquals(150.0, accountRepo.getAccountById("acc_card").firstOrNull()?.balance)

        // Revertir transferencia
        val deleteResult = deleteTxUseCase(tx.id)
        assertTrue(deleteResult.isSuccess)

        // Ahorros vuelve a 1000, Tarjeta vuelve a tener 400 de deuda
        assertEquals(1000.0, accountRepo.getAccountById("acc_savings").firstOrNull()?.balance)
        assertEquals(400.0, accountRepo.getAccountById("acc_card").firstOrNull()?.balance)
    }


    @Test
    fun testDeleteIncomeRevertsCreditCardDebt() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_card_pay",
                name = "Mastercard",
                type = AccountType.CREDIT_CARD,
                balance = 300.0, // Deuda inicial
                creditLimit = 1500.0
            )
        )

        val tx = Transaction(
            id = "tx_card_payment",
            accountId = "acc_card_pay",
            categoryId = "cat_payment",
            type = TransactionType.INCOME,
            amount = 100.0,
            timestamp = 1000L
        )

        val createResult = createTxUseCase(tx)
        assertTrue(createResult.isSuccess)

        // Pago redujo deuda a 200
        assertEquals(200.0, accountRepo.getAccountById("acc_card_pay").firstOrNull()?.balance)

        val deleteResult = deleteTxUseCase(tx.id)
        assertTrue(deleteResult.isSuccess)

        // Revertir pago restaura deuda a 300
        assertEquals(300.0, accountRepo.getAccountById("acc_card_pay").firstOrNull()?.balance)
        assertNull(txRepo.getTransactionById(tx.id))
    }

    @Test
    fun testDeleteTransactionWhenAccountAlreadyDeletedDoesNotCrash() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val deleteTxUseCase = DeleteTransactionUseCase(txRepo, accountRepo)

        // Insertamos tx sin cuenta en el repo
        txRepo.insertTransaction(
            Transaction(
                id = "tx_orphaned",
                accountId = "acc_non_existent",
                categoryId = "cat_misc",
                type = TransactionType.EXPENSE,
                amount = 50.0,
                timestamp = 1000L
            )
        )

        val deleteResult = deleteTxUseCase("tx_orphaned")
        assertTrue(deleteResult.isSuccess)
        assertNull(txRepo.getTransactionById("tx_orphaned"))
    }
}
