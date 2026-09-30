package dev.ajvanegasv.kontio.presentation.transactions.voice

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.ParseVoiceTransactionUseCase
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoiceTransactionViewModelTest {

    private class FakeAccountRepo(accountsList: List<Account>) : AccountRepository {
        private val accounts = MutableStateFlow(accountsList)
        override fun getAccounts(): Flow<List<Account>> = accounts
        override fun getAccountById(id: String): Flow<Account?> = accounts.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun insertAccount(account: Account) {}
        override suspend fun updateAccount(account: Account) {}
        override suspend fun updateBalance(accountId: String, newBalance: Double) {}
        override suspend fun archiveAccount(id: String) {}
        override suspend fun deleteAccount(id: String) {}
        override suspend fun getAccountsCount(): Int = accounts.value.size
    }

    private class FakeCategoryRepo(categoriesList: List<Category>) : CategoryRepository {
        private val categories = MutableStateFlow(categoriesList)
        override fun getCategories(type: TransactionType?): Flow<List<Category>> =
            categories.map { list -> if (type == null) list else list.filter { it.type == type } }
        override fun getCategoryById(id: String): Flow<Category?> =
            categories.map { list -> list.firstOrNull { it.id == id } }
        override suspend fun insertCategory(category: Category) {}
        override suspend fun deleteCategory(id: String) {}
        override suspend fun seedDefaultCategoriesIfEmpty() {}
        override suspend fun getCategoriesCount(): Int = categories.value.size
    }

    private class FakeTransactionRepo : TransactionRepository {
        val insertedTransactions = mutableListOf<Transaction>()
        override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> = flowOf(insertedTransactions)
        override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(insertedTransactions)
        override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> = flowOf(insertedTransactions)
        override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> = flowOf(insertedTransactions)
        override suspend fun insertTransaction(transaction: Transaction) {
            insertedTransactions.add(transaction)
        }
        override suspend fun insertTransactions(transactions: List<Transaction>) {
            insertedTransactions.addAll(transactions)
        }
        override suspend fun deleteTransaction(id: String) {}
        override suspend fun deleteTransactionsByAccountId(accountId: String) {}
        override suspend fun getTransactionsCount(): Int = insertedTransactions.size
        override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
            insertedTransactions.count { it.categoryId == categoryId }
        override suspend fun getTransactionById(id: String): Transaction? = insertedTransactions.firstOrNull { it.id == id }
    }

    private class FakeAiConfigStorage : AiConfigStorage {
        override val apiKeyFlow: Flow<String?> = flowOf(null)
        override fun getApiKey(): String? = null
        override fun setApiKey(apiKey: String) {}
        override fun clearApiKey() {}
        override val modelFlow: Flow<String> = flowOf("gemini-3.8-flash")
        override fun getModel(): String = "gemini-3.8-flash"
        override fun setModel(model: String) {}
        override val isAiEnabledFlow: Flow<Boolean> = flowOf(false)
        override fun isAiEnabled(): Boolean = false
        override fun setAiEnabled(enabled: Boolean) {}
    }

    private val sampleAccounts = listOf(
        Account(id = "acc_cash", name = "Efectivo", type = AccountType.CASH, balance = 100.0, currency = "USD"),
        Account(id = "acc_bank", name = "Bancolombia", type = AccountType.CHECKING, balance = 500.0, currency = "COP")
    )

    private val sampleCategories = listOf(
        Category(id = "cat_food", name = "Comida", iconName = "restaurant", colorHex = "#F59E0B", type = TransactionType.EXPENSE),
        Category(id = "cat_salary", name = "Salario", iconName = "payments", colorHex = "#10B981", type = TransactionType.INCOME)
    )

    @Test
    fun testInitialStateIsDictating() = runBlocking {
        val txRepo = FakeTransactionRepo()
        val accountRepo = FakeAccountRepo(sampleAccounts)
        val categoryRepo = FakeCategoryRepo(sampleCategories)
        val parseUseCase = ParseVoiceTransactionUseCase(FakeAiConfigStorage(), GeminiApiClient())
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)

        val viewModel = VoiceTransactionViewModel(
            parseVoiceTransactionUseCase = parseUseCase,
            createTransactionUseCase = createTxUseCase,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = viewModel.uiState.first()
        assertEquals(VoiceStep.DICTATING, state.step)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun testProcessVoiceTextTransitionsToPreviewAndSaves() = runBlocking {
        val txRepo = FakeTransactionRepo()
        val accountRepo = FakeAccountRepo(sampleAccounts)
        val categoryRepo = FakeCategoryRepo(sampleCategories)
        val parseUseCase = ParseVoiceTransactionUseCase(FakeAiConfigStorage(), GeminiApiClient())
        val createTxUseCase = CreateTransactionUseCase(txRepo, accountRepo)

        val viewModel = VoiceTransactionViewModel(
            parseVoiceTransactionUseCase = parseUseCase,
            createTransactionUseCase = createTxUseCase,
            accountRepository = accountRepo,
            categoryRepository = categoryRepo,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // 1. Procesar dictado
        viewModel.processVoiceText("Gasté 45 mil en comida con Bancolombia")

        val previewState = viewModel.uiState.first()
        assertEquals(VoiceStep.PREVIEW_EDITABLE, previewState.step)
        assertEquals("45000", previewState.draft.amountString)
        assertEquals(TransactionType.EXPENSE, previewState.draft.type)
        assertEquals("acc_bank", previewState.draft.selectedAccountId)
        assertEquals("cat_food", previewState.draft.selectedCategoryId)

        // 2. Modificar monto y nota en la vista previa editable
        viewModel.setDraftAmount("50000")
        viewModel.setDraftNote("Cena especial")

        var successCalled = false
        viewModel.submitDraft(onSuccess = { successCalled = true })

        assertTrue(successCalled)
        assertEquals(1, txRepo.insertedTransactions.size)
        val saved = txRepo.insertedTransactions.first()
        assertEquals(50000.0, saved.amount)
        assertEquals("Cena especial", saved.note)
        assertEquals("acc_bank", saved.accountId)
        assertEquals("cat_food", saved.categoryId)
    }
}
