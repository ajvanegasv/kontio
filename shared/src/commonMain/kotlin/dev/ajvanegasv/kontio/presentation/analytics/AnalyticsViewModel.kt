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
import dev.ajvanegasv.kontio.domain.model.FinancialSuggestion
import dev.ajvanegasv.kontio.domain.usecase.GetFinancialSuggestionsUseCase

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
    val selectedCreditCategoryId: String? = null,
    val queryText: String = "",
    val aiReport: AiVisualReport? = null,
    val agentResponse: AgentResponse? = null,
    val suggestions: List<FinancialSuggestion> = emptyList(),
    val isAiLoading: Boolean = false,
    val isLoadingSuggestions: Boolean = false,
    val isLoadingSummary: Boolean = true,
    val errorMessage: String? = null
)

class AnalyticsViewModel(
    private val getAnalyticsSummaryUseCase: GetAnalyticsSummaryUseCase = AppContainer.getAnalyticsSummaryUseCase,
    private val aiFinancialAdvisorUseCase: AiFinancialAdvisorUseCase = AppContainer.aiFinancialAdvisorUseCase,
    private val getFinancialSuggestionsUseCase: GetFinancialSuggestionsUseCase? = null,
    private val agentUseCase: KontioAgentUseCase? = null,
    coroutineScope: CoroutineScope? = null,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _timeframe = MutableStateFlow(AnalyticsTimeframe.CURRENT_MONTH)
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    private val _selectedCreditCategoryId = MutableStateFlow<String?>(null)
    private val _queryText = MutableStateFlow("")
    private val _aiReport = MutableStateFlow<AiVisualReport?>(null)
    private val _agentResponse = MutableStateFlow<AgentResponse?>(null)
    private val _suggestions = MutableStateFlow<List<FinancialSuggestion>>(emptyList())
    private val _isAiLoading = MutableStateFlow(false)
    private val _isLoadingSuggestions = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    init {
        loadSuggestions()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        _timeframe.flatMapLatest { tf -> getAnalyticsSummaryUseCase(tf) },
        _selectedCategoryId,
        _selectedCreditCategoryId,
        _queryText,
        _aiReport
    ) { summary, selectedCatId, selectedCreditCatId, query, aiReport ->
        AnalyticsUiState(
            summary = summary,
            selectedTimeframe = summary.timeframe,
            selectedCategoryId = selectedCatId,
            selectedCreditCategoryId = selectedCreditCatId,
            queryText = query,
            aiReport = aiReport,
            agentResponse = _agentResponse.value,
            suggestions = _suggestions.value,
            isAiLoading = false,
            isLoadingSuggestions = _isLoadingSuggestions.value,
            isLoadingSummary = false,
            errorMessage = _errorMessage.value
        )
    }.combine(_agentResponse) { state, agentResp ->
        state.copy(agentResponse = agentResp)
    }.combine(_isAiLoading) { state, isAiLoading ->
        state.copy(isAiLoading = isAiLoading)
    }.combine(_suggestions) { state, suggestions ->
        state.copy(suggestions = suggestions)
    }.combine(_isLoadingSuggestions) { state, isLoadingSuggestions ->
        state.copy(isLoadingSuggestions = isLoadingSuggestions)
    }.stateIn(
        scope = scope,
        started = sharingStarted,
        initialValue = AnalyticsUiState()
    )

    fun setTimeframe(timeframe: AnalyticsTimeframe) {
        _selectedCategoryId.value = null
        _selectedCreditCategoryId.value = null
        _timeframe.value = timeframe
    }

    fun selectCategory(category: CategorySpending?) {
        _selectedCategoryId.value = category?.categoryId
    }

    fun selectCreditCategory(category: CategorySpending?) {
        _selectedCreditCategoryId.value = category?.categoryId
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
                val baseReport = aiFinancialAdvisorUseCase(trimmed)
                val finalReport = if (agentResp != null && agentResp.visualPayload != null) {
                    baseReport.copy(
                        visualPayload = agentResp.visualPayload,
                        toolsUsed = agentResp.toolsUsed,
                        speechText = agentResp.speechText,
                        aiAdvice = agentResp.text.ifBlank { baseReport.aiAdvice },
                        isAiGenerated = agentResp.isAiGenerated
                    )
                } else if (agentResp != null) {
                    baseReport.copy(
                        toolsUsed = agentResp.toolsUsed,
                        speechText = agentResp.speechText,
                        aiAdvice = agentResp.text.ifBlank { baseReport.aiAdvice },
                        isAiGenerated = agentResp.isAiGenerated
                    )
                } else {
                    baseReport
                }
                _aiReport.value = finalReport
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

    fun loadSuggestions(forceRefresh: Boolean = false) {
        scope.launch {
            _isLoadingSuggestions.value = true
            try {
                val useCase = getFinancialSuggestionsUseCase ?: runCatching { AppContainer.getFinancialSuggestionsUseCase }.getOrNull()
                if (useCase != null) {
                    val list = useCase(forceRefresh)
                    _suggestions.value = list
                }
            } catch (_: Exception) {
                // Silencioso para no degradar la experiencia en pantalla si falla
            } finally {
                _isLoadingSuggestions.value = false
            }
        }
    }
}
