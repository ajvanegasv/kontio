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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.presentation.accounts.components.AddAccountBottomSheet
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 90.dp, bottom = 110.dp, start = 20.dp, end = 20.dp),
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

            // 3. Carrusel horizontal de tarjetas bancarias estilo Glassmorphism
            item {
                Text(
                    text = "Tarjetas y Cuentas Activas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.accounts) { account ->
                        BankCardGlassItem(account = account)
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
                AccountRowItem(account = account)
            }
        }

        // Modal para agregar cuenta si está abierto
        if (state.isAddAccountOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { viewModel.closeAddAccount() },
                contentAlignment = Alignment.BottomCenter
            ) {
                AddAccountBottomSheet(
                    onDismiss = { viewModel.closeAddAccount() },
                    onSaveAccount = { name, type, balance, creditLimit, colorHex ->
                        viewModel.createAccount(
                            name = name,
                            type = type,
                            initialBalance = balance,
                            creditLimit = creditLimit,
                            colorHex = colorHex
                        )
                    },
                    errorMessage = state.errorMessage
                )
            }
        }
    }
}

@Composable
fun BankCardGlassItem(
    account: Account,
    modifier: Modifier = Modifier
) {
    val cardColor = try {
        Color(
            red = account.colorHex.substring(1, 3).toInt(16) / 255f,
            green = account.colorHex.substring(3, 5).toInt(16) / 255f,
            blue = account.colorHex.substring(5, 7).toInt(16) / 255f
        )
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    KontioGlassCard(
        modifier = modifier
            .width(280.dp)
            .height(170.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = GlassTokens.HeroCardElevation,
        contentPadding = PaddingValues(20.dp)
    ) {
        // Fondo con gradiente sutil del color de la tarjeta
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            cardColor.copy(alpha = 0.25f),
                            cardColor.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = when (account.type) {
                        AccountType.CREDIT_CARD -> "CRÉDITO"
                        AccountType.SAVINGS -> "AHORROS"
                        AccountType.CHECKING -> "CORRIENTE"
                        AccountType.CASH -> "EFECTIVO"
                        AccountType.DIGITAL_WALLET -> "BILLETERA"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Chip simulado
            Box(
                modifier = Modifier
                    .size(width = 34.dp, height = 24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFD4AF37).copy(alpha = 0.8f))
            )

            Column {
                Text(
                    text = if (account.type == AccountType.CREDIT_CARD) "Saldo adeudado" else "Saldo disponible",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.format(account.balance, account.currency),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (account.availableCredit != null) {
                    Text(
                        text = "Cupo libre: ${CurrencyFormatter.format(account.availableCredit!!, account.currency)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun AccountRowItem(
    account: Account,
    modifier: Modifier = Modifier
) {
    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = account.type.name.replace("_", " "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = CurrencyFormatter.format(account.balance, account.currency),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (account.type == AccountType.CREDIT_CARD && account.balance > 0)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
