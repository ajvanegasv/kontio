package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import dev.ajvanegasv.kontio.presentation.dashboard.DashboardColors
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
    val iconBgColor: Color,
    val iconBorderColor: Color,
    val iconTintColor: Color
)

/**
 * Lista por defecto correspondiente a las 4 transacciones del HTML.
 */
fun defaultDashboardTransactions(): List<DashboardTransaction> {
    return listOf(
        DashboardTransaction(
            id = "tx-1",
            title = "Netflix",
            category = "Entertainment",
            amount = "-$15.99",
            isIncome = false,
            icon = DashboardIcons.LiveTv,
            iconBgColor = DashboardColors.TertiaryContainer.copy(alpha = 0.20f),
            iconBorderColor = DashboardColors.TertiaryContainer.copy(alpha = 0.30f),
            iconTintColor = DashboardColors.TertiaryContainer
        ),
        DashboardTransaction(
            id = "tx-2",
            title = "Grocery Store",
            category = "Food",
            amount = "-$84.20",
            isIncome = false,
            icon = DashboardIcons.ShoppingCart,
            iconBgColor = DashboardColors.SecondaryContainer.copy(alpha = 0.20f),
            iconBorderColor = DashboardColors.Secondary.copy(alpha = 0.30f),
            iconTintColor = DashboardColors.Secondary
        ),
        DashboardTransaction(
            id = "tx-3",
            title = "Salary Deposit",
            category = "Income",
            amount = "+$2,100.00",
            isIncome = true,
            icon = DashboardIcons.AccountBalanceWallet,
            iconBgColor = DashboardColors.Secondary.copy(alpha = 0.20f),
            iconBorderColor = DashboardColors.Secondary.copy(alpha = 0.30f),
            iconTintColor = DashboardColors.Secondary
        ),
        DashboardTransaction(
            id = "tx-4",
            title = "Coffee Shop",
            category = "Food",
            amount = "-$4.50",
            isIncome = false,
            icon = DashboardIcons.LocalCafe,
            iconBgColor = DashboardColors.SecondaryContainer.copy(alpha = 0.20f),
            iconBorderColor = DashboardColors.Secondary.copy(alpha = 0.30f),
            iconTintColor = DashboardColors.Secondary
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
    onTransactionClick: (DashboardTransaction) -> Unit = {}
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
                color = DashboardColors.OnSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "See All",
                color = DashboardColors.Primary,
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
                    onClick = { onTransactionClick(transaction) }
                )
            }
        }
    }
}

@Composable
fun TransactionCardItem(
    transaction: DashboardTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contenedor circular con borde y color temático
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(transaction.iconBgColor)
                        .border(1.dp, transaction.iconBorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = transaction.icon,
                        contentDescription = transaction.title,
                        tint = transaction.iconTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = transaction.title,
                        color = DashboardColors.OnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.category,
                        color = DashboardColors.OnSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            // Monto (Secondary para ingresos positivos, OnSurface para negativos)
            Text(
                text = transaction.amount,
                color = if (transaction.isIncome) DashboardColors.Secondary else DashboardColors.OnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
