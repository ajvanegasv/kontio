package dev.ajvanegasv.kontio.presentation.analytics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.AiVisualReport
import dev.ajvanegasv.kontio.domain.model.ChartBarItem
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.categories.components.parseColorFromHex
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper

/**
 * Tarjeta de Reporte de IA generativo (MCP-UI) para Kontio.
 * Renderiza dinámicamente vistas personalizadas según el tipo de reporte/payload retornado
 * por el mini agente de IA (Cuentas, Desglose por Categorías, Transacciones y Tendencias,
 * Visión Financiera General o Respuestas Conversacionales).
 */
@Composable
fun AiVisualReportCard(
    report: AiVisualReport,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Encabezado común con Badge de IA, Chip de Herramienta MCP y botón de cierre
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DashboardIcons.AutoAwesome,
                            contentDescription = "IA",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (report.isAiGenerated) "KONTIO AI REPORT" else "REPORTE DE BÚSQUEDA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )

                            if (report.toolsUsed.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = report.toolsUsed.first(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Text(
                            text = report.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = DashboardIcons.Close,
                        contentDescription = "Cerrar reporte",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Renderizado dinámico de la vista personalizada según el payload MCP-UI
            when (val payload = report.visualPayload) {
                is AgentVisualPayload.AccountsSummaryPayload -> {
                    AccountsSummaryReportView(
                        payload = payload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is AgentVisualPayload.CategoryBreakdownPayload -> {
                    CategoryBreakdownReportView(
                        payload = payload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is AgentVisualPayload.TransactionListPayload -> {
                    TransactionListReportView(
                        payload = payload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is AgentVisualPayload.FinancialOverviewPayload -> {
                    FinancialOverviewReportView(
                        payload = payload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                null -> {
                    if (report.chartBars.isNotEmpty() || report.matchingTransactions.isNotEmpty()) {
                        LegacyTransactionReportView(
                            report = report,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        ConversationalAdviceReportView(
                            advice = report.aiAdvice,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 3. Consejo o Análisis Inteligente (visible cuando hay payload tabular y consejo disponible)
            if (report.visualPayload != null && report.aiAdvice.isNotBlank()) {
                AiAdviceBanner(
                    advice = report.aiAdvice,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// COMPONENTE PERSONALIZADO 1: RESUMEN DE CUENTAS Y BANCOS (AccountsSummaryReportView)
// =============================================================================
@Composable
private fun AccountsSummaryReportView(
    payload: AgentVisualPayload.AccountsSummaryPayload,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Hero de Patrimonio Neto Consolidado
        if (payload.formattedNetWorth.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DashboardIcons.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "PATRIMONIO NETO CONSOLIDADO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = payload.formattedNetWorth,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Listado de cuentas con formato de tarjeta de banco
        if (payload.accounts.isEmpty()) {
            Text(
                text = "No se encontraron cuentas financieras registradas.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payload.accounts.forEach { acc ->
                    val isCreditCard = acc.type == AccountType.CREDIT_CARD
                    val accColor = parseColorFromHex(acc.colorHex)
                    val typeLabel = when (acc.type) {
                        AccountType.CREDIT_CARD -> "Tarjeta de Crédito"
                        AccountType.SAVINGS -> "Cuenta de Ahorros"
                        AccountType.CHECKING -> "Cuenta Corriente"
                        AccountType.CASH -> "Efectivo"
                        AccountType.DIGITAL_WALLET -> "Billetera Digital"
                    }
                    val accIcon = when (acc.type) {
                        AccountType.CREDIT_CARD -> DashboardIcons.CreditCard
                        else -> DashboardIcons.AccountBalanceWallet
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accColor.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = accIcon,
                                    contentDescription = null,
                                    tint = accColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = acc.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = typeLabel,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isCreditCard && acc.availableCreditFormatted != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Cupo disp: ${acc.availableCreditFormatted}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = acc.balanceFormatted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCreditCard && acc.balanceRaw > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            if (isCreditCard) {
                                Text(
                                    text = if (acc.balanceRaw > 0) "Deuda consumida" else "Sin deuda",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPONENTE PERSONALIZADO 2: DESGLOSE POR CATEGORÍAS (CategoryBreakdownReportView)
// =============================================================================
@Composable
private fun CategoryBreakdownReportView(
    payload: AgentVisualPayload.CategoryBreakdownPayload,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Métricas superiores de gasto por categoría
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMetricTile(
                modifier = Modifier.weight(1.3f),
                title = "TOTAL EN ${payload.timeframeLabel.uppercase()}",
                value = payload.totalAmountFormatted,
                valueColor = MaterialTheme.colorScheme.primary
            )

            ReportMetricTile(
                modifier = Modifier.weight(0.9f),
                title = "CATEGORÍAS",
                value = "${payload.categories.size}",
                valueColor = MaterialTheme.colorScheme.onSurface
            )

            val totalTxs = payload.categories.sumOf { it.transactionCount }
            ReportMetricTile(
                modifier = Modifier.weight(0.9f),
                title = "MOVIMIENTOS",
                value = "$totalTxs",
                valueColor = MaterialTheme.colorScheme.onSurface
            )
        }

        // Ranking visual de categorías con barras de distribución
        if (payload.categories.isEmpty()) {
            Text(
                text = "No se encontraron gastos en este periodo.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payload.categories.forEach { item ->
                    val catColor = parseColorFromHex(item.colorHex)
                    val catIcon = IconMapper.getIconForCategory(item.iconName, TransactionType.EXPENSE)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${item.transactionCount} movimientos",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = item.amountFormatted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.percentage.toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = catColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Barra de progreso horizontal de porcentaje de gasto
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((item.percentage / 100f).coerceIn(0.02f, 1f))
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(catColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// COMPONENTE PERSONALIZADO 3: LISTADO DE TRANSACCIONES Y TENDENCIAS (TransactionListReportView)
// =============================================================================
@Composable
private fun TransactionListReportView(
    payload: AgentVisualPayload.TransactionListPayload,
    modifier: Modifier = Modifier
) {
    var isTxListExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPIs Clave
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMetricTile(
                modifier = Modifier.weight(1.2f),
                title = "TOTAL",
                value = payload.totalAmountFormatted,
                valueColor = MaterialTheme.colorScheme.primary
            )

            ReportMetricTile(
                modifier = Modifier.weight(0.9f),
                title = "CANTIDAD",
                value = "${payload.count}",
                valueColor = MaterialTheme.colorScheme.onSurface
            )

            ReportMetricTile(
                modifier = Modifier.weight(1.1f),
                title = "PROMEDIO",
                value = payload.averageAmountFormatted,
                valueColor = MaterialTheme.colorScheme.onSurface
            )
        }

        // Gráfico de Barras Visual en Canvas
        if (payload.chartBars.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Distribución Cronológica",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                VisualBarChart(
                    bars = payload.chartBars,
                    currency = payload.currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }

        // Acordeón de transacciones asociadas
        if (payload.transactions.isNotEmpty()) {
            TransactionAccordion(
                transactions = payload.transactions,
                isExpanded = isTxListExpanded,
                onToggleExpand = { isTxListExpanded = !isTxListExpanded }
            )
        }
    }
}

// =============================================================================
// COMPONENTE PERSONALIZADO 4: VISIÓN FINANCIERA GENERAL (FinancialOverviewReportView)
// =============================================================================
@Composable
private fun FinancialOverviewReportView(
    payload: AgentVisualPayload.FinancialOverviewPayload,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Fila de Ingresos, Gastos y Ahorro Neto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMetricTile(
                modifier = Modifier.weight(1f),
                title = "INGRESOS",
                value = payload.incomeFormatted,
                valueColor = MaterialTheme.colorScheme.secondary
            )

            ReportMetricTile(
                modifier = Modifier.weight(1f),
                title = "GASTOS",
                value = payload.expensesFormatted,
                valueColor = MaterialTheme.colorScheme.error
            )

            ReportMetricTile(
                modifier = Modifier.weight(1f),
                title = "AHORRO NETO",
                value = payload.savingsFormatted,
                valueColor = MaterialTheme.colorScheme.primary
            )
        }

        // Medidor de Tasa de Ahorro (Savings Rate Gauge)
        val rateColor = when {
            payload.savingsRate >= 20f -> MaterialTheme.colorScheme.primary
            payload.savingsRate > 0f -> Color(0xFFF59E0B)
            else -> MaterialTheme.colorScheme.error
        }
        val rateMessage = when {
            payload.savingsRate >= 20f -> "¡Excelente ritmo de ahorro! Muy saludable para tus objetivos."
            payload.savingsRate > 0f -> "Ritmo de ahorro positivo. Hay espacio para optimizar gastos."
            else -> "Atención: Los gastos superaron los ingresos en este periodo."
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasa de Ahorro (${payload.periodLabel})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${payload.savingsRate.toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = rateColor
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((payload.savingsRate / 100f).coerceIn(0f, 1f))
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(rateColor)
                    )
                }

                Text(
                    text = rateMessage,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// COMPONENTE PERSONALIZADO 5: CONVERSACIONAL DIRECTO (ConversationalAdviceReportView)
// =============================================================================
@Composable
private fun ConversationalAdviceReportView(
    advice: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = DashboardIcons.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Respuesta de Kontio AI",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = advice,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// =============================================================================
// VISTA LEGACY DE TRANSACCIONES (para AiVisualReport sin visualPayload explícito)
// =============================================================================
@Composable
private fun LegacyTransactionReportView(
    report: AiVisualReport,
    modifier: Modifier = Modifier
) {
    var isTxListExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportMetricTile(
                modifier = Modifier.weight(1.2f),
                title = "TOTAL",
                value = CurrencyFormatter.format(report.totalAmount, report.currency),
                valueColor = MaterialTheme.colorScheme.primary
            )

            ReportMetricTile(
                modifier = Modifier.weight(0.9f),
                title = "CANTIDAD",
                value = "${report.transactionCount}",
                valueColor = MaterialTheme.colorScheme.onSurface
            )

            ReportMetricTile(
                modifier = Modifier.weight(1.1f),
                title = "PROMEDIO",
                value = CurrencyFormatter.format(report.averageAmount, report.currency),
                valueColor = MaterialTheme.colorScheme.onSurface
            )
        }

        if (report.chartBars.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Distribución Cronológica",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                VisualBarChart(
                    bars = report.chartBars,
                    currency = report.currency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }

        if (report.matchingTransactions.isNotEmpty()) {
            TransactionAccordion(
                transactions = report.matchingTransactions,
                isExpanded = isTxListExpanded,
                onToggleExpand = { isTxListExpanded = !isTxListExpanded }
            )
        }
    }
}

// =============================================================================
// COMPONENTES AUXILIARES REUTILIZABLES
// =============================================================================

@Composable
private fun AiAdviceBanner(
    advice: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = DashboardIcons.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Consejo Financiero Inteligente",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = advice,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TransactionAccordion(
    transactions: List<Transaction>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ver movimientos asociados (${transactions.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Icon(
                imageVector = if (isExpanded) DashboardIcons.ArrowUpward else DashboardIcons.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                transactions.forEach { tx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.note.ifBlank { tx.category?.name ?: "Transacción" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = DateFormatter.formatDisplayDate(tx.timestamp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = CurrencyFormatter.format(tx.amount, tx.currency),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricTile(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VisualBarChart(
    bars: List<ChartBarItem>,
    currency: String,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(bars) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.width(42.dp)
            ) {
                Text(
                    text = CurrencyFormatter.format(bar.amount, currency, showSign = false).replace(" ", ""),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Canvas(
                    modifier = Modifier
                        .width(22.dp)
                        .height(80.dp)
                ) {
                    val fullHeight = size.height
                    val barHeight = fullHeight * bar.heightRatio * animProgress.value
                    val top = fullHeight - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        topLeft = Offset(0f, top),
                        size = Size(size.width, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = bar.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}
