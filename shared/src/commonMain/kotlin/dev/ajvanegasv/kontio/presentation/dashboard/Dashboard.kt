package dev.ajvanegasv.kontio.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardBottomNavBar
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTab
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTopAppBar
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTransaction
import dev.ajvanegasv.kontio.presentation.dashboard.components.MainBalanceCard
import dev.ajvanegasv.kontio.presentation.dashboard.components.QuickStatsSection
import dev.ajvanegasv.kontio.presentation.dashboard.components.RecentTransactionsSection
import dev.ajvanegasv.kontio.presentation.dashboard.components.defaultDashboardTransactions
import dev.ajvanegasv.kontio.presentation.designsystem.glass.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

typealias Transaction = DashboardTransaction

/**
 * Pantalla principal del Dashboard de finanzas personales, replicando fielmente
 * el diseño y efectos visuales de cristal esmerilado (Glassmorphism) del HTML proporcionado.
 */
@Composable
@Preview
fun Dashboard(
    modifier: Modifier = Modifier,
    balance: String = "$12,450.80",
    incomeAmount: String = "$4,200.00",
    expensesAmount: String = "$1,840.50",
    transactions: List<DashboardTransaction> = remember { defaultDashboardTransactions() },
    onAddFundsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSeeAllTransactionsClick: () -> Unit = {},
    onTransactionClick: (DashboardTransaction) -> Unit = {}
) {
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf(DashboardTab.CARDS) }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DashboardColors.Background)
        ) {
            // Fondo ambiental Mesh-gradient idéntico al CSS del HTML
            DashboardMeshBackground()

            // Contenido desplazable (registrado en Haze para efecto de desenfoque en tiempo real)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    top = 84.dp,     // pt-24 (espacio para el Header fijo superior)
                    bottom = 100.dp, // pb-24 (espacio para la BottomNavBar fija inferior)
                    start = 20.dp,   // px-container-padding-mobile
                    end = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(32.dp) // space-y-stack-lg (32px)
            ) {
                // 1. Tarjeta de balance principal
                item {
                    MainBalanceCard(
                        balance = balance,
                        onAddFundsClick = onAddFundsClick
                    )
                }

                // 2. Sección de estadísticas rápidas (Ingresos y Gastos)
                item {
                    QuickStatsSection(
                        incomeAmount = incomeAmount,
                        expensesAmount = expensesAmount
                    )
                }

                // 3. Sección de transacciones recientes
                item {
                    RecentTransactionsSection(
                        transactions = transactions,
                        onSeeAllClick = onSeeAllTransactionsClick,
                        onTransactionClick = onTransactionClick
                    )
                }
            }

            // TopAppBar esmerilada fija superior
            DashboardTopAppBar(
                modifier = Modifier.align(Alignment.TopCenter),
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick
            )

            // BottomNavBar esmerilada fija inferior
            DashboardBottomNavBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

/**
 * Fondo ambiental con malla de gradientes radiales ("mesh-bg" del HTML):
 * radial-gradient at 0% 0% (hsla 253, 16%, 7%)
 * radial-gradient at 50% 0% (hsla 225, 39%, 30%, 0.5)
 * radial-gradient at 100% 0% (hsla 339, 49%, 30%, 0.5)
 */
@Composable
private fun DashboardMeshBackground(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Orbe superior izquierdo (hsla 253, 16%, 7%)
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DashboardColors.MeshIndigo.copy(alpha = 0.9f),
                            Color(0xFF1E1735).copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Orbe superior central (hsla 225, 39%, 30%, 0.5)
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DashboardColors.MeshCoolBlue.copy(alpha = 0.55f),
                            Color(0xFF172545).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Orbe superior derecho (hsla 339, 49%, 30%, 0.5)
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DashboardColors.MeshMagenta.copy(alpha = 0.50f),
                            Color(0xFF48172D).copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Orbe ambiental cian en la zona media-inferior para refracción del cristal
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            DashboardColors.SecondaryContainer.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}