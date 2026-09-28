package dev.ajvanegasv.kontio.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.accounts.components.RealisticBankCard
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassTopAppBar
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper

/**
 * Pantalla de Detalle de Tarjeta o Cuenta Bancaria.
 * Muestra la tarjeta física realista en primer plano, métricas de saldo/cupo/fechas de corte,
 * acciones rápidas para agregar transacciones con la cuenta pre-seleccionada,
 * y el historial filtrado de movimientos asociados a esta tarjeta.
 */
@Composable
fun AccountDetailScreen(
    account: Account,
    transactions: List<Transaction>,
    onBackClick: () -> Unit,
    onAddTransactionClick: (accountId: String, type: TransactionType) -> Unit,
    onDeleteAccount: (Account) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    allAccounts: List<Account> = emptyList(),
    onEditTransaction: (Transaction) -> Unit = {},
    onReassignTransactions: (toAccountId: String) -> Unit = {}
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showReassignDialog by remember { mutableStateOf(false) }
    var transactionPendingDelete by remember { mutableStateOf<Transaction?>(null) }

    val groupedTransactions = remember(transactions) {
        transactions.groupBy { DateFormatter.formatDateGroup(it.timestamp) }
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
                            text = account.name,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = when (account.type) {
                                AccountType.CREDIT_CARD -> "Tarjeta de Crédito • ${account.currency}"
                                AccountType.SAVINGS -> "Cuenta de Ahorros • ${account.currency}"
                                AccountType.CHECKING -> "Cuenta Corriente • ${account.currency}"
                                AccountType.CASH -> "Efectivo • ${account.currency}"
                                AccountType.DIGITAL_WALLET -> "Billetera Digital • ${account.currency}"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    val otherAccounts = allAccounts.filter { it.id != account.id }
                    if (transactions.isNotEmpty() && otherAccounts.isNotEmpty()) {
                        IconButton(onClick = { showReassignDialog = true }) {
                            Icon(
                                imageVector = DashboardIcons.SwapHoriz,
                                contentDescription = "Mover transacciones a otra cuenta",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteAccountDialog = true }) {
                        Icon(
                            imageVector = DashboardIcons.Delete,
                            contentDescription = "Eliminar Cuenta",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Tarjeta Bancaria Realista en Modo Hero
                item {
                    RealisticBankCard(
                        account = account,
                        isHero = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Acciones Rápidas (Nueva Transacción pre-seleccionando esta cuenta)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val defaultType = if (account.type == AccountType.CREDIT_CARD)
                                    TransactionType.EXPENSE
                                else
                                    TransactionType.EXPENSE
                                onAddTransactionClick(account.id, defaultType)
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1.3f),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = DashboardIcons.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Transacción", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Botón de Gasto Rápido
                        FilledTonalButton(
                            onClick = { onAddTransactionClick(account.id, TransactionType.EXPENSE) },
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Text(text = "- Gasto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Botón de Ingreso Rápido
                        FilledTonalButton(
                            onClick = { onAddTransactionClick(account.id, TransactionType.INCOME) },
                            shape = CircleShape,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Text(text = "+ Ingreso", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // 3. Métricas Financieras y Fechas de la Tarjeta
                item {
                    if (account.type == AccountType.CREDIT_CARD) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                KontioGlassCard(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Límite Total",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = CurrencyFormatter.format(account.creditLimit ?: 0.0, account.currency),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                KontioGlassCard(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Cupo Libre",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = CurrencyFormatter.format(account.availableCredit ?: 0.0, account.currency),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            // Fechas de Corte y Pago
                            KontioGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Día de Corte",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (account.cutoffDay != null) "Día ${account.cutoffDay}" else "No fijado",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Fecha Límite Pago",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (account.dueDay != null) "Día ${account.dueDay}" else "No fijado",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KontioGlassCard(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Saldo Actual",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = CurrencyFormatter.format(account.balance, account.currency),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            KontioGlassCard(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Movimientos",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${transactions.size} registrados",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Encabezado de Movimientos de la Tarjeta
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Movimientos en esta cuenta",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${transactions.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val otherAccounts = allAccounts.filter { it.id != account.id }
                        if (transactions.isNotEmpty() && otherAccounts.isNotEmpty()) {
                            TextButton(
                                onClick = { showReassignDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = DashboardIcons.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Mover a otra",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 5. Historial de Transacciones o Estado Vacío
                if (transactions.isEmpty()) {
                    item {
                        KontioGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(28.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = DashboardIcons.CreditCard,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Sin movimientos registrados",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Aún no has registrado transacciones en ${account.name}. Agrega una para comenzar a llevar el control.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { onAddTransactionClick(account.id, TransactionType.EXPENSE) },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = DashboardIcons.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Registrar primer gasto", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    groupedTransactions.forEach { (dateHeader, txsInGroup) ->
                        item {
                            Text(
                                text = dateHeader,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(txsInGroup, key = { it.id }) { tx ->
                            AccountTransactionItem(
                                transaction = tx,
                                onClick = { onEditTransaction(tx) },
                                onEdit = { onEditTransaction(tx) },
                                onDelete = { transactionPendingDelete = tx }
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de Confirmación para Eliminar Cuenta
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = {
                    Text(
                        text = "¿Eliminar cuenta?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Se eliminará permanentemente '${account.name}' y sus registros asociados.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteAccountDialog = false
                            onDeleteAccount(account)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Diálogo de Confirmación para Eliminar Transacción
        val txToDelete = transactionPendingDelete
        if (txToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionPendingDelete = null },
                title = {
                    Text(
                        text = "¿Eliminar transacción?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "¿Deseas eliminar '${txToDelete.note.ifBlank { txToDelete.category?.name ?: "esta transacción" }}'? El balance de la cuenta se ajustará automáticamente.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            transactionPendingDelete = null
                            onDeleteTransaction(txToDelete)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionPendingDelete = null }) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Diálogo para Reasignar Transacciones a otra cuenta
        val eligibleAccounts = allAccounts.filter { it.id != account.id }
        if (showReassignDialog && eligibleAccounts.isNotEmpty()) {
            var selectedTargetAccountId by remember { mutableStateOf(eligibleAccounts.first().id) }

            AlertDialog(
                onDismissRequest = { showReassignDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DashboardIcons.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mover transacciones",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Transfiere todas las ${transactions.size} transacciones de '${account.name}' a otra cuenta. Los saldos de ambas cuentas se recalcularán de forma automática.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Selecciona la cuenta de destino:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            eligibleAccounts.forEach { targetAcc ->
                                val isSelected = targetAcc.id == selectedTargetAccountId
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedTargetAccountId = targetAcc.id }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = targetAcc.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = CurrencyFormatter.format(targetAcc.balance, targetAcc.currency),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = DashboardIcons.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReassignDialog = false
                            onReassignTransactions(selectedTargetAccountId)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Mover ${transactions.size} movimientos", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReassignDialog = false }) {
                        Text("Cancelar")
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

/**
 * Fila de transacción individual perteneciente a la cuenta bancaria.
 */
@Composable
private fun AccountTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryIcon = IconMapper.getIconForCategory(
        transaction.category?.iconName ?: "",
        transaction.type
    )

    val categoryColor = remember(transaction.category?.colorHex) {
        try {
            val hex = transaction.category?.colorHex ?: "#3B82F6"
            Color(
                red = hex.substring(1, 3).toInt(16) / 255f,
                green = hex.substring(3, 5).toInt(16) / 255f,
                blue = hex.substring(5, 7).toInt(16) / 255f
            )
        } catch (_: Exception) {
            Color(0xFF3B82F6)
        }
    }

    val isIncome = transaction.type == TransactionType.INCOME

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de categoría con fondo translúcido coloreado
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.note.ifBlank { transaction.category?.name ?: "Transacción" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${transaction.category?.name ?: "Sin categoría"} • ${DateFormatter.formatTime(transaction.timestamp)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${if (isIncome) "+ " else "- "}${CurrencyFormatter.format(transaction.amount, transaction.currency)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
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
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = DashboardIcons.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.70f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
