package dev.ajvanegasv.kontio.presentation.analytics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.CategorySpending
import dev.ajvanegasv.kontio.presentation.categories.components.parseColorFromHex
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Gráfico interactivo Donut / Pie Chart implementado en Canvas puro de Compose Multiplatform.
 * Permite selección por tap directo en las rebanadas o sincronizado con la lista de categorías.
 */
@Composable
fun DonutPieChart(
    spendings: List<CategorySpending>,
    selectedCategoryId: String?,
    onCategorySelect: (CategorySpending?) -> Unit,
    totalExpensesFormatted: String,
    currency: String = "USD",
    modifier: Modifier = Modifier,
    chartSize: Dp = 220.dp,
    strokeWidth: Dp = 28.dp,
    selectedStrokeWidth: Dp = 36.dp
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val selectedStrokeWidthPx = with(density) { selectedStrokeWidth.toPx() }

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(spendings) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )
    }

    val selectedCategory = remember(spendings, selectedCategoryId) {
        spendings.firstOrNull { it.categoryId == selectedCategoryId }
    }

    Box(
        modifier = modifier.size(chartSize),
        contentAlignment = Alignment.Center
    ) {
        val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

        Canvas(
            modifier = Modifier
                .size(chartSize)
                .pointerInput(spendings) {
                    detectTapGestures { tapOffset ->
                        if (spendings.isEmpty()) return@detectTapGestures

                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        val radius = size.width / 2f

                        // Verificar si el tap ocurrió dentro del anillo del donut
                        val innerRadius = radius - selectedStrokeWidthPx
                        if (distance in innerRadius..radius) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // Normalizar ángulo con 0° arriba a las 12:00 (-90°)
                            angle = (angle + 90f + 360f) % 360f

                            var currentStartAngle = 0f
                            var tappedCategory: CategorySpending? = null

                            for (item in spendings) {
                                val sweep = (item.percentage / 100f) * 360f
                                if (angle in currentStartAngle..(currentStartAngle + sweep)) {
                                    tappedCategory = item
                                    break
                                }
                                currentStartAngle += sweep
                            }

                            if (tappedCategory != null) {
                                if (tappedCategory.categoryId == selectedCategoryId) {
                                    onCategorySelect(null)
                                } else {
                                    onCategorySelect(tappedCategory)
                                }
                            }
                        } else {
                            // Tap en el centro deselecciona
                            onCategorySelect(null)
                        }
                    }
                }
        ) {
            val canvasRadius = size.width / 2f
            val basePadding = selectedStrokeWidthPx / 2f
            val arcSize = Size(
                width = size.width - selectedStrokeWidthPx,
                height = size.height - selectedStrokeWidthPx
            )
            val arcTopLeft = Offset(basePadding, basePadding)

            if (spendings.isEmpty()) {
                // Estado vacío: Anillo base neutro
                drawArc(
                    color = surfaceContainer.copy(alpha = 0.5f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            } else {
                var currentStartAngle = -90f
                val hasMultiple = spendings.size > 1

                spendings.forEach { spending ->
                    val sweep = (spending.percentage / 100f) * 360f * animProgress.value
                    val isSelected = spending.categoryId == selectedCategoryId
                    val currentStroke = if (isSelected) selectedStrokeWidthPx else strokeWidthPx
                    val catColor = parseColorFromHex(spending.colorHex)

                    // Margen entre rebanadas
                    val gap = if (hasMultiple && sweep > 4f) 2f else 0f
                    val effectiveSweep = (sweep - gap).coerceAtLeast(0.5f)

                    drawArc(
                        color = catColor,
                        startAngle = currentStartAngle + (gap / 2f),
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = currentStroke, cap = StrokeCap.Butt)
                    )

                    currentStartAngle += sweep
                }
            }
        }

        // Centro interactivo del Donut con información contextual
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedCategory == null) {
                Text(
                    text = "Gastos Totales",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = totalExpensesFormatted,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            } else {
                val catColor = parseColorFromHex(selectedCategory.colorHex)
                Text(
                    text = selectedCategory.categoryName,
                    fontSize = 12.sp,
                    color = catColor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.format(selectedCategory.totalAmount, currency),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "${(selectedCategory.percentage * 10).toInt() / 10f}% del total",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
