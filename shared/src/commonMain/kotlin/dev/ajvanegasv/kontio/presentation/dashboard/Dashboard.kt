package dev.ajvanegasv.kontio.presentation.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassDock
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassTopAppBar
import dev.ajvanegasv.kontio.presentation.designsystem.glass.LocalHazeState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

data class Transaction(
    val id: String,
    val title: String,
    val category: String,
    val emoji: String,
    val date: String,
    val amount: String,
    val isIncome: Boolean
)

@Composable
@Preview
fun Dashboard() {
    val hazeState = remember { HazeState() }
    var selectedDockTab by remember { mutableStateOf(0) }

    val transactions = remember {
        listOf(
            Transaction("1", "Nómina Tech Inc", "Ingresos", "💼", "Hoy, 14:30", "+$4,250.00", true),
            Transaction("2", "Supermercado Orgánico", "Alimentación", "🛒", "Hoy, 11:15", "-$128.40", false),
            Transaction("3", "Cafetería Especialidad", "Ocio", "☕", "Ayer, 17:40", "-$4.50", false),
            Transaction("4", "Dividendos Fondos Indexados", "Inversión", "📈", "Ayer, 09:00", "+$215.30", true),
            Transaction("5", "Suscripción Cloud Server", "Servicios", "☁️", "28 Ago", "-$34.99", false),
            Transaction("6", "Gimnasio & Bienestar", "Salud", "⚡", "26 Ago", "-$65.00", false),
            Transaction("7", "Cena Restaurante", "Restauración", "🍽️", "24 Ago", "-$82.50", false)
        )
    }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0D1117)) // Fondo base profundo para contraste glassmorphic
        ) {
            // Orbes lumínicos ambientales de fondo (crean las refracciones de color en el cristal)
            Box(
                modifier = Modifier
                    .size(340.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.35f), // Indigo vibrante
                                Color(0xFF3B82F6).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.CenterEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = 0.28f), // Esmeralda financiero
                                Color(0xFF06B6D4).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Contenido desplazable registrado como hazeSource
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    top = 88.dp, // Espacio para la TopAppBar fija
                    bottom = 110.dp, // Espacio para el Dock flotante
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Tarjeta Principal de Balance Financiero
                    KontioGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Patrimonio Total",
                                    color = Color.White.copy(alpha = 0.70f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "+14.2% este mes",
                                        color = Color(0xFF34D399),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "$52,840.65",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Desglose Ingresos / Gastos con efecto de cristal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Métrica Ingresos
                                KontioGlassCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Ingresos",
                                            color = Color.White.copy(alpha = 0.60f),
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "+$6,450.00",
                                            color = Color(0xFF34D399),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Métrica Gastos
                                KontioGlassCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Gastos",
                                            color = Color.White.copy(alpha = 0.60f),
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "-$1,824.30",
                                            color = Color(0xFFF87171),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Acciones rápidas en chips esmerilados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val actions = listOf("Enviar" to "↗️", "Recibir" to "↙️", "Tarjetas" to "💳", "Informes" to "📊")
                        actions.forEach { (name, icon) ->
                            KontioGlassCard(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = name,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movimientos Recientes",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ver todos",
                            color = Color(0xFF818CF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Lista de transacciones con tarjetas esmeriladas individuales
                items(transactions, key = { it.id }) { tx ->
                    KontioGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(GlassTokens.CardCornerRadius),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Badge esmerilado de categoría
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = tx.emoji, fontSize = 20.sp)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.title,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${tx.category} • ${tx.date}",
                                    color = Color.White.copy(alpha = 0.50f),
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = tx.amount,
                                color = if (tx.isIncome) Color(0xFF34D399) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Barra Superior de Cristal fijada (desenfoca los elementos que scrollean debajo)
            KontioGlassTopAppBar(
                modifier = Modifier.align(Alignment.TopCenter),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🐻", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Kontio",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔔", fontSize = 16.sp)
                    }
                }
            )

            // Dock Inferior Flotante de Cristal (estilo píldora moderna)
            KontioGlassDock(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                val dockTabs = listOf("Inicio" to "🏠", "Analítica" to "📊", "Billetera" to "💳", "Ajustes" to "⚙️")
                dockTabs.forEachIndexed { index, (label, icon) ->
                    val isSelected = selectedDockTab == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}