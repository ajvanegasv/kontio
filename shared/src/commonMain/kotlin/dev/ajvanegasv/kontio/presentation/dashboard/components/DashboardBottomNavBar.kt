package dev.ajvanegasv.kontio.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.dashboard.DashboardColors
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.kontioGlass
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

enum class DashboardTab {
    HOME,
    STATS,
    ADD,
    CARDS,
    PROFILE
}

/**
 * Barra de navegación inferior esmerilada fija correspondiente al `<nav>` del HTML:
 * - Ancho completo con esquinas superiores redondeadas (rounded-t-lg)
 * - Efecto de cristal esmerilado profundo (backdrop-blur-2xl)
 * - Borde superior especular translúcido (border-t border-white/20)
 * - Píldora activa destacada para la pestaña seleccionada (Cards por defecto)
 */
@Composable
fun DashboardBottomNavBar(
    selectedTab: DashboardTab = DashboardTab.CARDS,
    onTabSelected: (DashboardTab) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .kontioGlass(
                shape = navShape,
                style = {
                    HazeStyle(
                        backgroundColor = DashboardColors.SurfaceContainer.copy(alpha = 0.15f),
                        blurRadius = 26.dp,
                        tints = listOf(HazeTint(color = DashboardColors.SurfaceContainer.copy(alpha = 0.25f))),
                        noiseFactor = GlassTokens.NoiseFactor
                    )
                },
                borderBrush = null
            )
            .drawBehind {
                // Borde superior especular (border-t border-white/20)
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            DashboardNavItem(
                icon = DashboardIcons.Home,
                label = "Home",
                isSelected = selectedTab == DashboardTab.HOME,
                onClick = { onTabSelected(DashboardTab.HOME) }
            )

            // Stats
            DashboardNavItem(
                icon = DashboardIcons.Leaderboard,
                label = "Stats",
                isSelected = selectedTab == DashboardTab.STATS,
                onClick = { onTabSelected(DashboardTab.STATS) }
            )

            // Botón central Add (+)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 24.dp),
                        onClick = { onTabSelected(DashboardTab.ADD) }
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DashboardIcons.AddCircle,
                    contentDescription = "Add",
                    tint = DashboardColors.Primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            // Cards (Activo por defecto)
            DashboardNavItem(
                icon = DashboardIcons.CreditCard,
                label = "Cards",
                isSelected = selectedTab == DashboardTab.CARDS,
                onClick = { onTabSelected(DashboardTab.CARDS) }
            )

            // Profile
            DashboardNavItem(
                icon = DashboardIcons.Person,
                label = "Profile",
                isSelected = selectedTab == DashboardTab.PROFILE,
                onClick = { onTabSelected(DashboardTab.PROFILE) }
            )
        }
    }
}

@Composable
private fun DashboardNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) DashboardColors.Secondary else DashboardColors.Outline

    val containerModifier = if (isSelected) {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardColors.SecondaryContainer.copy(alpha = 0.20f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    } else {
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    }

    Column(
        modifier = containerModifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
