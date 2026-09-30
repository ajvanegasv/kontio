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
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import dev.ajvanegasv.kontio.presentation.util.NumberInputFormatter
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.presentation.categories.components.parseColorFromHex
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import dev.ajvanegasv.kontio.presentation.util.IconMapper

@Composable
fun AddBudgetBottomSheet(
    editingBudget: Budget? = null,
    categories: List<Category>,
    onSave: (name: String, categoryId: String, limitAmount: Double, period: BudgetPeriod, currency: String, note: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()

    var name by remember(editingBudget) { mutableStateOf(editingBudget?.name ?: "") }
    var selectedCategoryId by remember(editingBudget) {
        mutableStateOf(editingBudget?.categoryId ?: categories.firstOrNull()?.id ?: "")
    }
    var amountString by remember(editingBudget) {
        val initialAmount = editingBudget?.limitAmount
        val str = if (initialAmount != null && initialAmount > 0) {
            if (initialAmount % 1.0 == 0.0) initialAmount.toLong().toString() else initialAmount.toString()
        } else "0"
        mutableStateOf(str)
    }
    var selectedPeriod by remember(editingBudget) {
        mutableStateOf(editingBudget?.period ?: BudgetPeriod.MONTHLY)
    }
    var note by remember(editingBudget) { mutableStateOf(editingBudget?.note ?: "") }

    val numericAmount = amountString.toDoubleOrNull() ?: 0.0

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

            Text(
                text = if (editingBudget != null) "Editar Presupuesto" else "Nuevo Presupuesto",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Define un límite máximo de gasto para esta categoría",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Display del Monto Límite
            Text(
                text = "$ ${NumberInputFormatter.formatNumberString(amountString)}",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Nombre (ej: Agua, Mercado, Ocio...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Categoría
            Text(
                text = "Categoría vinculada:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category.id == selectedCategoryId
                    val catColor = parseColorFromHex(category.colorHex)
                    val icon = IconMapper.getIconForCategory(category.iconName, category.type)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) catColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategoryId = category.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) catColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Período (Mensual por defecto)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetPeriod.entries.forEach { period ->
                    val isSelected = period == selectedPeriod
                    val label = when (period) {
                        BudgetPeriod.MONTHLY -> "Mensual"
                        BudgetPeriod.WEEKLY -> "Semanal"
                        BudgetPeriod.ANNUAL -> "Anual"
                        BudgetPeriod.ONE_TIME -> "Puntual"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else Color.Transparent
                            )
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teclado Numérico
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
                                    .height(44.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón Guardar
            Button(
                onClick = {
                    onSave(
                        name,
                        selectedCategoryId,
                        numericAmount,
                        selectedPeriod,
                        "USD",
                        note
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (editingBudget != null) "Guardar Cambios" else "Crear Presupuesto",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
