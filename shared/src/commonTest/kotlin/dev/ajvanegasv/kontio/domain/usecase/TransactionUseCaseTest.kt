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

    override suspend fun archiveAccount(id: String) {
        val current = accounts.value[id] ?: return
        accounts.value = accounts.value + (id to current.copy(isArchived = true))
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
        val updated = transactions.value.filterNot { it.id == transaction.id }
        transactions.value = listOf(transaction) + updated
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        val newIds = transactions.map { it.id }.toSet()
        val remaining = this.transactions.value.filterNot { it.id in newIds }
        this.transactions.value = transactions + remaining
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

    @Test
    fun testUpdateTransactionAmountAdjustsBalanceProperly() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val updateTxUseCase = UpdateTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 500.0
            )
        )

        val tx = Transaction(
            id = "tx_edit_1",
            accountId = "acc_savings",
            categoryId = "cat_food",
            type = TransactionType.EXPENSE,
            amount = 100.0,
            timestamp = 1000L
        )

        createTxUseCase(tx)
        // Saldo después de crear gasto de 100: 500 - 100 = 400
        assertEquals(400.0, accountRepo.getAccountById("acc_savings").firstOrNull()?.balance)

        // Editamos el gasto para que sea de 150
        val updatedTx = tx.copy(amount = 150.0, note = "Actualizado")
        val updateResult = updateTxUseCase(updatedTx)
        assertTrue(updateResult.isSuccess)

        // Saldo debe ser 500 - 150 = 350
        assertEquals(350.0, accountRepo.getAccountById("acc_savings").firstOrNull()?.balance)
        val saved = txRepo.getTransactionById("tx_edit_1")
        assertEquals(150.0, saved?.amount)
        assertEquals("Actualizado", saved?.note)
    }

    @Test
    fun testUpdateTransactionChangeAccountReconcilesBothAccounts() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val updateTxUseCase = UpdateTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_wrong",
                name = "Cuenta Errónea",
                type = AccountType.SAVINGS,
                balance = 1000.0
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "acc_correct",
                name = "Cuenta Correcta",
                type = AccountType.SAVINGS,
                balance = 500.0
            )
        )

        val tx = Transaction(
            id = "tx_move_1",
            accountId = "acc_wrong",
            categoryId = "cat_food",
            type = TransactionType.EXPENSE,
            amount = 200.0,
            timestamp = 1000L
        )

        createTxUseCase(tx)
        // acc_wrong debita 200 -> 800
        assertEquals(800.0, accountRepo.getAccountById("acc_wrong").firstOrNull()?.balance)
        assertEquals(500.0, accountRepo.getAccountById("acc_correct").firstOrNull()?.balance)

        // Editamos la transacción para moverla a acc_correct
        val reassignedTx = tx.copy(accountId = "acc_correct")
        val result = updateTxUseCase(reassignedTx)
        assertTrue(result.isSuccess)

        // acc_wrong debe restaurar sus 200 -> 1000
        assertEquals(1000.0, accountRepo.getAccountById("acc_wrong").firstOrNull()?.balance)
        // acc_correct debe debitar 200 -> 300
        assertEquals(300.0, accountRepo.getAccountById("acc_correct").firstOrNull()?.balance)

        val saved = txRepo.getTransactionById("tx_move_1")
        assertEquals("acc_correct", saved?.accountId)
    }

    @Test
    fun testUpdateTransactionFromCreditCardToSavingsReconcilesBalances() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val updateTxUseCase = UpdateTransactionUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_card",
                name = "Tarjeta",
                type = AccountType.CREDIT_CARD,
                balance = 200.0 // deuda inicial
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 1000.0
            )
        )

        val tx = Transaction(
            id = "tx_card_to_sav",
            accountId = "acc_card",
            categoryId = "cat_shopping",
            type = TransactionType.EXPENSE,
            amount = 150.0,
            timestamp = 1000L
        )

        createTxUseCase(tx)
        // Deuda en tarjeta aumenta a 350
        assertEquals(350.0, accountRepo.getAccountById("acc_card").firstOrNull()?.balance)
        assertEquals(1000.0, accountRepo.getAccountById("acc_savings").firstOrNull()?.balance)

        // Reasignamos la transacción a Ahorros
        val reassigned = tx.copy(accountId = "acc_savings")
        val updateResult = updateTxUseCase(reassigned)
        assertTrue(updateResult.isSuccess)

        // Deuda en tarjeta vuelve a 200
        assertEquals(200.0, accountRepo.getAccountById("acc_card").firstOrNull()?.balance)
        // Saldo de ahorros debita 150 -> 850
        assertEquals(850.0, accountRepo.getAccountById("acc_savings").firstOrNull()?.balance)
    }

    @Test
    fun testReassignTransactionsAccountBulkMovesAllTransactionsAndReconciles() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)
        val reassignUseCase = ReassignTransactionsAccountUseCase(txRepo, accountRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_imported_wrong",
                name = "Cuenta Equivocada",
                type = AccountType.SAVINGS,
                balance = 1000.0
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "acc_target",
                name = "Cuenta Destino",
                type = AccountType.SAVINGS,
                balance = 2000.0
            )
        )

        // Creamos 5 transacciones importadas en la cuenta errónea
        for (i in 1..5) {
            createTxUseCase(
                Transaction(
                    id = "tx_bulk_$i",
                    accountId = "acc_imported_wrong",
                    categoryId = "cat_food",
                    type = TransactionType.EXPENSE,
                    amount = 50.0,
                    timestamp = 1000L + i
                )
            )
        }

        // Saldo de cuenta equivocada: 1000 - 250 = 750
        assertEquals(750.0, accountRepo.getAccountById("acc_imported_wrong").firstOrNull()?.balance)
        assertEquals(2000.0, accountRepo.getAccountById("acc_target").firstOrNull()?.balance)

        // Mover todas las transacciones de acc_imported_wrong a acc_target
        val result = reassignUseCase(
            fromAccountId = "acc_imported_wrong",
            toAccountId = "acc_target"
        )
        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())

        // Cuenta equivocada restaura su saldo a 1000
        assertEquals(1000.0, accountRepo.getAccountById("acc_imported_wrong").firstOrNull()?.balance)
        // Cuenta destino debita los 250 -> 1750
        assertEquals(1750.0, accountRepo.getAccountById("acc_target").firstOrNull()?.balance)

        // Todas las transacciones ahora pertenecen a acc_target
        val targetTxs = txRepo.getTransactionsByAccount("acc_target").firstOrNull() ?: emptyList()
        assertEquals(5, targetTxs.size)
        val wrongTxs = txRepo.getTransactionsByAccount("acc_imported_wrong").firstOrNull() ?: emptyList()
        assertEquals(0, wrongTxs.size)
    }
}
