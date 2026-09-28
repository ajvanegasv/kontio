package dev.ajvanegasv.kontio.presentation.accounts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.accounts.components.RealisticBankCard
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier,
    onAddTransactionForAccount: (accountId: String, type: TransactionType) -> Unit = { _, _ -> },
    onEditTransaction: (Transaction) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var accountPendingDelete by remember { mutableStateOf<Account?>(null) }

    // Si hay una cuenta seleccionada para ver su detalle, mostrar AccountDetailScreen
    val selectedAcc = state.selectedAccount
    if (state.selectedAccountId != null && selectedAcc != null) {
        AccountDetailScreen(
            account = selectedAcc,
            transactions = state.selectedAccountTransactions,
            allAccounts = state.accounts,
            onBackClick = { viewModel.selectAccountForDetail(null) },
            onAddTransactionClick = onAddTransactionForAccount,
            onEditTransaction = onEditTransaction,
            onReassignTransactions = { toAccountId ->
                viewModel.reassignAccountTransactions(selectedAcc.id, toAccountId)
            },
            onDeleteAccount = { viewModel.deleteAccount(it.id) },
            onDeleteTransaction = { viewModel.deleteTransaction(it.id) },
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Encabezado y botón agregar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tus Cuentas",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tarjetas y productos bancarios",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.openAddAccount() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Add,
                            contentDescription = "Agregar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Nueva", fontSize = 13.sp)
                    }
                }
            }

            // 2. Resumen rápido de Activos vs Pasivos (Tarjetas)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Activos
                    KontioGlassCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Total Disponible",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(state.totalAssets),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Pasivos / Deuda en tarjetas
                    KontioGlassCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Deuda en Tarjetas",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyFormatter.format(state.totalLiabilities),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.totalLiabilities > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. Carrusel horizontal de tarjetas bancarias hiper-realistas
            item {
                Column {
                    Text(
                        text = "Tarjetas y Cuentas Activas",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Toca una tarjeta para ver sus movimientos y detalles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.accounts) { account ->
                        RealisticBankCard(
                            account = account,
                            onClick = { viewModel.selectAccountForDetail(account.id) }
                        )
                    }
                }
            }

            // 4. Lista detallada de cuentas
            item {
                Text(
                    text = "Detalle de Productos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(state.accounts) { account ->
                AccountRowItem(
                    account = account,
                    onClick = { viewModel.selectAccountForDetail(account.id) },
                    onDelete = { accountPendingDelete = account }
                )
            }
        }

        val accToDelete = accountPendingDelete
        if (accToDelete != null) {
            AlertDialog(
                onDismissRequest = { accountPendingDelete = null },
                title = {
                    Text(
                        text = "¿Eliminar cuenta?",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = "Se eliminará '${accToDelete.name}' de forma permanente.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteAccount(accToDelete.id)
                            accountPendingDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { accountPendingDelete = null }
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

/**
 * Delegado de compatibilidad para BankCardGlassItem hacia RealisticBankCard.
 */
@Composable
fun BankCardGlassItem(
    account: Account,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    RealisticBankCard(
        account = account,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun AccountRowItem(
    account: Account,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    KontioGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(16.dp)
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
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (account.type) {
                            AccountType.CREDIT_CARD -> DashboardIcons.CreditCard
                            AccountType.CASH -> DashboardIcons.AccountBalanceWallet
                            else -> DashboardIcons.Home
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = account.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (account.type) {
                            AccountType.CREDIT_CARD -> "Tarjeta de Crédito"
                            AccountType.SAVINGS -> "Cuenta de Ahorros"
                            AccountType.CHECKING -> "Cuenta Corriente"
                            AccountType.CASH -> "Efectivo"
                            AccountType.DIGITAL_WALLET -> "Billetera Digital"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = CurrencyFormatter.format(account.balance, account.currency),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (account.type == AccountType.CREDIT_CARD && account.balance > 0)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Delete,
                            contentDescription = "Eliminar cuenta",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
