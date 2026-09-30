package dev.ajvanegasv.kontio.presentation.budgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ajvanegasv.kontio.presentation.budgets.components.AddBudgetBottomSheet
import dev.ajvanegasv.kontio.presentation.budgets.components.BudgetItemCard
import dev.ajvanegasv.kontio.presentation.budgets.components.QuickBudgetPaymentBottomSheet
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassBottomSheetContainer
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassTopAppBar
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import dev.ajvanegasv.kontio.presentation.util.BackHandler
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter

@Composable
fun BudgetsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BudgetsViewModel = viewModel { BudgetsViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isKontioDarkTheme()

    val isAnySheetOpen = state.isAddBudgetOpen || state.budgetForPayment != null
    BackHandler(enabled = !isAnySheetOpen) {
        onBackClick()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior esmerilada
            KontioGlassTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = DashboardIcons.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Presupuestos",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.budgets.size} activos este mes",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.openAddBudget() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Add,
                            contentDescription = "Nuevo",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Nuevo", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )

            // Contenido scrollable
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Tarjeta Resumen Consolidado del Mes
                if (state.budgets.isNotEmpty()) {
                    item {
                        BudgetSummaryCard(
                            totalBudgeted = state.totalBudgeted,
                            totalSpent = state.totalSpent,
                            totalRemaining = state.totalRemaining,
                            globalPercentage = state.globalPercentage,
                            isAnyExceeded = state.isAnyExceeded
                        )
                    }
                }

                // 2. Estado Vacío si no hay presupuestos
                if (state.budgets.isEmpty() && !state.isLoading) {
                    item {
                        KontioGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = DashboardIcons.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Sin presupuestos activos",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Define límites de gasto por categoría (ej. Agua, Mercado, Ocio) para mantener tus finanzas bajo control y registrar pagos fácilmente.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { viewModel.openAddBudget() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("+ Crear Presupuesto", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // 3. Lista de tarjetas de presupuesto
                items(state.budgets, key = { it.budget.id }) { item ->
                    BudgetItemCard(
                        item = item,
                        onPayClick = { viewModel.openQuickPayment(item) },
                        onEditClick = { viewModel.openEditBudget(item.budget) },
                        onDeleteClick = { viewModel.requestDeleteBudget(item) }
                    )
                }
            }
        }

        // Modal inferior: Crear o Editar Presupuesto
        KontioGlassBottomSheetContainer(
            visible = state.isAddBudgetOpen,
            onDismissRequest = { viewModel.closeAddBudget() }
        ) {
            AddBudgetBottomSheet(
                editingBudget = state.editingBudget,
                categories = state.categories,
                onSave = { name, categoryId, limit, period, currency, note ->
                    viewModel.saveBudget(
                        name = name,
                        categoryId = categoryId,
                        limitAmount = limit,
                        period = period,
                        currency = currency,
                        note = note
                    )
                },
                onDismiss = { viewModel.closeAddBudget() },
                errorMessage = state.errorMessage
            )
        }

        // Modal inferior: Pagar / Registrar Gasto asistido desde el Presupuesto
        val paymentTarget = state.budgetForPayment
        if (paymentTarget != null) {
            KontioGlassBottomSheetContainer(
                visible = true,
                onDismissRequest = { viewModel.closeQuickPayment() }
            ) {
                QuickBudgetPaymentBottomSheet(
                    budgetProgress = paymentTarget,
                    accounts = state.accounts,
                    isSubmitting = state.isSubmittingPayment,
                    onConfirmPayment = { amount, accountId, dateMillis, note ->
                        viewModel.executeQuickPayment(
                            amount = amount,
                            accountId = accountId,
                            dateMillis = dateMillis,
                            note = note
                        )
                    },
                    onDismiss = { viewModel.closeQuickPayment() },
                    errorMessage = state.errorMessage
                )
            }
        }

        // Diálogo de confirmación para eliminar presupuesto
        val pendingDelete = state.budgetPendingDelete
        if (pendingDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteBudget() },
                title = {
                    Text(
                        text = "¿Eliminar presupuesto?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas eliminar el presupuesto \"${pendingDelete.budget.name}\"? Las transacciones registradas previamente no serán eliminadas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDeleteBudget() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteBudget() }) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Diálogo de alerta en caso de error general
        if (state.errorMessage != null && state.budgetPendingDelete == null && !state.isAddBudgetOpen && state.budgetForPayment == null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = {
                    Text(
                        text = "Aviso",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Entendido", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudgeted: Double,
    totalSpent: Double,
    totalRemaining: Double,
    globalPercentage: Float,
    isAnyExceeded: Boolean
) {
    val isDark = isKontioDarkTheme()
    val progressFraction = globalPercentage.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "summaryProgress")

    val progressColor = when {
        isAnyExceeded || globalPercentage >= 1.0f -> MaterialTheme.colorScheme.error
        globalPercentage >= 0.80f -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.secondary
    }

    KontioGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESUMEN DEL MES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${(globalPercentage * 100).toInt()}% consumido",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Fila de Métricas Consolidadas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Presupuestado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalBudgeted),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(text = "Gastado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalSpent),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAnyExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(text = "Disponible", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.format(totalRemaining),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra de progreso consolidada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.35f else 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor)
                )
            }
        }
    }
}
