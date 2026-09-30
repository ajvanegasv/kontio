package dev.ajvanegasv.kontio.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassTopAppBar
import dev.ajvanegasv.kontio.presentation.util.BackHandler
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter

@Composable
fun TransactionsScreen(
    onBackClick: () -> Unit,
    onAddTransactionClick: (TransactionType) -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: TransactionFilter = TransactionFilter.ALL,
    viewModel: TransactionsViewModel = viewModel { TransactionsViewModel() },
    onTransactionClick: (TransactionItemUiModel) -> Unit = {},
    onImportStatementClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isKontioDarkTheme()

    BackHandler(enabled = true) {
        onBackClick()
    }

    LaunchedEffect(initialFilter) {
        viewModel.setFilter(initialFilter)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Barra superior de navegación esmerilada
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
                            text = "Transacciones",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${state.filteredTransactions.size} movimientos registrados",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onImportStatementClick) {
                        Icon(
                            imageVector = DashboardIcons.AutoAwesome,
                            contentDescription = "Importar Extracto con IA",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Button(
                        onClick = {
                            val defaultType = when (state.filter) {
                                TransactionFilter.INCOME -> TransactionType.INCOME
                                else -> TransactionType.EXPENSE
                            }
                            onAddTransactionClick(defaultType)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Add,
                            contentDescription = "Nueva",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Nueva", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )

            // Contenido con desplazamiento
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Tarjetas de resumen métrico consolidado (Ingresos y Egresos totales)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tarjeta Ingresos
                        val isIncomeSelected = state.filter == TransactionFilter.INCOME
                        KontioGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isIncomeSelected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.secondary,
                                            RoundedCornerShape(20.dp)
                                        )
                                    } else Modifier
                                ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(14.dp),
                            onClick = {
                                viewModel.setFilter(
                                    if (isIncomeSelected) TransactionFilter.ALL else TransactionFilter.INCOME
                                )
                            }
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = DashboardIcons.ArrowUpward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "INGRESOS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyFormatter.format(state.totalIncome, state.currency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Tarjeta Egresos
                        val isExpenseSelected = state.filter == TransactionFilter.EXPENSE
                        KontioGlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isExpenseSelected) {
                                        Modifier.border(
                                            2.dp,
                                            MaterialTheme.colorScheme.error,
                                            RoundedCornerShape(20.dp)
                                        )
                                    } else Modifier
                                ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(14.dp),
                            onClick = {
                                viewModel.setFilter(
                                    if (isExpenseSelected) TransactionFilter.ALL else TransactionFilter.EXPENSE
                                )
                            }
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = DashboardIcons.ArrowDownward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EGRESOS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = CurrencyFormatter.format(state.totalExpenses, state.currency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 2. Filtros rápidos (Todas, Ingresos, Gastos)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                MaterialTheme.colorScheme.surfaceContainer.copy(
                                    alpha = if (isDark) 0.35f else 0.65f
                                )
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterPill(
                            label = "Todas (${state.allTransactions.size})",
                            isSelected = state.filter == TransactionFilter.ALL,
                            onClick = { viewModel.setFilter(TransactionFilter.ALL) }
                        )

                        FilterPill(
                            label = "Ingresos (${state.allTransactions.count { it.isIncome }})",
                            isSelected = state.filter == TransactionFilter.INCOME,
                            activeColor = MaterialTheme.colorScheme.secondary,
                            onClick = { viewModel.setFilter(TransactionFilter.INCOME) }
                        )

                        FilterPill(
                            label = "Gastos (${state.allTransactions.count { it.type == TransactionType.EXPENSE }})",
                            isSelected = state.filter == TransactionFilter.EXPENSE,
                            activeColor = MaterialTheme.colorScheme.error,
                            onClick = { viewModel.setFilter(TransactionFilter.EXPENSE) }
                        )
                    }
                }

                // 3. Barra de búsqueda rápida
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Buscar por nota, categoría o cuenta...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = DashboardIcons.Search,
                                contentDescription = "Buscar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotBlank()) {
                                TextButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Text("Limpiar", fontSize = 12.sp)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                                alpha = if (isDark) 0.25f else 0.5f
                            ),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(
                                alpha = if (isDark) 0.15f else 0.35f
                            ),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    )
                }

                // 4. Estado vacío si no hay transacciones
                if (state.filteredTransactions.isEmpty() && !state.isLoading) {
                    item {
                        KontioGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(28.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = DashboardIcons.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = if (state.searchQuery.isNotBlank()) "No se encontraron resultados" else "Sin transacciones",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (state.searchQuery.isNotBlank()) {
                                        "Intenta buscar con otro término o limpia el buscador."
                                    } else {
                                        "Aún no hay transacciones registradas. Puedes crear tu primer ingreso o egreso ahora."
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onAddTransactionClick(TransactionType.INCOME) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Text("+ Ingreso", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    Button(
                                        onClick = { onAddTransactionClick(TransactionType.EXPENSE) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("+ Gasto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Lista de transacciones agrupadas por fecha (Hoy, Ayer, Fecha)
                state.groupedTransactions.forEach { (dateHeader, transactionsInGroup) ->
                    item {
                        Text(
                            text = dateHeader.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }

                    items(transactionsInGroup.size) { index ->
                        val item = transactionsInGroup[index]
                        TransactionItemRow(
                            transaction = item,
                            onClick = { onTransactionClick(item) },
                            onEdit = { onTransactionClick(item) },
                            onDelete = { viewModel.requestDelete(item) }
                        )
                    }
                }
            }
        }

        // Botones flotantes de acción rápida (+ Ingreso / + Gasto) en la parte inferior
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onAddTransactionClick(TransactionType.INCOME) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = DashboardIcons.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Ingreso", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = { onAddTransactionClick(TransactionType.EXPENSE) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = DashboardIcons.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Gasto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Diálogo de confirmación para eliminar transacción
        val txToDelete = state.transactionPendingDelete
        if (txToDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = {
                    Text(
                        text = "¿Eliminar transacción?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas eliminar la transacción \"${txToDelete.title}\" (${txToDelete.amountFormatted})? El saldo de la cuenta será recalculado automáticamente.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDelete() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.cancelDelete() }
                    ) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val isDark = isKontioDarkTheme()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) activeColor.copy(alpha = if (isDark) 0.30f else 0.20f)
                else Color.Transparent
            )
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TransactionItemRow(
    transaction: TransactionItemUiModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()

    val badgeBgColor = if (transaction.isIncome) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.25f else 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.35f else 0.6f)
    }

    val badgeTintColor = if (transaction.isIncome) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono circular
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = transaction.icon,
                        contentDescription = transaction.title,
                        tint = badgeTintColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = transaction.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${transaction.categoryName} • ${transaction.accountName} • ${transaction.timeFormatted}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = transaction.amountFormatted,
                    color = if (transaction.isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = DashboardIcons.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = DashboardIcons.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
