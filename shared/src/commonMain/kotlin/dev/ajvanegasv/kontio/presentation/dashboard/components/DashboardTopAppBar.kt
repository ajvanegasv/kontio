package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.kontioGlass
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

/**
 * Barra superior esmerilada fija correspondiente al `<header>` del HTML:
 * - Avatar de usuario a la izquierda con borde translúcido
 * - Título central "Wallet" en color Primary
 * - Botón de notificaciones a la derecha
 */
@Composable
fun DashboardTopAppBar(
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val bottomBorderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .kontioGlass(
                shape = RectangleShape,
                style = {
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    HazeStyle(
                        backgroundColor = surfaceColor.copy(alpha = if (isDark) 0.10f else 0.40f),
                        blurRadius = 24.dp,
                        tints = listOf(HazeTint(color = surfaceColor.copy(alpha = if (isDark) 0.20f else 0.50f))),
                        noiseFactor = GlassTokens.NoiseFactor
                    )
                },
                borderBrush = null
            )
            .drawBehind {
                // Borde inferior sutil (border-b)
                drawLine(
                    color = bottomBorderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        // Avatar izquierdo
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.60f), CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2E3E6B),
                            Color(0xFF722744),
                            Color(0xFF6366F1)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onProfileClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DashboardIcons.Person,
                contentDescription = "User Profile",
                tint = Color.White.copy(alpha = 0.90f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Título central "Wallet"
        Text(
            text = "Wallet",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Botón de notificaciones derecho
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, radius = 20.dp),
                    onClick = onNotificationClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = DashboardIcons.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
