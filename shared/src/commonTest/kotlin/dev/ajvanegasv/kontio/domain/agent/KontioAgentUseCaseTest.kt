package dev.ajvanegasv.kontio.domain.agent

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.agent.service.KontioAgentUseCase
import dev.ajvanegasv.kontio.domain.agent.tools.GetAccountsSummaryTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetCategoriesTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetCategorySpendingTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetFinancialOverviewTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetRecentTransactionsTool
import dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry
import dev.ajvanegasv.kontio.domain.agent.tools.SearchTransactionsTool
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.FakeAccountRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsCategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsTransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.FinancialToolExecutor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeAgentConfigStorage(
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

class KontioAgentUseCaseTest {

    private fun createUseCase(
        accountRepo: FakeAccountRepository = FakeAccountRepository(),
        catRepo: FakeAnalyticsCategoryRepository = FakeAnalyticsCategoryRepository(),
        txRepo: FakeAnalyticsTransactionRepository = FakeAnalyticsTransactionRepository(),
        storage: FakeAgentConfigStorage = FakeAgentConfigStorage()
    ): KontioAgentUseCase {
        val executor = FinancialToolExecutor(txRepo, catRepo)
        val registry = KontioToolRegistry(
            listOf(
                GetAccountsSummaryTool(accountRepo),
                GetCategoriesTool(catRepo),
                GetCategorySpendingTool(catRepo, txRepo),
                SearchTransactionsTool(executor),
                GetRecentTransactionsTool(txRepo),
                GetFinancialOverviewTool(txRepo)
            )
        )
        return KontioAgentUseCase(
            toolRegistry = registry,
            aiConfigStorage = storage
        )
    }

    @Test
    fun executeQuery_withDisallowedMutation_returnsPoliteRejectionWithoutRunningTools() = runBlocking {
        val useCase = createUseCase()
        val response = useCase.executeQuery("Eliminar todas las transacciones de comida")

        assertTrue(response.text.contains("solo lectura"))
        assertTrue(response.toolsUsed.isEmpty())
        assertFalse(response.isAiGenerated)
    }

    @Test
    fun executeQuery_offlineAccountQuery_returnsAccountsSummaryPayload() = runBlocking {
        val accountRepo = FakeAccountRepository()
        accountRepo.insertAccount(
            Account(id = "acc1", name = "Bancolombia", type = AccountType.SAVINGS, balance = 2500.0, currency = "USD")
        )
        val useCase = createUseCase(accountRepo = accountRepo)

        val response = useCase.executeQuery("¿cuánto dinero tengo en mis cuentas?")

        assertTrue(response.toolsUsed.contains("get_accounts_summary"))
        assertTrue(response.text.contains("Bancolombia"))
        assertNotNull(response.speechText)
        assertFalse(response.speechText.contains("**"))

        val payload = response.visualPayload as? AgentVisualPayload.AccountsSummaryPayload
        assertNotNull(payload)
        assertEquals(1, payload.accounts.size)
    }

    @Test
    fun executeQuery_offlineCategorySpendingQuery_returnsCategoryBreakdownPayload() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val cat = Category(id = "c1", name = "Alimentación", iconName = "restaurant", colorHex = "#123", type = TransactionType.EXPENSE)
        val tx = Transaction(id = "t1", accountId = "a1", categoryId = "c1", type = TransactionType.EXPENSE, amount = 120.0, timestamp = now, note = "Cena")

        val catRepo = FakeAnalyticsCategoryRepository(listOf(cat))
        val txRepo = FakeAnalyticsTransactionRepository(listOf(tx))
        val useCase = createUseCase(catRepo = catRepo, txRepo = txRepo)

        val response = useCase.executeQuery("¿en qué categorías gasto más este mes?")

        assertTrue(response.toolsUsed.contains("get_category_spending"))
        val payload = response.visualPayload as? AgentVisualPayload.CategoryBreakdownPayload
        assertNotNull(payload)
        assertEquals(1, payload.categories.size)
        assertEquals("Alimentación", payload.categories.first().name)
    }
}
