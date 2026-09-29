package dev.ajvanegasv.kontio.presentation.analytics

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.domain.model.AnalyticsTimeframe
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.CategorySpending
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.AiFinancialAdvisorUseCase
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsCategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsTransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.FinancialToolExecutor
import dev.ajvanegasv.kontio.domain.usecase.GetAnalyticsSummaryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FakeTestAiConfigStorage : AiConfigStorage {
    private val apiKey = MutableStateFlow<String?>(null)
    private val model = MutableStateFlow("gemini-3.8-flash")
    private val enabled = MutableStateFlow(true)

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

class AnalyticsViewModelTest {

    @Test
    fun testTimeframeAndCategorySelection() = runBlocking {
        val catRepo = FakeAnalyticsCategoryRepository()
        val txRepo = FakeAnalyticsTransactionRepository()
        val getSummaryUseCase = GetAnalyticsSummaryUseCase(txRepo, catRepo)
        val toolExecutor = FinancialToolExecutor(txRepo, catRepo)
        val aiStorage = FakeTestAiConfigStorage()
        val aiUseCase = AiFinancialAdvisorUseCase(toolExecutor, aiStorage)

        val viewModel = AnalyticsViewModel(
            getAnalyticsSummaryUseCase = getSummaryUseCase,
            aiFinancialAdvisorUseCase = aiUseCase,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )

        assertEquals(AnalyticsTimeframe.CURRENT_MONTH, viewModel.uiState.value.selectedTimeframe)

        viewModel.setTimeframe(AnalyticsTimeframe.CURRENT_YEAR)
        assertEquals(AnalyticsTimeframe.CURRENT_YEAR, viewModel.uiState.value.selectedTimeframe)

        val dummyCategory = CategorySpending(
            categoryId = "cat_test",
            categoryName = "Test",
            iconName = "star",
            colorHex = "#123456",
            totalAmount = 100.0,
            percentage = 50f,
            transactionCount = 1
        )

        viewModel.selectCategory(dummyCategory)
        assertEquals("cat_test", viewModel.uiState.value.selectedCategoryId)

        viewModel.selectCategory(null)
        assertNull(viewModel.uiState.value.selectedCategoryId)
    }

    @Test
    fun testExecuteAiQuery_updatesAiReportAndAllowsDismissal() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val cat = Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)
        val tx = Transaction(id = "tx1", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 42.0, timestamp = now, note = "Uber Centro", category = cat)

        val catRepo = FakeAnalyticsCategoryRepository(listOf(cat))
        val txRepo = FakeAnalyticsTransactionRepository(listOf(tx))
        val getSummaryUseCase = GetAnalyticsSummaryUseCase(txRepo, catRepo)
        val toolExecutor = FinancialToolExecutor(txRepo, catRepo)
        val aiStorage = FakeTestAiConfigStorage()
        val aiUseCase = AiFinancialAdvisorUseCase(toolExecutor, aiStorage)

        val viewModel = AnalyticsViewModel(
            getAnalyticsSummaryUseCase = getSummaryUseCase,
            aiFinancialAdvisorUseCase = aiUseCase,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )

        assertNull(viewModel.uiState.value.aiReport)

        viewModel.executeAiQuery("¿Cuánto he gastado en Uber?")

        val report = viewModel.uiState.value.aiReport
        assertNotNull(report)
        assertEquals("Uber", report.query)
        assertEquals(42.0, report.totalAmount)
        assertEquals(1, report.transactionCount)

        viewModel.dismissAiReport()
        assertNull(viewModel.uiState.value.aiReport)
    }

    @Test
    fun testExecuteAiQuery_contextualYearQuery_extractsUberAndYear2026() = runBlocking {
        // Timestamp en marzo de 2026
        val timestamp2026 = 1773187200000L
        val cat = Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)
        val tx = Transaction(id = "tx1", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 85.0, timestamp = timestamp2026, note = "Uber VIP", category = cat)

        val catRepo = FakeAnalyticsCategoryRepository(listOf(cat))
        val txRepo = FakeAnalyticsTransactionRepository(listOf(tx))
        val getSummaryUseCase = GetAnalyticsSummaryUseCase(txRepo, catRepo)
        val toolExecutor = FinancialToolExecutor(txRepo, catRepo)
        val aiStorage = FakeTestAiConfigStorage()
        val aiUseCase = AiFinancialAdvisorUseCase(toolExecutor, aiStorage)

        val viewModel = AnalyticsViewModel(
            getAnalyticsSummaryUseCase = getSummaryUseCase,
            aiFinancialAdvisorUseCase = aiUseCase,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )

        viewModel.executeAiQuery("¿cuánto he gastado en Uber en 2026?")

        val report = viewModel.uiState.value.aiReport
        assertNotNull(report)
        assertEquals("Uber", report.query)
        assertEquals("Reporte de Gastos: Uber (2026)", report.title)
        assertEquals(85.0, report.totalAmount)
        assertEquals(1, report.transactionCount)
    }

    @Test
    fun testDynamicSuggestions_loadedInUiState() = runBlocking {
        val catRepo = FakeAnalyticsCategoryRepository()
        val txRepo = FakeAnalyticsTransactionRepository()
        val getSummaryUseCase = GetAnalyticsSummaryUseCase(txRepo, catRepo)
        val toolExecutor = FinancialToolExecutor(txRepo, catRepo)
        val aiStorage = FakeTestAiConfigStorage()
        val aiUseCase = AiFinancialAdvisorUseCase(toolExecutor, aiStorage)

        val registry = dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry(
            listOf(
                dev.ajvanegasv.kontio.domain.agent.tools.GetAccountsSummaryTool(dev.ajvanegasv.kontio.domain.usecase.FakeAccountRepository()),
                dev.ajvanegasv.kontio.domain.agent.tools.GetCategorySpendingTool(catRepo, txRepo),
                dev.ajvanegasv.kontio.domain.agent.tools.GetRecentTransactionsTool(txRepo),
                dev.ajvanegasv.kontio.domain.agent.tools.GetFinancialOverviewTool(txRepo)
            )
        )
        val suggestionsUseCase = dev.ajvanegasv.kontio.domain.usecase.GetFinancialSuggestionsUseCase(
            toolRegistry = registry,
            aiConfigStorage = aiStorage
        )

        val viewModel = AnalyticsViewModel(
            getAnalyticsSummaryUseCase = getSummaryUseCase,
            aiFinancialAdvisorUseCase = aiUseCase,
            getFinancialSuggestionsUseCase = suggestionsUseCase,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined),
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )

        // Las sugerencias deben cargarse en el estado inicial
        val suggestions = viewModel.uiState.value.suggestions
        kotlin.test.assertFalse(suggestions.isEmpty())

        // Refrescar sugerencias
        viewModel.loadSuggestions(forceRefresh = true)
        kotlin.test.assertFalse(viewModel.uiState.value.suggestions.isEmpty())
    }
}
