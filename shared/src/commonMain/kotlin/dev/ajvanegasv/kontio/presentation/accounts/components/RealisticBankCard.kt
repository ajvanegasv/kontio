package dev.ajvanegasv.kontio.presentation.accounts.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter

/**
 * Componente de Tarjeta Bancaria Realista para Kontio.
 * Simula el aspecto físico de una tarjeta bancaria moderna (material pulido,
 * reflejos de luz especular, chip EMV metálico, guilloche de seguridad y detalles de titular/saldo).
 */
@Composable
fun RealisticBankCard(
    account: Account,
    modifier: Modifier = Modifier,
    isHero: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val cardColor = remember(account.colorHex) {
        try {
            Color(
                red = account.colorHex.substring(1, 3).toInt(16) / 255f,
                green = account.colorHex.substring(3, 5).toInt(16) / 255f,
                blue = account.colorHex.substring(5, 7).toInt(16) / 255f
            )
        } catch (_: Exception) {
            when (account.type) {
                AccountType.CREDIT_CARD -> Color(0xFF1E293B)
                AccountType.SAVINGS -> Color(0xFF0F766E)
                AccountType.CHECKING -> Color(0xFF1E40AF)
                AccountType.DIGITAL_WALLET -> Color(0xFF6B21A8)
                AccountType.CASH -> Color(0xFF15803D)
            }
        }
    }

    // Generar paleta de tonos para el cuerpo físico de la tarjeta
    val darkBase = remember(cardColor) {
        Color(
            red = (cardColor.red * 0.35f).coerceIn(0f, 1f),
            green = (cardColor.green * 0.35f).coerceIn(0f, 1f),
            blue = (cardColor.blue * 0.35f).coerceIn(0f, 1f)
        )
    }
    val midTone = remember(cardColor) {
        Color(
            red = (cardColor.red * 0.75f).coerceIn(0f, 1f),
            green = (cardColor.green * 0.75f).coerceIn(0f, 1f),
            blue = (cardColor.blue * 0.75f).coerceIn(0f, 1f)
        )
    }
    val highlightTone = remember(cardColor) {
        Color(
            red = (cardColor.red * 1.15f).coerceIn(0f, 1f),
            green = (cardColor.green * 1.15f).coerceIn(0f, 1f),
            blue = (cardColor.blue * 1.15f).coerceIn(0f, 1f)
        )
    }

    val cardShape = RoundedCornerShape(22.dp)
    val cardWidth = if (isHero) Modifier.fillMaxWidth() else Modifier.width(315.dp)
    val cardHeight = if (isHero) 210.dp else 190.dp

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(bounded = true, color = Color.White.copy(alpha = 0.25f)),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(cardWidth)
            .height(cardHeight)
            .shadow(
                elevation = if (isHero) 16.dp else 10.dp,
                shape = cardShape,
                spotColor = cardColor.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(cardShape)
            .then(clickableModifier),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    cardColor.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.12f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Fondo base de gradiente multi-fase lujoso
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                darkBase,
                                midTone,
                                cardColor,
                                highlightTone
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )

            // 2. Patrón de seguridad de guilloche / ondas bancarias sutiles
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Ondas decorativas de seguridad con transparencia sutil
                val wavePath1 = Path().apply {
                    moveTo(w * 0.45f, 0f)
                    cubicTo(w * 0.65f, h * 0.3f, w * 0.4f, h * 0.7f, w * 0.9f, h)
                    lineTo(w, h)
                    lineTo(w, 0f)
                    close()
                }
                drawPath(
                    path = wavePath1,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )

                // Círculos concéntricos de seguridad
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.2f),
                        radius = w * 0.45f
                    ),
                    center = Offset(w * 0.85f, h * 0.2f),
                    radius = w * 0.45f
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.035f),
                    center = Offset(w * 0.85f, h * 0.85f),
                    radius = w * 0.35f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // 3. Brillo especular diagonal (simulación de laminado de plástico/metal)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.03f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )

            // 4. Contenido interactivo y estructurado de la tarjeta
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Nombre de Institución y Tipo de Tarjeta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.name.uppercase(),
                        fontSize = if (isHero) 15.sp else 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Badge de Tipo de Cuenta
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (account.type) {
                                AccountType.CREDIT_CARD -> "CRÉDITO"
                                AccountType.SAVINGS -> "AHORROS"
                                AccountType.CHECKING -> "CORRIENTE"
                                AccountType.CASH -> "EFECTIVO"
                                AccountType.DIGITAL_WALLET -> "BILLETERA"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Zona Central: Chip EMV Metálico y Saldo / Deuda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chip EMV físico realista
                    EmvMicrochip()

                    // Visualización de Saldo o Deuda
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (account.type == AccountType.CREDIT_CARD) "DEUDA ACTUAL" else "DISPONIBLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.75f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = CurrencyFormatter.format(account.balance, account.currency),
                            fontSize = if (isHero) 22.sp else 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Barra de límite de crédito utilizada si aplica
                val limit = account.creditLimit
                if (account.type == AccountType.CREDIT_CARD && limit != null && limit > 0) {
                    val usedRatio = (account.balance / limit).toFloat().coerceIn(0f, 1f)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cupo libre: ${CurrencyFormatter.format(account.availableCredit ?: 0.0, account.currency)}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Límite: ${CurrencyFormatter.format(limit, account.currency)}",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { usedRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = if (usedRatio > 0.8f) Color(0xFFEF4444) else Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.20f)
                        )
                    }
                }

                // Footer: Titular y Vigencia / Corte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TITULAR",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = account.name.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (account.type == AccountType.CREDIT_CARD) "CORTE/PAGO" else "VENCE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.65f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (account.type == AccountType.CREDIT_CARD)
                                "${account.cutoffDay ?: 15}/${account.dueDay ?: 5}"
                            else
                                "12/29",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Chip EMV dorado metálico de contacto físico con divisiones de pad de circuito.
 */
@Composable
private fun EmvMicrochip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE082),
                        Color(0xFFFFD54F),
                        Color(0xFFFFB300),
                        Color(0xFFE6C875)
                    )
                )
            )
            .padding(1.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeColor = Color(0xFF8D6E63).copy(alpha = 0.65f)
            val strokeWidth = 1.dp.toPx()

            // Línea divisoria horizontal central
            drawLine(
                color = strokeColor,
                start = Offset(0f, h * 0.5f),
                end = Offset(w, h * 0.5f),
                strokeWidth = strokeWidth
            )

            // Líneas divisorias verticales
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.32f, 0f),
                end = Offset(w * 0.32f, h),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.68f, 0f),
                end = Offset(w * 0.68f, h),
                strokeWidth = strokeWidth
            )

            // Núcleo central redondeado del chip
            drawRoundRect(
                color = strokeColor,
                topLeft = Offset(w * 0.32f, h * 0.28f),
                size = Size(w * 0.36f, h * 0.44f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
