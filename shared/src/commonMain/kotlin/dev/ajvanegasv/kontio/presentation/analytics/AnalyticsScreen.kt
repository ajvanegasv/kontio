package dev.ajvanegasv.kontio.presentation.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ajvanegasv.kontio.presentation.analytics.components.AiQueryBar
import dev.ajvanegasv.kontio.presentation.analytics.components.AiVisualReportCard
import dev.ajvanegasv.kontio.presentation.analytics.components.CategorySpendingList
import dev.ajvanegasv.kontio.presentation.analytics.components.DonutPieChart
import dev.ajvanegasv.kontio.presentation.analytics.components.TimeframeSelector
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter

/**
 * Pantalla principal de Analytics en Kontio:
 * Integra gráfico interactivo de torta/dona por categorías, selector temporal,
 * asistente de IA para búsquedas de transacciones y generación de reportes visuales con gráficos Canvas.
 */
@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = viewModel { AnalyticsViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Título y Selector de Rango de Tiempo
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Analytics",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Optimización financiera & desglose inteligente",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Leaderboard,
                            contentDescription = "Analytics",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TimeframeSelector(
                    selectedTimeframe = state.selectedTimeframe,
                    onTimeframeSelected = { viewModel.setTimeframe(it) }
                )
            }
        }

        // 2. Tarjeta Principal con Donut / Pie Chart interactivo (Flujo de Caja - Cuentas & Efectivo)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DashboardIcons.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Flujo de Caja (Cuentas & Efectivo)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                KontioGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DonutPieChart(
                            spendings = state.summary.categorySpendings,
                            selectedCategoryId = state.selectedCategoryId,
                            onCategorySelect = { viewModel.selectCategory(it) },
                            totalExpensesFormatted = CurrencyFormatter.format(state.summary.totalExpenses, state.summary.currency),
                            currency = state.summary.currency
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Resumen rápido de Ingresos, Gastos y Ahorro
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryPill(
                                label = "Ingresos",
                                amount = CurrencyFormatter.format(state.summary.totalIncome, state.summary.currency),
                                color = MaterialTheme.colorScheme.secondary
                            )

                            SummaryPill(
                                label = "Gastos",
                                amount = CurrencyFormatter.format(state.summary.totalExpenses, state.summary.currency),
                                color = MaterialTheme.colorScheme.error
                            )

                            SummaryPill(
                                label = "Ahorro",
                                amount = "${(state.summary.savingsRate * 10).toInt() / 10f}%",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (state.summary.categorySpendings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(20.dp))
                            CategorySpendingList(
                                spendings = state.summary.categorySpendings,
                                selectedCategoryId = state.selectedCategoryId,
                                onCategorySelect = { cat ->
                                    if (cat.categoryId == state.selectedCategoryId) {
                                        viewModel.selectCategory(null)
                                    } else {
                                        viewModel.selectCategory(cat)
                                    }
                                },
                                currency = state.summary.currency
                            )
                        }
                    }
                }
            }
        }

        // 3. Tarjeta Exclusiva de Tarjetas de Crédito
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DashboardIcons.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gastos con Tarjeta de Crédito",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                KontioGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (state.summary.creditCardCategorySpendings.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = DashboardIcons.CreditCard,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Sin consumos en tarjeta de crédito",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Los movimientos realizados con tarjeta de crédito se muestran de forma independiente de tu flujo de caja.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            DonutPieChart(
                                spendings = state.summary.creditCardCategorySpendings,
                                selectedCategoryId = state.selectedCreditCategoryId,
                                onCategorySelect = { viewModel.selectCreditCategory(it) },
                                totalExpensesFormatted = CurrencyFormatter.format(state.summary.creditCardTotalExpenses, state.summary.currency),
                                currency = state.summary.currency
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                SummaryPill(
                                    label = "Consumos",
                                    amount = CurrencyFormatter.format(state.summary.creditCardTotalExpenses, state.summary.currency),
                                    color = MaterialTheme.colorScheme.error
                                )

                                SummaryPill(
                                    label = "Abonos / Reembolsos",
                                    amount = CurrencyFormatter.format(state.summary.creditCardTotalIncome, state.summary.currency),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            CategorySpendingList(
                                spendings = state.summary.creditCardCategorySpendings,
                                selectedCategoryId = state.selectedCreditCategoryId,
                                onCategorySelect = { cat ->
                                    if (cat.categoryId == state.selectedCreditCategoryId) {
                                        viewModel.selectCreditCategory(null)
                                    } else {
                                        viewModel.selectCreditCategory(cat)
                                    }
                                },
                                currency = state.summary.currency
                            )
                        }
                    }
                }
            }
        }

        // 4. Sección Asistente Financiero Kontio AI
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DashboardIcons.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Consultas Financieras con IA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AiQueryBar(
                    queryText = state.queryText,
                    onQueryChange = { viewModel.onQueryChange(it) },
                    onExecuteQuery = { viewModel.executeAiQuery(it) },
                    isLoading = state.isAiLoading,
                    suggestions = state.suggestions,
                    isLoadingSuggestions = state.isLoadingSuggestions,
                    onRefreshSuggestions = { viewModel.loadSuggestions(forceRefresh = true) }
                )
            }
        }

        // 5. Tarjeta de Reporte Visual con Gráficos (si hay una consulta activa)
        item {
            AnimatedVisibility(
                visible = state.aiReport != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.aiReport?.let { report ->
                    AiVisualReportCard(
                        report = report,
                        onDismiss = { viewModel.dismissAiReport() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
