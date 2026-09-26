package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.dashboard.DashboardColors
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard

/**
 * Tarjeta principal de balance esmerilada correspondiente a la sección "Main Balance Card":
 * - Orbe lumínico sutil en el fondo
 * - Etiqueta "Total Balance"
 * - Monto financiero con tipografía display
 * - Botón "Add Funds" con gradiente y sombra estilizada
 */
@Composable
fun MainBalanceCard(
    modifier: Modifier = Modifier,
    balance: String = "$12,450.80",
    onAddFundsClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val glowAlpha = if (isDark) 0.25f else 0.12f
    val buttonGradientColors = if (isDark) {
        listOf(DashboardColors.InversePrimary, DashboardColors.SecondaryContainer)
    } else {
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    }
    val buttonShadowSpotColor = if (isDark) {
        DashboardColors.SecondaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }
    val buttonShadowAmbientColor = if (isDark) {
        DashboardColors.SecondaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    }

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(24.dp)
    ) {
        // Orbe sutil con gradiente radial detrás del texto de balance
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.32f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Etiqueta Total Balance
            Text(
                text = "Total Balance",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Monto en gran tamaño
            Text(
                text = balance,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Botón "Add Funds" con degradado y sombra adaptativa
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        spotColor = buttonShadowSpotColor,
                        ambientColor = buttonShadowAmbientColor
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = buttonGradientColors
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onAddFundsClick
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = DashboardIcons.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Funds",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
