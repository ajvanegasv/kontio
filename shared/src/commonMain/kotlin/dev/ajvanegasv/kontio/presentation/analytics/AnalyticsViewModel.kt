package dev.ajvanegasv.kontio.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.AiVisualReport
import dev.ajvanegasv.kontio.domain.model.AnalyticsSummary
import dev.ajvanegasv.kontio.domain.model.AnalyticsTimeframe
import dev.ajvanegasv.kontio.domain.model.CategorySpending
import dev.ajvanegasv.kontio.domain.usecase.AiFinancialAdvisorUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetAnalyticsSummaryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dev.ajvanegasv.kontio.domain.agent.model.AgentResponse
import dev.ajvanegasv.kontio.domain.agent.service.KontioAgentUseCase

data class AnalyticsUiState(
    val summary: AnalyticsSummary = AnalyticsSummary(
        totalIncome = 0.0,
        totalExpenses = 0.0,
        netSavings = 0.0,
        savingsRate = 0f,
        categorySpendings = emptyList(),
        timeframe = AnalyticsTimeframe.CURRENT_MONTH
    ),
    val selectedTimeframe: AnalyticsTimeframe = AnalyticsTimeframe.CURRENT_MONTH,
    val selectedCategoryId: String? = null,
    val queryText: String = "",
    val aiReport: AiVisualReport? = null,
    val agentResponse: AgentResponse? = null,
    val isAiLoading: Boolean = false,
    val isLoadingSummary: Boolean = true,
    val errorMessage: String? = null
)

class AnalyticsViewModel(
    private val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase = AppContainer.getAnalyticsSummaryUseCase,
    private val aiFinancialAdvisorUseCase: AiFinancialAdvisorUseCase = AppContainer.aiFinancialAdvisorUseCase,
    private val agentUseCase: KontioAgentUseCase? = null,
    coroutineScope: CoroutineScope? = null,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _timeframe = MutableStateFlow(AnalyticsTimeframe.CURRENT_MONTH)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _queryText = MutableStateFlow("")
    private val _aiReport = MutableStateFlow<AiVisualReport?>(null)
    private val _agentResponse = MutableStateFlow<AgentResponse?>(null)
    private val _isAiLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        _timeframe.flatMapLatest { tf -> getAnalyticsSummaryUseCase(tf) },
        _selectedCategoryId,
        _queryText,
        _aiReport,
        _agentResponse
    ) { summary, selectedCatId, query, aiReport, agentResp ->
        AnalyticsUiState(
            summary = summary,
            selectedTimeframe = summary.timeframe,
            selectedCategoryId = selectedCatId,
            queryText = query,
            aiReport = aiReport,
            agentResponse = agentResp,
            isAiLoading = false,
            isLoadingSummary = false,
            errorMessage = _errorMessage.value
        )
    }.combine(_isAiLoading) { state, isAiLoading ->
        state.copy(isAiLoading = isAiLoading)
    }.stateIn(
        scope = scope,
        started = sharingStarted,
        initialValue = AnalyticsUiState()
    )

    fun setTimeframe(timeframe: AnalyticsTimeframe) {
        _selectedCategoryId.value = null
        _timeframe.value = timeframe
    }

    fun selectCategory(category: CategorySpending?) {
        _selectedCategoryId.value = category?.categoryId
    }

    fun onQueryChange(text: String) {
        _queryText.value = text
    }

    fun executeAiQuery(query: String = _queryText.value) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return

        _queryText.value = trimmed
        _isAiLoading.value = true
        _errorMessage.value = null

        scope.launch {
            try {
                val resolvedAgent = agentUseCase ?: runCatching { AppContainer.kontioAgentUseCase }.getOrNull()
                val agentResp = resolvedAgent?.executeQuery(trimmed)
                _agentResponse.value = agentResp
                val report = aiFinancialAdvisorUseCase(trimmed)
                _aiReport.value = report
            } catch (e: Exception) {
                _errorMessage.value = "Error al analizar con IA: ${e.message}"
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun dismissAiReport() {
        _aiReport.value = null
        _agentResponse.value = null
    }
}
