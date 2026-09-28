package dev.ajvanegasv.kontio.presentation.analytics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.CategorySpending
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.categories.components.parseColorFromHex
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper

/**
 * Lista ordenada de categorías de mayor a menor gasto con barras de progreso y selección sincronizada.
 */
@Composable
fun CategorySpendingList(
    spendings: List<CategorySpending>,
    selectedCategoryId: String?,
    onCategorySelect: (CategorySpending) -> Unit,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        spendings.forEachIndexed { index, item ->
            val isSelected = item.categoryId == selectedCategoryId
            val categoryColor = parseColorFromHex(item.colorHex)
            val iconVector = IconMapper.getIconForCategory(item.iconName, TransactionType.EXPENSE)

            val animatedProgress by animateFloatAsState(
                targetValue = (item.percentage / 100f).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600)
            )

            KontioGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = categoryColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                onClick = { onCategorySelect(item) }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icono circular con color temático
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(categoryColor.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = item.categoryName,
                                    tint = categoryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = item.categoryName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.transactionCount} ${if (item.transactionCount == 1) "movimiento" else "movimientos"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.format(item.totalAmount, currency),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(item.percentage * 10).toInt() / 10f}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = categoryColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barra horizontal de porcentaje de gasto
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(categoryColor)
                        )
                    }
                }
            }
        }
    }
}
