package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.ParsedStatementItem
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BatchImportTransactionsUseCaseTest {

    private class TestAccountRepository : AccountRepository {
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

    private class TestTransactionRepository : TransactionRepository {
        val inserted = mutableListOf<Transaction>()

        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = MutableStateFlow(inserted)
        override fun getAllTransactions(): Flow<List<Transaction>> = MutableStateFlow(inserted)
        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
            MutableStateFlow(inserted.filter { it.accountId == accountId })
        override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
            MutableStateFlow(inserted.filter { it.timestamp in startDate..endDate })
        override suspend fun insertTransaction(transaction: Transaction) {
            inserted.add(transaction)
        }
        override suspend fun insertTransactions(transactions: List<Transaction>) {
            inserted.addAll(transactions)
        }
        override suspend fun deleteTransaction(id: String) {
            inserted.removeAll { it.id == id }
        }
        override suspend fun getTransactionById(id: String): Transaction? =
            inserted.firstOrNull { it.id == id }
        override suspend fun deleteTransactionsByAccountId(accountId: String) {
            inserted.removeAll { it.accountId == accountId }
        }
        override suspend fun getTransactionsCount(): Int = inserted.size
        override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
            inserted.count { it.categoryId == categoryId }
    }

    @Test
    fun testBatchImportSavingsAccountCalculatesBalanceCorrectly() = runBlocking {
        val accountRepo = TestAccountRepository()
        val txRepo = TestTransactionRepository()
        val useCase = BatchImportTransactionsUseCase(txRepo, accountRepo)

        val account = Account(
            id = "acc_savings_1",
            name = "Ahorros Bancolombia",
            type = AccountType.SAVINGS,
            balance = 1000.0,
            currency = "COP"
        )
        accountRepo.insertAccount(account)

        val items = listOf(
            ParsedStatementItem(
                id = "item_1",
                date = 1773532800000L,
                originalDescription = "COMPRA RESTAURANTE XYZ",
                cleanTitle = "Restaurante XYZ",
                amount = 200.0,
                type = TransactionType.EXPENSE,
                suggestedCategoryId = "cat_food",
                confidenceScore = 0.95f,
                isSelected = true
            ),
            ParsedStatementItem(
                id = "item_2",
                date = 1773532800000L,
                originalDescription = "GASOLINERA PRIMAX",
                cleanTitle = "Primax",
                amount = 50.0,
                type = TransactionType.EXPENSE,
                suggestedCategoryId = "cat_transport",
                confidenceScore = 0.90f,
                isSelected = true
            ),
            ParsedStatementItem(
                id = "item_3",
                date = 1773532800000L,
                originalDescription = "TRANSFERENCIA NOMINA",
                cleanTitle = "Nómina Empresa",
                amount = 500.0,
                type = TransactionType.INCOME,
                suggestedCategoryId = "cat_salary",
                confidenceScore = 0.98f,
                isSelected = true
            ),
            ParsedStatementItem(
                id = "item_4",
                date = 1773532800000L,
                originalDescription = "DUPLICADO OMITIDO",
                cleanTitle = "Omitido",
                amount = 100.0,
                type = TransactionType.EXPENSE,
                suggestedCategoryId = "cat_other_exp",
                isSelected = false // Usuario lo desmarcó
            )
        )

        val result = useCase(
            accountId = "acc_savings_1",
            items = items,
            detectedBankName = "Bancolombia"
        )

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())

        // 1000.0 + 500.0 (ingreso) - 250.0 (gastos seleccionados) = 1250.0
        val updatedAccount = accountRepo.getAccountById("acc_savings_1").firstOrNull()
        assertNotNull(updatedAccount)
        assertEquals(1250.0, updatedAccount.balance)

        // Verificar inserciones y metadatos de IA
        assertEquals(3, txRepo.inserted.size)
        val firstTx = txRepo.inserted.first()
        assertEquals("acc_savings_1", firstTx.accountId)
        assertNotNull(firstTx.aiMetadata)
        assertTrue(firstTx.aiMetadata!!.isAutoGenerated)
        assertEquals("cat_food", firstTx.aiMetadata!!.inferredCategory)
        assertEquals("Bancolombia", firstTx.aiMetadata!!.extractedEntities["detectedBank"])
    }

    @Test
    fun testBatchImportCreditCardIncreasesDebtOnExpenseAndReducesOnIncome() = runBlocking {
        val accountRepo = TestAccountRepository()
        val txRepo = TestTransactionRepository()
        val useCase = BatchImportTransactionsUseCase(txRepo, accountRepo)

        val ccAccount = Account(
            id = "acc_cc_1",
            name = "Tarjeta Nu",
            type = AccountType.CREDIT_CARD,
            balance = 300.0, // Deuda inicial
            creditLimit = 2000.0,
            currency = "USD"
        )
        accountRepo.insertAccount(ccAccount)

        val items = listOf(
            ParsedStatementItem(
                id = "item_1",
                date = 1773532800000L,
                originalDescription = "AMAZON MARKETPLACE",
                cleanTitle = "Amazon",
                amount = 150.0,
                type = TransactionType.EXPENSE,
                suggestedCategoryId = "cat_shopping",
                isSelected = true
            ),
            ParsedStatementItem(
                id = "item_2",
                date = 1773532800000L,
                originalDescription = "SU PAGO EN EFECTIVO",
                cleanTitle = "Abono Tarjeta",
                amount = 200.0,
                type = TransactionType.INCOME,
                suggestedCategoryId = "cat_other_inc",
                isSelected = true
            )
        )

        val result = useCase("acc_cc_1", items, "Nu")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        // En tarjeta de crédito: 300.0 (deuda inicial) + 150.0 (compra) - 200.0 (abono) = 250.0
        val updatedCc = accountRepo.getAccountById("acc_cc_1").firstOrNull()
        assertNotNull(updatedCc)
        assertEquals(250.0, updatedCc.balance)
    }

    @Test
    fun testBatchImportEmptySelectionReturnsZero() = runBlocking {
        val accountRepo = TestAccountRepository()
        val txRepo = TestTransactionRepository()
        val useCase = BatchImportTransactionsUseCase(txRepo, accountRepo)

        val account = Account(
            id = "acc_1",
            name = "Cuenta",
            type = AccountType.SAVINGS,
            balance = 100.0
        )
        accountRepo.insertAccount(account)

        val items = listOf(
            ParsedStatementItem(
                id = "item_1",
                date = 1000L,
                originalDescription = "Gasto",
                cleanTitle = "Gasto",
                amount = 50.0,
                type = TransactionType.EXPENSE,
                suggestedCategoryId = null,
                isSelected = false
            )
        )

        val result = useCase("acc_1", items)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals(0, txRepo.inserted.size)
        assertEquals(100.0, accountRepo.getAccountById("acc_1").firstOrNull()?.balance)
    }
}
