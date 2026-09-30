package dev.ajvanegasv.kontio.presentation.accounts.components

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import dev.ajvanegasv.kontio.presentation.util.NumberInputFormatter
import dev.ajvanegasv.kontio.presentation.util.ThousandsSeparatorVisualTransformation

@Composable
fun AddAccountBottomSheet(
    onDismiss: () -> Unit,
    onSaveAccount: (name: String, type: AccountType, balance: Double, creditLimit: Double?, colorHex: String, cutoffDay: Int?, dueDay: Int?) -> Unit,
    accountToEdit: Account? = null,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()
    val colors = listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B", "#EC4899", "#1E293B")

    var name by remember(accountToEdit) { mutableStateOf(accountToEdit?.name ?: "") }
    var selectedType by remember(accountToEdit) { mutableStateOf(accountToEdit?.type ?: AccountType.SAVINGS) }
    var balanceString by remember(accountToEdit) {
        mutableStateOf(
            accountToEdit?.let {
                if (it.balance % 1.0 == 0.0) it.balance.toLong().toString() else it.balance.toString()
            } ?: "0"
        )
    }
    var creditLimitString by remember(accountToEdit) {
        mutableStateOf(
            accountToEdit?.creditLimit?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: "1000"
        )
    }
    var cutoffDayString by remember(accountToEdit) {
        mutableStateOf(accountToEdit?.cutoffDay?.toString() ?: "")
    }
    var dueDayString by remember(accountToEdit) {
        mutableStateOf(accountToEdit?.dueDay?.toString() ?: "")
    }
    var selectedColor by remember(accountToEdit) {
        mutableStateOf(accountToEdit?.colorHex ?: colors[0])
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
            modifier = Modifier
                .fillMaxWidth()
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (accountToEdit != null) "Editar Producto Bancario" else "Nuevo Producto Bancario",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la cuenta o tarjeta") },
                placeholder = { Text("Ej: Ahorros Bancolombia, Visa Infinite...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Tipo de Producto
            Text(
                text = "Tipo de producto:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            val types = listOf(
                AccountType.SAVINGS to "Ahorros",
                AccountType.CHECKING to "Corriente",
                AccountType.CREDIT_CARD to "T. Crédito",
                AccountType.CASH to "Efectivo",
                AccountType.DIGITAL_WALLET to "Billetera"
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(types) { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedType = type }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Saldo
            OutlinedTextField(
                value = balanceString,
                onValueChange = { balanceString = NumberInputFormatter.cleanNumericInput(it) },
                label = { Text(if (selectedType == AccountType.CREDIT_CARD) "Saldo consumido / Deuda actual" else "Saldo inicial disponible") },
                placeholder = { Text("0") },
                prefix = { Text("$ ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                visualTransformation = ThousandsSeparatorVisualTransformation(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            if (selectedType == AccountType.CREDIT_CARD) {
                Spacer(modifier = Modifier.height(12.dp))
                // Cupo total
                OutlinedTextField(
                    value = creditLimitString,
                    onValueChange = { creditLimitString = NumberInputFormatter.cleanNumericInput(it, allowNegative = false) },
                    label = { Text("Cupo límite de la tarjeta") },
                    placeholder = { Text("0") },
                    prefix = { Text("$ ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    visualTransformation = ThousandsSeparatorVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                // Fechas de corte y pago lado a lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cutoffDayString,
                        onValueChange = { cutoffDayString = it },
                        label = { Text("Día de corte") },
                        placeholder = { Text("1 - 31") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    OutlinedTextField(
                        value = dueDayString,
                        onValueChange = { dueDayString = it },
                        label = { Text("Día límite pago") },
                        placeholder = { Text("1 - 31") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selector de Color Glass
            Text(
                text = "Color de la tarjeta:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colors.forEach { hex ->
                    val color = Color(
                        red = hex.substring(1, 3).toInt(16) / 255f,
                        green = hex.substring(3, 5).toInt(16) / 255f,
                        blue = hex.substring(5, 7).toInt(16) / 255f
                    )
                    val isSelected = selectedColor == hex

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val bal = balanceString.replace(",", "").toDoubleOrNull() ?: 0.0
                    val limit = if (selectedType == AccountType.CREDIT_CARD) {
                        creditLimitString.replace(",", "").toDoubleOrNull() ?: 0.0
                    } else null
                    val cutoff = if (selectedType == AccountType.CREDIT_CARD) {
                        cutoffDayString.toIntOrNull()
                    } else null
                    val due = if (selectedType == AccountType.CREDIT_CARD) {
                        dueDayString.toIntOrNull()
                    } else null

                    onSaveAccount(name, selectedType, bal, limit, selectedColor, cutoff, due)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (accountToEdit != null) "Guardar Cambios" else "Crear Cuenta",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
