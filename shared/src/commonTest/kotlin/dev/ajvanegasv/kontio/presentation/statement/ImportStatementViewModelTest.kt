package dev.ajvanegasv.kontio.presentation.statement

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiStatementParser
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.ParsedStatementItem
import dev.ajvanegasv.kontio.domain.model.ParsedStatementResult
import dev.ajvanegasv.kontio.domain.model.StatementFile
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.AnalyzeBankStatementUseCase
import dev.ajvanegasv.kontio.domain.usecase.BatchImportTransactionsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeAiConfigStorageForImport : AiConfigStorage {
    private var key: String? = "test-api-key"
    private var currentModel: String = "gemini-3.8-flash"
    private var enabled: Boolean = true

    override val apiKeyFlow: Flow<String?> = flowOf(key)
    override fun getApiKey(): String? = key
    override fun setApiKey(apiKey: String) { this.key = apiKey }
    override fun clearApiKey() { this.key = null }

    override val modelFlow: Flow<String> = flowOf(currentModel)
    override fun getModel(): String = currentModel
    override fun setModel(model: String) { this.currentModel = model }

    override val isAiEnabledFlow: Flow<Boolean> = flowOf(enabled)
    override fun isAiEnabled(): Boolean = enabled
    override fun setAiEnabled(enabled: Boolean) { this.enabled = enabled }
}

private class FakeAccountRepoForImport : AccountRepository {
    val accounts = MutableStateFlow<List<Account>>(emptyList())

    override fun getAccounts(): Flow<List<Account>> = accounts.map { list -> list.filterNot { it.isArchived } }
    override fun getAccountById(id: String): Flow<Account?> = accounts.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun insertAccount(account: Account) { accounts.value = accounts.value + account }
    override suspend fun updateAccount(account: Account) {}
    override suspend fun updateBalance(accountId: String, newBalance: Double) {}
    override suspend fun archiveAccount(id: String) {}
    override suspend fun deleteAccount(id: String) {}
    override suspend fun getAccountsCount(): Int = accounts.value.size
}

private class FakeCategoryRepoForImport : CategoryRepository {
    override fun getCategories(type: TransactionType?): Flow<List<Category>> = flowOf(emptyList())
    override fun getCategoryById(id: String): Flow<Category?> = flowOf(null)
    override suspend fun insertCategory(category: Category) {}
    override suspend fun deleteCategory(id: String) {}
    override suspend fun seedDefaultCategoriesIfEmpty() {}
    override suspend fun getCategoriesCount(): Int = 0
}

private class FakeTxRepoForImport : TransactionRepository {
    val txs = mutableListOf<Transaction>()
    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(txs)
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs)
    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> = flowOf(txs.filter { it.accountId == accountId })
    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(txs)
    override suspend fun insertTransaction(transaction: Transaction) { txs.add(transaction) }
    override suspend fun insertTransactions(transactions: List<Transaction>) { txs.addAll(transactions) }
    override suspend fun deleteTransaction(id: String) {}
    override suspend fun getTransactionById(id: String): Transaction? = txs.firstOrNull { it.id == id }
    override suspend fun deleteTransactionsByAccountId(accountId: String) {}
    override suspend fun getTransactionsCount(): Int = txs.size
    override suspend fun getTransactionsCountByCategory(categoryId: String): Int = 0
}

private class MockGeminiStatementParser(
    private val resultToReturn: ParsedStatementResult
) : GeminiStatementParser() {
    override suspend fun parseStatement(
        file: StatementFile,
        apiKey: String,
        categories: List<Category>,
        accounts: List<Account>,
        model: String
    ): Result<ParsedStatementResult> = Result.success(resultToReturn)
}

class ImportStatementViewModelTest {

