package dev.ajvanegasv.kontio.domain.model

import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import kotlinx.serialization.Serializable

/**
 * Rangos de tiempo predefinidos para la vista de Analytics.
 */
enum class AnalyticsTimeframe(val label: String) {
    CURRENT_MONTH("Este Mes"),
    LAST_3_MONTHS("3 Meses"),
    CURRENT_YEAR("Año"),
    ALL_TIME("Todo")
}

/**
 * Desglose de gasto acumulado por categoría para gráficos de torta/dona y rankings.
 */
@Serializable
data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Float, // 0.0f a 100.0f
    val transactionCount: Int
)

/**
 * Resumen consolidado para la pantalla de Analytics en un rango temporal específico.
 */
@Serializable
data class AnalyticsSummary(
    val totalIncome: Double,
    val totalExpenses: Double,
    val netSavings: Double,
    val savingsRate: Float, // 0.0f a 100.0f
    val categorySpendings: List<CategorySpending>,
    val creditCardTotalExpenses: Double = 0.0,
    val creditCardTotalIncome: Double = 0.0,
    val creditCardCategorySpendings: List<CategorySpending> = emptyList(),
    val timeframe: AnalyticsTimeframe,
    val currency: String = "USD"
)

/**
 * Representa una barra individual en el gráfico de barras visual para el reporte de IA.
 */
@Serializable
data class ChartBarItem(
    val label: String,
    val amount: Double,
    val heightRatio: Float, // 0.0f a 1.0f para la escala visual
    val timestamp: Long = 0L
)

/**
 * Reporte visual completo generado por el agente de IA tras ejecutar herramientas financieras.
 */
@Serializable
data class AiVisualReport(
    val query: String,
    val title: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val averageAmount: Double,
    val currency: String = "USD",
    val chartBars: List<ChartBarItem> = emptyList(),
    val matchingTransactions: List<Transaction> = emptyList(),
    val aiAdvice: String,
    val isAiGenerated: Boolean = true,
    val visualPayload: AgentVisualPayload? = null,
    val toolsUsed: List<String> = emptyList(),
    val speechText: String? = null
)
