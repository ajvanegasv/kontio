package dev.ajvanegasv.kontio.presentation.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.util.IconMapper

fun parseColorFromHex(hex: String, default: Color = Color(0xFF3B82F6)): Color {
    return try {
        val clean = hex.removePrefix("#")
        when (clean.length) {
            6 -> Color(
                red = clean.substring(0, 2).toInt(16) / 255f,
                green = clean.substring(2, 4).toInt(16) / 255f,
                blue = clean.substring(4, 6).toInt(16) / 255f
            )
            8 -> Color(
                alpha = clean.substring(0, 2).toInt(16) / 255f,
                red = clean.substring(2, 4).toInt(16) / 255f,
                green = clean.substring(4, 6).toInt(16) / 255f,
                blue = clean.substring(6, 8).toInt(16) / 255f
            )
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

@Composable
fun AddCategoryBottomSheet(
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, iconName: String, colorHex: String, type: TransactionType) -> Unit,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val paletteColors = remember {
        listOf(
            "#EF4444", // Rojo
            "#F59E0B", // Ámbar/Naranja
            "#10B981", // Esmeralda/Verde
            "#06B6D4", // Cian
            "#3B82F6", // Azul
            "#6366F1", // Índigo
            "#8B5CF6", // Púrpura
            "#EC4899"  // Rosa
        )
    }
    var selectedColorHex by remember { mutableStateOf(paletteColors[4]) } // Default blue

    val selectableIcons = remember { IconMapper.getSelectableIcons() }
    var selectedIconName by remember { mutableStateOf(selectableIcons[0].first) }

    val activeColor = parseColorFromHex(selectedColorHex)

    KontioGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding(),
        shape = RoundedCornerShape(
            topStart = GlassTokens.ModalCornerRadius,
            topEnd = GlassTokens.ModalCornerRadius
        ),
        style = { GlassTokens.modalStyle() },
        elevation = GlassTokens.ModalElevation,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tirador
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Nueva Categoría",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Selector de Tipo: Gasto vs Ingreso
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer.copy(
                            alpha = if (isDark) 0.35f else 0.60f
                        )
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Opción Gasto
                val isExpense = selectedType == TransactionType.EXPENSE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isExpense) MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.25f else 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isExpense) MaterialTheme.colorScheme.error else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedType = TransactionType.EXPENSE }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DashboardIcons.ArrowDownward,
                            contentDescription = null,
                            tint = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Gasto",
                            fontSize = 13.sp,
                            fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Normal,
                            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Opción Ingreso
                val isIncome = selectedType == TransactionType.INCOME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isIncome) MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.25f else 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = 1.dp,
                            color = if (isIncome) MaterialTheme.colorScheme.secondary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedType = TransactionType.INCOME }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = DashboardIcons.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ingreso",
                            fontSize = 13.sp,
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Normal,
                            color = if (isIncome) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Campo Nombre de Categoría
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la categoría") },
                placeholder = { Text("Ej: Mascotas, Gimnasio, Viajes...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Paleta de Colores
            Text(
                text = "Color de la categoría:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                paletteColors.forEach { hex ->
                    val color = parseColorFromHex(hex)
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColorHex = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = DashboardIcons.Check,
                                contentDescription = "Seleccionado",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Selector de Icono
            Text(
                text = "Icono representativo:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                selectableIcons.forEach { (iconKey, vector) ->
                    val isSelected = selectedIconName == iconKey

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) activeColor.copy(alpha = if (isDark) 0.35f else 0.25f)
                                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) activeColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedIconName = iconKey },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = vector,
                            contentDescription = iconKey,
                            tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Live Preview Chip/Card
            Text(
                text = "Vista previa:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val previewIconVector = IconMapper.getIconForCategory(selectedIconName, selectedType)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = previewIconVector,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = name.ifBlank { "Nombre de la categoría" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (name.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Categoría personalizada",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Badge Tipo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedType == TransactionType.INCOME) "Ingreso" else "Gasto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedType == TransactionType.INCOME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Botón de Creación
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSaveCategory(name.trim(), selectedIconName, selectedColorHex, selectedType)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Crear Categoría",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