    @Test
    fun testManualAccountSelectionSetsFlagAndPreservesAccountOnAnalysis() = runBlocking {
        val accountRepo = FakeAccountRepoForImport()
        val acc1 = Account(id = "acc_1", name = "Ahorros Bancolombia", type = AccountType.SAVINGS, balance = 100.0, currency = "USD")
        val acc2 = Account(id = "acc_custom", name = "Cuenta Manual", type = AccountType.CHECKING, balance = 200.0, currency = "USD")
        accountRepo.accounts.value = listOf(acc1, acc2)

        val parserResult = ParsedStatementResult(
            detectedBankName = "Bancolombia",
            detectedAccountNumber = null,
            suggestedAccountId = "acc_1", // AI suggests acc_1
            currency = "USD",
            items = listOf(
                ParsedStatementItem(
                    id = "item_1",
                    date = 1000L,
                    originalDescription = "Compra",
                    cleanTitle = "Compra",
                    amount = 50.0,
                    type = TransactionType.EXPENSE,
                    suggestedCategoryId = null
                )
            )
        )

        val aiConfig = FakeAiConfigStorageForImport()
        val analyzeUseCase = AnalyzeBankStatementUseCase(
            geminiStatementParser = MockGeminiStatementParser(parserResult),
            categoryRepository = FakeCategoryRepoForImport(),
            accountRepository = accountRepo,
            transactionRepository = FakeTxRepoForImport(),
            aiConfigStorage = aiConfig
        )

        val viewModel = ImportStatementViewModel(
            analyzeBankStatementUseCase = analyzeUseCase,
            batchImportTransactionsUseCase = BatchImportTransactionsUseCase(FakeTxRepoForImport(), accountRepo),
            aiConfigStorage = aiConfig,
            accountRepository = accountRepo,
            categoryRepository = FakeCategoryRepoForImport(),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // 1. Manually select acc_custom
        assertFalse(viewModel.uiState.value.isAccountManuallySelected)
        viewModel.selectAccount("acc_custom")
        assertTrue(viewModel.uiState.value.isAccountManuallySelected)
        assertEquals("acc_custom", viewModel.uiState.value.selectedAccountId)

        // 2. Select file and analyze
        val file = StatementFile("extracto.csv", "text/csv", "dummy".encodeToByteArray())
        viewModel.onFileSelected(file)
        viewModel.analyzeStatement()

        // 3. Even though AI suggested acc_1, manual selection acc_custom must be preserved!
        assertEquals(ImportStage.REVIEWING, viewModel.uiState.value.stage)
        assertEquals("acc_custom", viewModel.uiState.value.selectedAccountId)
    }

    @Test
    fun testAutomaticAccountSelectionAdoptsSuggestedAccountWhenNotManuallySelected() = runBlocking {
        val accountRepo = FakeAccountRepoForImport()
        val acc1 = Account(id = "acc_1", name = "Ahorros Bancolombia", type = AccountType.SAVINGS, balance = 100.0, currency = "USD")
        val acc2 = Account(id = "acc_2", name = "Tarjeta Davivienda", type = AccountType.CREDIT_CARD, balance = 200.0, currency = "USD")
        accountRepo.accounts.value = listOf(acc1, acc2)

        val parserResult = ParsedStatementResult(
            detectedBankName = "Davivienda",
            detectedAccountNumber = null,
            suggestedAccountId = "acc_2", // AI suggests acc_2
            currency = "USD",
            items = emptyList()
        )

        val aiConfig = FakeAiConfigStorageForImport()
        val analyzeUseCase = AnalyzeBankStatementUseCase(
            geminiStatementParser = MockGeminiStatementParser(parserResult),
            categoryRepository = FakeCategoryRepoForImport(),
            accountRepository = accountRepo,
            transactionRepository = FakeTxRepoForImport(),
            aiConfigStorage = aiConfig
        )

        val viewModel = ImportStatementViewModel(
            analyzeBankStatementUseCase = analyzeUseCase,
            batchImportTransactionsUseCase = BatchImportTransactionsUseCase(FakeTxRepoForImport(), accountRepo),
            aiConfigStorage = aiConfig,
            accountRepository = accountRepo,
            categoryRepository = FakeCategoryRepoForImport(),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // No manual selection performed
        assertFalse(viewModel.uiState.value.isAccountManuallySelected)

        val file = StatementFile("extracto.csv", "text/csv", "dummy".encodeToByteArray())
        viewModel.onFileSelected(file)
        viewModel.analyzeStatement()

        // Should adopt suggested account from analysis
        assertEquals("acc_2", viewModel.uiState.value.selectedAccountId)
    }

    @Test
    fun testResetClearsManualSelectionFlag() = runBlocking {
        val aiConfig = FakeAiConfigStorageForImport()
        val accountRepo = FakeAccountRepoForImport()
        val viewModel = ImportStatementViewModel(
            analyzeBankStatementUseCase = AnalyzeBankStatementUseCase(
                GeminiStatementParser(),
                FakeCategoryRepoForImport(),
                accountRepo,
                FakeTxRepoForImport(),
                aiConfig
            ),
            batchImportTransactionsUseCase = BatchImportTransactionsUseCase(FakeTxRepoForImport(), accountRepo),
            aiConfigStorage = aiConfig,
            accountRepository = accountRepo,
            categoryRepository = FakeCategoryRepoForImport(),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.selectAccount("acc_test")
        assertTrue(viewModel.uiState.value.isAccountManuallySelected)

        viewModel.reset()
        assertFalse(viewModel.uiState.value.isAccountManuallySelected)
        assertEquals(ImportStage.FILE_SELECTION, viewModel.uiState.value.stage)
    }
}
