package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard

/**
 * Modelo de datos para las transacciones del Dashboard.
 */
data class DashboardTransaction(
    val id: String,
    val title: String,
    val category: String,
    val amount: String,
    val isIncome: Boolean,
    val icon: ImageVector,
    val iconBgColor: Color = Color.Unspecified,
    val iconBorderColor: Color = Color.Unspecified,
    val iconTintColor: Color = Color.Unspecified
)

/**
 * Lista por defecto correspondiente a las 4 transacciones del diseño.
 */
fun defaultDashboardTransactions(): List<DashboardTransaction> {
    return listOf(
        DashboardTransaction(
            id = "tx-1",
            title = "Netflix",
            category = "Entertainment",
            amount = "-$15.99",
            isIncome = false,
            icon = DashboardIcons.LiveTv
        ),
        DashboardTransaction(
            id = "tx-2",
            title = "Grocery Store",
            category = "Food",
            amount = "-$84.20",
            isIncome = false,
            icon = DashboardIcons.ShoppingCart
        ),
        DashboardTransaction(
            id = "tx-3",
            title = "Salary Deposit",
            category = "Income",
            amount = "+$2,100.00",
            isIncome = true,
            icon = DashboardIcons.AccountBalanceWallet
        ),
        DashboardTransaction(
            id = "tx-4",
            title = "Coffee Shop",
            category = "Food",
            amount = "-$4.50",
            isIncome = false,
            icon = DashboardIcons.LocalCafe
        )
    )
}

/**
 * Sección "Recent Transactions" con encabezado y lista de tarjetas esmeriladas individuales.
 */
@Composable
fun RecentTransactionsSection(
    modifier: Modifier = Modifier,
    transactions: List<DashboardTransaction> = defaultDashboardTransactions(),
    onSeeAllClick: () -> Unit = {},
    onTransactionClick: (DashboardTransaction) -> Unit = {},
    onDeleteTransaction: ((DashboardTransaction) -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Encabezado de sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "See All",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onSeeAllClick
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Lista de transacciones con espaciado stack-sm (8.dp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transactions.forEach { transaction ->
                TransactionCardItem(
                    transaction = transaction,
                    onClick = { onTransactionClick(transaction) },
                    onDeleteTransaction = onDeleteTransaction
                )
            }
        }
    }
}

@Composable
fun TransactionCardItem(
    transaction: DashboardTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteTransaction: ((DashboardTransaction) -> Unit)? = null
) {
    val isDark = isKontioDarkTheme()
    val isTertiary = transaction.category.equals("Entertainment", ignoreCase = true)

    // Insignias dinámicas de iconos adaptativas según el tema
    val badgeBgColor = if (transaction.iconBgColor != Color.Unspecified) {
        transaction.iconBgColor
    } else if (isDark) {
        if (isTertiary) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.20f)
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.20f)
    } else {
        if (isTertiary) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.60f)
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.60f)
    }

    val badgeBorderColor = if (transaction.iconBorderColor != Color.Unspecified) {
        transaction.iconBorderColor
    } else if (isDark) {
        if (isTertiary) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.30f)
        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
    } else {
        if (isTertiary) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
    }

    val badgeTintColor = if (transaction.iconTintColor != Color.Unspecified) {
        transaction.iconTintColor
    } else if (isDark) {
        if (isTertiary) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.secondary
    } else {
        if (isTertiary) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.secondary
    }

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
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
                // Contenedor circular con borde y color temático adaptativo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(badgeBgColor)
                        .border(1.dp, badgeBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = transaction.icon,
                        contentDescription = transaction.title,
                        tint = badgeTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = transaction.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Monto (Secondary para ingresos positivos, OnSurface para negativos)
                Text(
                    text = transaction.amount,
                    color = if (transaction.isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                if (onDeleteTransaction != null) {
                    IconButton(
                        onClick = { onDeleteTransaction(transaction) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Delete,
                            contentDescription = "Eliminar transacción",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
