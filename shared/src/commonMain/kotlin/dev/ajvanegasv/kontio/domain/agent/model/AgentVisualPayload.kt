package dev.ajvanegasv.kontio.domain.agent.model

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.ChartBarItem
import dev.ajvanegasv.kontio.domain.model.Transaction
import kotlinx.serialization.Serializable

/**
 * Jerarquía polimórfica que representa el modelo visual estructurado devuelto por las
 * herramientas del agente de IA. Diseñado para ser renderizado dinámicamente en la UI (estilo MCP-UI).
 */
@Serializable
sealed interface AgentVisualPayload {

    /**
     * Resumen de cuentas y bancos (saldos, tarjetas de crédito, cupos disponibles y patrimonio neto).
     */
    @Serializable
    data class AccountsSummaryPayload(
        val accounts: List<AccountSummaryItem>,
        val totalNetWorthByCurrency: Map<String, Double> = emptyMap(),
        val formattedNetWorth: String = ""
    ) : AgentVisualPayload

    @Serializable
    data class AccountSummaryItem(
        val id: String,
        val name: String,
        val type: AccountType,
        val balanceFormatted: String,
        val balanceRaw: Double,
        val currency: String,
        val availableCreditFormatted: String? = null,
        val creditLimitFormatted: String? = null,
        val colorHex: String,
        val iconName: String
    )

    /**
     * Desglose y distribución del gasto por categorías en un periodo temporal.
     */
    @Serializable
    data class CategoryBreakdownPayload(
        val timeframeLabel: String,
        val categories: List<CategorySpendingItem>,
        val totalAmountFormatted: String,
        val totalAmountRaw: Double,
        val currency: String
    ) : AgentVisualPayload

    @Serializable
    data class CategorySpendingItem(
        val categoryId: String,
        val name: String,
        val amountFormatted: String,
        val amountRaw: Double,
        val percentage: Float, // 0.0f a 100.0f
        val colorHex: String,
        val iconName: String,
        val transactionCount: Int
    )

    /**
     * Listado o reporte visual de transacciones coincidentes con métricas y gráfico de barras.
     */
    @Serializable
    data class TransactionListPayload(
        val title: String,
        val query: String,
        val transactions: List<Transaction>,
        val totalAmountRaw: Double,
        val totalAmountFormatted: String,
        val count: Int,
        val averageAmountFormatted: String,
        val currency: String,
        val chartBars: List<ChartBarItem> = emptyList()
    ) : AgentVisualPayload

    /**
     * Visión general de ingresos vs gastos vs tasa de ahorro.
     */
    @Serializable
    data class FinancialOverviewPayload(
        val periodLabel: String,
        val incomeFormatted: String,
        val expensesFormatted: String,
        val savingsFormatted: String,
        val savingsRate: Float,
        val currency: String
    ) : AgentVisualPayload
}
