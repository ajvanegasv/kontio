package dev.ajvanegasv.kontio.presentation.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.transactions.TransactionCreationUiState
import dev.ajvanegasv.kontio.presentation.util.IconMapper

@Composable
fun AddTransactionBottomSheet(
    state: TransactionCreationUiState,
    onTypeSelect: (TransactionType) -> Unit,
    onNumberPadClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onAccountSelect: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    KontioGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        elevation = GlassTokens.DockElevation,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tirador superior del modal
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector Ingreso / Gasto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.3f else 0.7f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Píldora Gasto
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (state.type == TransactionType.EXPENSE)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.35f else 0.8f)
                            else Color.Transparent
                        )
                        .clickable { onTypeSelect(TransactionType.EXPENSE) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gasto (-)",
                        fontWeight = if (state.type == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Píldora Ingreso
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (state.type == TransactionType.INCOME)
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.35f else 0.8f)
                            else Color.Transparent
                        )
                        .clickable { onTypeSelect(TransactionType.INCOME) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ingreso (+)",
                        fontWeight = if (state.type == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                        color = if (state.type == TransactionType.INCOME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display del Monto
            Text(
                text = "$ ${state.amountString}",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = if (state.type == TransactionType.INCOME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Selector de Cuenta
            Text(
                text = "Cuenta de cargo:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.accounts) { account ->
                    val isSelected = account.id == state.selectedAccountId
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
                            .clickable { onAccountSelect(account.id) }
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

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Categoría
            Text(
                text = "Categoría:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories) { category ->
                    val isSelected = category.id == state.selectedCategoryId
                    val icon = IconMapper.getIconForCategory(category.iconName, category.type)

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onCategorySelect(category.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = category.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Campo de Nota u Observación
            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                placeholder = { Text("Nota o descripción (ej: Almuerzo, Uber...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Teclado Numérico Glassmorphism
            val keyRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "⌫")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keyRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f))
                                    .clickable {
                                        if (key == "⌫") onBackspaceClick() else onNumberPadClick(key)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Guardar con degradado
            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (state.isSubmitting) "Guardando..." else "Guardar Transacción",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
