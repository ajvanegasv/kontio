package dev.ajvanegasv.kontio.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.ajvanegasv.kontio.MainActivity
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.CreditCardWidgetSummary
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import kotlinx.coroutines.flow.first

class CreditCardBalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = try {
            AppContainer.getCreditCardWidgetDataUseCase().first()
        } catch (_: Exception) {
            CreditCardWidgetSummary()
        }

        provideContent {
            CreditCardWidgetContent(summary)
        }
    }
}

@Composable
fun CreditCardWidgetContent(summary: CreditCardWidgetSummary) {
    val darkCardBackground = Color(0xFF0F172A) // Slate 900
    val innerBadgeBackground = Color(0xFF1E293B) // Slate 800
    val textPrimary = Color(0xFFF8FAFC)
    val textSecondary = Color(0xFF94A3B8)
    val accentCard = Color(0xFFA855F7) // Purple / Violet
    val accentCoral = Color(0xFFF43F5E) // Red / Rose
    val accentTeal = Color(0xFF10B981) // Green / Emerald

    val formattedDebt = CurrencyFormatter.format(summary.totalDebt, summary.currency)
    val formattedExpenses = CurrencyFormatter.format(summary.monthlyExpenses, summary.currency)
    val formattedAvailable = CurrencyFormatter.format(summary.availableCredit, summary.currency)
    val formattedLimit = CurrencyFormatter.format(summary.totalCreditLimit, summary.currency)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(darkCardBackground)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(MainActivity::class.java))
            .padding(16.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            // Header: Marca + Cantidad de tarjetas
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KONTIO • TARJETAS DE CRÉDITO",
                    style = TextStyle(
                        color = ColorProvider(accentCard),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                Text(
                    text = "${summary.cardsCount} tarjetas",
                    style = TextStyle(
                        color = ColorProvider(textSecondary),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            // Hero: Deuda Total
            Text(
                text = "Deuda en Tarjetas",
                style = TextStyle(
                    color = ColorProvider(textSecondary),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )

            Text(
                text = formattedDebt,
                style = TextStyle(
                    color = ColorProvider(if (summary.totalDebt > 0) accentCoral else textPrimary),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Sub-métricas: Gastos del mes, Cupo disponible y Límite total
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(innerBadgeBackground)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gastos este mes
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Gastos Mes",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = formattedExpenses,
                            style = TextStyle(
                                color = ColorProvider(accentCoral),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Cupo disponible
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Cupo Disp.",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = formattedAvailable,
                            style = TextStyle(
                                color = ColorProvider(accentTeal),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(6.dp))

                    // Cupo total
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Límite Total",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = formattedLimit,
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
