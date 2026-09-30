package dev.ajvanegasv.kontio.presentation.budgets.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.categories.components.parseColorFromHex
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper

@Composable
fun BudgetItemCard(
    item: BudgetWithProgress,
    onPayClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()
    val budget = item.budget
    val category = item.category
    val categoryColor = category?.let { parseColorFromHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val iconVector = IconMapper.getIconForCategory(category?.iconName ?: "category", category?.type ?: TransactionType.EXPENSE)

    val progressFraction = (item.percentage).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "budgetProgress")

    val progressColor = when {
        item.isExceeded -> MaterialTheme.colorScheme.error
        item.percentage >= 0.80f -> Color(0xFFF59E0B) // Ámbar / Alerta
        else -> MaterialTheme.colorScheme.secondary // Verde / Cian saludable
    }

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabecera: Icono, Nombre, Categoría y Botones de acción rápida (Editar / Eliminar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.20f))
                            .border(1.dp, categoryColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = budget.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = category?.name ?: "Sin categoría",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Edit,
                            contentDescription = "Editar presupuesto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = DashboardIcons.Delete,
                            contentDescription = "Eliminar presupuesto",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Montos: Gastado de Límite y Porcentaje
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Gastado",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CurrencyFormatter.format(item.spentAmount, budget.currency),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "/ ${CurrencyFormatter.format(item.limitAmount, budget.currency)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val pctText = "${(item.percentage * 100).toInt()}%"
                Text(
                    text = pctText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de progreso esmerilada
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

            Spacer(modifier = Modifier.height(12.dp))

            // Chip de Estado y Botón "Pagar / Registrar Gasto"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip descriptivo de estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                item.isExceeded -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.40f else 0.70f)
                                item.percentage >= 0.80f -> Color(0xFFF59E0B).copy(alpha = 0.20f)
                                else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.30f else 0.60f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    val statusText = when {
                        item.isExceeded -> "¡Excedido por ${CurrencyFormatter.format(item.exceededAmount, budget.currency)}!"
                        item.percentage >= 0.80f -> "Alerta • Restan ${CurrencyFormatter.format(item.remainingAmount, budget.currency)}"
                        else -> "Restan ${CurrencyFormatter.format(item.remainingAmount, budget.currency)}"
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            item.isExceeded -> MaterialTheme.colorScheme.error
                            item.percentage >= 0.80f -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.secondary
                        }
                    )
                }

                // Botón destacado: Pagar / Registrar Gasto
                Button(
                    onClick = onPayClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.7f else 0.9f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = DashboardIcons.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pagar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (item.lastTransaction != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val lastTx = item.lastTransaction
                val txNote = if (lastTx.note.isNotBlank()) " • ${lastTx.note}" else ""
                Text(
                    text = "Último registro: ${CurrencyFormatter.format(lastTx.amount, budget.currency)}$txNote",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
