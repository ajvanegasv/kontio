package dev.ajvanegasv.kontio.domain.agent

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiRequest
import dev.ajvanegasv.kontio.domain.agent.tools.GetAccountsSummaryTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetCategorySpendingTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetFinancialOverviewTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetRecentTransactionsTool
import dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.FakeAccountRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsCategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsTransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.GetFinancialSuggestionsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeAiConfig(
    initialApiKey: String? = null,
    initialEnabled: Boolean = false
) : AiConfigStorage {
    private val apiKey = MutableStateFlow(initialApiKey)
    private val model = MutableStateFlow("gemini-3.8-flash")
    private val enabled = MutableStateFlow(initialEnabled)

    override val apiKeyFlow: Flow<String?> = apiKey
    override fun getApiKey(): String? = apiKey.value
    override fun setApiKey(apiKey: String) { this.apiKey.value = apiKey }
    override fun clearApiKey() { this.apiKey.value = null }

    override val modelFlow: Flow<String> = model
    override fun getModel(): String = model.value
    override fun setModel(model: String) { this.model.value = model }

    override val isAiEnabledFlow: Flow<Boolean> = enabled
    override fun isAiEnabled(): Boolean = enabled.value
    override fun setAiEnabled(enabled: Boolean) { this.enabled.value = enabled }
}

private class FakeGeminiClient(
    var simulatedJsonResult: Result<String> = Result.failure(IllegalStateException("No network"))
) : GeminiApiClient() {
    override suspend fun generateContent(
        apiKey: String,
        request: GeminiRequest,
        model: String
    ): Result<String> = simulatedJsonResult
}

class FinancialSuggestionsUseCaseTest {

    private fun createUseCase(
        accountRepo: FakeAccountRepository = FakeAccountRepository(),
        catRepo: FakeAnalyticsCategoryRepository = FakeAnalyticsCategoryRepository(),
        txRepo: FakeAnalyticsTransactionRepository = FakeAnalyticsTransactionRepository(),
        aiConfig: FakeAiConfig = FakeAiConfig(),
        geminiClient: GeminiApiClient = GeminiApiClient()
    ): GetFinancialSuggestionsUseCase {
        val registry = KontioToolRegistry(
            listOf(
                GetAccountsSummaryTool(accountRepo),
                GetCategorySpendingTool(catRepo, txRepo),
                GetRecentTransactionsTool(txRepo),
                GetFinancialOverviewTool(txRepo)
            )
        )
        return GetFinancialSuggestionsUseCase(
            toolRegistry = registry,
            aiConfigStorage = aiConfig,
            geminiApiClient = geminiClient
        )
    }

