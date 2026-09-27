package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard

/**
 * Sección de estadísticas rápidas ("Quick Stats"):
 * Dos tarjetas esmeriladas paralelas para Ingresos (Income) y Gastos (Expenses).
 */
@Composable
fun QuickStatsSection(
    modifier: Modifier = Modifier,
    incomeAmount: String = "$4,200.00",
    expensesAmount: String = "$1,840.50",
    onIncomeClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta Income
        QuickStatCard(
            modifier = Modifier.weight(1f),
            title = "INCOME",
            amount = incomeAmount,
            icon = DashboardIcons.ArrowUpward,
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = onIncomeClick
        )

        // Tarjeta Expenses
        QuickStatCard(
            modifier = Modifier.weight(1f),
            title = "EXPENSES",
            amount = expensesAmount,
            icon = DashboardIcons.ArrowDownward,
            accentColor = MaterialTheme.colorScheme.error,
            onClick = onExpensesClick
        )
    }
}

@Composable
fun QuickStatCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)? = null
) {
    KontioGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = amount,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
