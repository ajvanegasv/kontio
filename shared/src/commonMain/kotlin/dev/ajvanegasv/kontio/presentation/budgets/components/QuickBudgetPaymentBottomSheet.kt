package dev.ajvanegasv.kontio.presentation.budgets.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.presentation.util.NumberInputFormatter
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.categories.components.parseColorFromHex
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper

@Composable
fun QuickBudgetPaymentBottomSheet(
    budgetProgress: BudgetWithProgress,
    accounts: List<Account>,
    isSubmitting: Boolean,
    onConfirmPayment: (amount: Double, accountId: String, dateMillis: Long, note: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()
    val budget = budgetProgress.budget
    val category = budgetProgress.category
    val categoryColor = category?.let { parseColorFromHex(it.colorHex) } ?: MaterialTheme.colorScheme.primary
    val iconVector = IconMapper.getIconForCategory(category?.iconName ?: "category", category?.type ?: TransactionType.EXPENSE)

    var amountString by remember { mutableStateOf("0") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var note by remember { mutableStateOf("Pago de presupuesto: ${budget.name}") }
    val timestamp = remember { kotlin.time.Clock.System.now().toEpochMilliseconds() }

    val numericAmount = amountString.toDoubleOrNull() ?: 0.0

    // Simulación del impacto en tiempo real
    val projectedTotalSpent = budgetProgress.spentAmount + numericAmount
    val willExceed = projectedTotalSpent > budget.limitAmount
    val projectedExceededAmount = if (willExceed) projectedTotalSpent - budget.limitAmount else 0.0
    val projectedRemainingAmount = if (!willExceed) budget.limitAmount - projectedTotalSpent else 0.0

    fun onNumberPadClick(digit: String) {
        if (digit == ".") {
            if (!amountString.contains(".")) {
                amountString = "$amountString."
            }
            return
        }
        if (amountString == "0") {
            amountString = digit
        } else {
            val dotIndex = amountString.indexOf(".")
            if (dotIndex != -1 && amountString.length - dotIndex > 2) return
            amountString += digit
        }
    }

    fun onBackspaceClick() {
        if (amountString.length <= 1) {
            amountString = "0"
        } else {
            amountString = amountString.dropLast(1)
        }
    }

    KontioGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        shape = RoundedCornerShape(
            topStart = GlassTokens.ModalCornerRadius,
            topEnd = GlassTokens.ModalCornerRadius
        ),
        style = { GlassTokens.modalStyle() },
        elevation = GlassTokens.ModalElevation,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tirador superior
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Resumen del Presupuesto objetivo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.35f else 0.65f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = budget.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Límite: ${CurrencyFormatter.format(budget.limitAmount, budget.currency)} • Actual: ${CurrencyFormatter.format(budgetProgress.spentAmount, budget.currency)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display del Monto a Pagar
            Text(
                text = "Monto pagado:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$ ${NumberInputFormatter.formatNumberString(amountString)}",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = if (willExceed && numericAmount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // BANNER DINÁMICO DE IMPACTO EN TIEMPO REAL
            if (numericAmount > 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (willExceed) MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.35f else 0.70f)
                            else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.30f else 0.65f)
                        )
                        .border(
                            1.dp,
                            if (willExceed) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (willExceed) "⚠️" else "✓",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (willExceed) "¡Excederás el presupuesto fijado!"
                                else "Dentro del presupuesto planeado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (willExceed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (willExceed)
                                    "Total gastado pasará a ${CurrencyFormatter.format(projectedTotalSpent, budget.currency)} (excedido por ${CurrencyFormatter.format(projectedExceededAmount, budget.currency)})"
                                else
                                    "Total gastado pasará a ${CurrencyFormatter.format(projectedTotalSpent, budget.currency)}. Te restarán ${CurrencyFormatter.format(projectedRemainingAmount, budget.currency)} disponibles.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Cuenta de Cargo
            Text(
                text = "Cuenta de pago:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts) { account ->
                    val isSelected = account.id == selectedAccountId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedAccountId = account.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = account.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Campo de Nota
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Nota o descripción de la transacción") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Teclado Numérico Glassmorphism
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "⌫")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keyRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.20f else 0.45f))
                                    .clickable {
                                        if (key == "⌫") onBackspaceClick() else onNumberPadClick(key)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón Confirmar Pago
            Button(
                onClick = {
                    onConfirmPayment(numericAmount, selectedAccountId, timestamp, note)
                },
                enabled = !isSubmitting && numericAmount > 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (willExceed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isSubmitting) "Procesando pago..."
                    else if (willExceed) "Confirmar Pago (Con Excedente)"
                    else "Confirmar Pago",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