    @Test
    fun testLocalSuggestionsWithUserData() = runBlocking {
        val accountRepo = FakeAccountRepository()
        accountRepo.insertAccount(
            Account(id = "acc_cc", name = "Tarjeta Nu", type = AccountType.CREDIT_CARD, balance = 200.0, creditLimit = 1000.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc_bank", name = "Bancolombia", type = AccountType.CHECKING, balance = 1500.0, currency = "USD")
        )

        val cat = Category(id = "cat_rest", name = "Restaurantes", iconName = "restaurant", colorHex = "#FF5722", type = TransactionType.EXPENSE)
        val catRepo = FakeAnalyticsCategoryRepository(listOf(cat))

        val now = Clock.System.now().toEpochMilliseconds()
        val tx1 = Transaction(id = "tx1", accountId = "acc_bank", categoryId = "cat_rest", type = TransactionType.EXPENSE, amount = 45.0, timestamp = now, note = "Uber Eats", category = cat)
        val txRepo = FakeAnalyticsTransactionRepository(listOf(tx1))

        val useCase = createUseCase(
            accountRepo = accountRepo,
            catRepo = catRepo,
            txRepo = txRepo,
            aiConfig = FakeAiConfig(initialApiKey = null, initialEnabled = false)
        )

        val suggestions = useCase()
        assertFalse(suggestions.isEmpty())

        // 1. Debe sugerir la tarjeta de crédito real
        val ccSuggestion = suggestions.firstOrNull { it.label.contains("Tarjeta Nu") }
        assertNotNull(ccSuggestion)
        assertTrue(ccSuggestion.query.contains("Tarjeta Nu"))
        assertEquals("get_account_detail", ccSuggestion.toolName)

        // 2. Debe sugerir el banco real
        val bankSuggestion = suggestions.firstOrNull { it.label.contains("Bancolombia") }
        assertNotNull(bankSuggestion)
        assertTrue(bankSuggestion.query.contains("Bancolombia"))

        // 3. Debe sugerir la categoría top real
        val catSuggestion = suggestions.firstOrNull { it.label.contains("Restaurantes") }
        assertNotNull(catSuggestion)
        assertTrue(catSuggestion.query.contains("Restaurantes"))
        assertEquals("get_category_spending", catSuggestion.toolName)

        // 4. Debe sugerir el comercio frecuente real
        val merchantSuggestion = suggestions.firstOrNull { it.label.contains("Uber Eats") }
        assertNotNull(merchantSuggestion)
        assertTrue(merchantSuggestion.query.contains("Uber Eats"))
        assertEquals("search_transactions", merchantSuggestion.toolName)

        // 5. Debe contener balance y movimientos recientes
        assertTrue(suggestions.any { it.label.contains("Balance") })
        assertTrue(suggestions.any { it.label.contains("Movimientos") })
    }

    @Test
    fun testColdStartDefaultSuggestions() = runBlocking {
        val useCase = createUseCase(
            aiConfig = FakeAiConfig(initialApiKey = null, initialEnabled = false)
        )

        val suggestions = useCase()
        assertFalse(suggestions.isEmpty())
        assertTrue(suggestions.any { it.label.contains("Mis Bancos") || it.label.contains("Cuentas") })
        assertTrue(suggestions.any { it.label.contains("Balance") })
        assertTrue(suggestions.any { it.label.contains("Categoría") })
        assertTrue(suggestions.any { it.label.contains("Movimientos") })
    }

    @Test
    fun testGeminiOnlineSuggestions() = runBlocking {
        val fakeAiResponse = """
            [
              {"id": "sug_gemini_1", "label": "🍔 Gasto en Starbucks", "query": "¿Cuánto he gastado en Starbucks este mes?", "toolName": "search_transactions"},
              {"id": "sug_gemini_2", "label": "💳 Cupo en Tarjeta Nu", "query": "¿Cuál es el cupo disponible de mi Tarjeta Nu?", "toolName": "get_account_detail"}
            ]
        """.trimIndent()

        val fakeGemini = FakeGeminiClient(simulatedJsonResult = Result.success(fakeAiResponse))
        val useCase = createUseCase(
            aiConfig = FakeAiConfig(initialApiKey = "fake-key", initialEnabled = true),
            geminiClient = fakeGemini
        )

        val suggestions = useCase()
        assertEquals(2, suggestions.size)
        assertEquals("🍔 Gasto en Starbucks", suggestions[0].label)
        assertEquals("¿Cuánto he gastado en Starbucks este mes?", suggestions[0].query)
        assertEquals("search_transactions", suggestions[0].toolName)
        assertEquals("💳 Cupo en Tarjeta Nu", suggestions[1].label)
    }

    @Test
    fun testGeminiFailureFallsBackToLocal() = runBlocking {
        val fakeGemini = FakeGeminiClient(simulatedJsonResult = Result.failure(IllegalStateException("API Error 500")))
        val useCase = createUseCase(
            aiConfig = FakeAiConfig(initialApiKey = "fake-key", initialEnabled = true),
            geminiClient = fakeGemini
        )

        val suggestions = useCase()
        // No debe fallar, debe devolver el fallback heurístico local
        assertFalse(suggestions.isEmpty())
        assertTrue(suggestions.any { it.label.contains("Balance") })
    }

    @Test
    fun testCacheAndForceRefresh() = runBlocking {
        val useCase = createUseCase()
        val firstResult = useCase()
        val secondResult = useCase(forceRefresh = false)
        assertEquals(firstResult, secondResult)

        useCase.clearCache()
        val refreshedResult = useCase(forceRefresh = true)
        assertEquals(firstResult.size, refreshedResult.size)
    }
}
