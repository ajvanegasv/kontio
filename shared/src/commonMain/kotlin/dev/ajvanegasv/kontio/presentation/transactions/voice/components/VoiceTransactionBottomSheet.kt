package dev.ajvanegasv.kontio.presentation.transactions.voice.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.designsystem.glass.GlassTokens
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.isKontioDarkTheme
import dev.ajvanegasv.kontio.presentation.transactions.voice.VoiceStep
import dev.ajvanegasv.kontio.presentation.transactions.voice.VoiceTransactionViewModel
import dev.ajvanegasv.kontio.presentation.util.DateFormatter
import dev.ajvanegasv.kontio.presentation.util.IconMapper
import dev.ajvanegasv.kontio.presentation.util.rememberSpeechRecognizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTransactionBottomSheet(
    viewModel: VoiceTransactionViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isKontioDarkTheme()
    val uiState by viewModel.uiState.collectAsState()
    val speechRecognizer = rememberSpeechRecognizer()

    // Texto dictado editable por el usuario
    var textInput by remember { mutableStateOf("") }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Sincronizar el texto del reconocedor de voz con el campo de texto
    LaunchedEffect(speechRecognizer.spokenText, speechRecognizer.partialText) {
        val currentSpoken = speechRecognizer.spokenText
        val partial = speechRecognizer.partialText
        if (partial.isNotBlank()) {
            textInput = if (currentSpoken.isNotBlank()) "$currentSpoken $partial" else partial
        } else if (currentSpoken.isNotBlank()) {
            textInput = currentSpoken
        }
    }

    // Auto-iniciar la escucha al abrir el paso DICTATING
    LaunchedEffect(uiState.step) {
        if (uiState.step == VoiceStep.DICTATING && !speechRecognizer.isListening && textInput.isBlank()) {
            speechRecognizer.requestPermissionAndStart()
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tirador superior del modal
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .clickable {
                        speechRecognizer.stopListening()
                        onDismiss()
                    }
            )

            Spacer(modifier = Modifier.height(14.dp))

            when (uiState.step) {
                VoiceStep.DICTATING -> {
                    DictatingView(
                        isDark = isDark,
                        textInput = textInput,
                        isListening = speechRecognizer.isListening,
                        rmsLevel = speechRecognizer.rmsLevel,
                        errorMessage = uiState.errorMessage ?: speechRecognizer.errorMessage,
                        onTextChange = {
                            textInput = it
                            speechRecognizer.updateSpokenText(it)
                        },
                        onToggleListening = {
                            if (speechRecognizer.isListening) {
                                speechRecognizer.stopListening()
                            } else {
                                speechRecognizer.requestPermissionAndStart()
                            }
                        },
                        onClearText = {
                            textInput = ""
                            speechRecognizer.reset()
                        },
                        onProcessWithAi = {
                            speechRecognizer.stopListening()
                            viewModel.processVoiceText(textInput)
                        },
                        onDismiss = {
                            speechRecognizer.stopListening()
                            onDismiss()
                        }
                    )
                }

                VoiceStep.ANALYZING -> {
                    AnalyzingView(isDark = isDark)
                }

                VoiceStep.PREVIEW_EDITABLE -> {
                    EditablePreviewView(
                        isDark = isDark,
                        uiState = uiState,
                        onTypeChange = { viewModel.setDraftType(it) },
                        onAmountChange = { viewModel.setDraftAmount(it) },
                        onAccountChange = { viewModel.setDraftAccount(it) },
                        onCategoryChange = { viewModel.setDraftCategory(it) },
                        onDateChange = { viewModel.setDraftDate(it) },
                        onNoteChange = { viewModel.setDraftNote(it) },
                        onRetryVoice = {
                            viewModel.retryDictation()
                            textInput = ""
                            speechRecognizer.reset()
                        },
                        onSubmit = {
                            viewModel.submitDraft(
                                onSuccess = {
                                    speechRecognizer.reset()
                                    onSuccess()
                                }
                            )
                        },
                        onOpenDatePicker = { showDatePickerDialog = true }
                    )
                }
            }
        }
    }

    // Diálogo selector de fecha nativo
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.draft.timestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            viewModel.setDraftDate(selectedMillis)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Aceptar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Vista de dictado en vivo con animación de ondas de micrófono y transcripción en tiempo real.
 */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DictatingView(
    isDark: Boolean,
    textInput: String,
    isListening: Boolean,
    rmsLevel: Float,
    errorMessage: String?,
    onTextChange: (String) -> Unit,
    onToggleListening: () -> Unit,
    onClearText: () -> Unit,
    onProcessWithAi: () -> Unit,
    onDismiss: () -> Unit
) {
    // Animación de pulso visual basada en el nivel de volumen o estado activo
    val animatedScale by animateFloatAsState(
        targetValue = if (isListening) 1f + (rmsLevel * 0.35f) else 1f,
        animationSpec = tween(150),
        label = "micPulseScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringAlpha"
    )

    Text(
        text = "Dictar con Voz e IA",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (isListening) "Escuchando... describe tu gasto o ingreso" else "Toca el micrófono para comenzar a dictar",
        fontSize = 13.sp,
        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Botón Central de Micrófono con ondas y pulsaciones dinámicas
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            // Anillo exterior de onda de audio
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(animatedScale * 1.15f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha))
            )
            // Anillo interior
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = ringAlpha * 1.5f))
            )
        }

        // Botón principal
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = if (isListening) {
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    )
                )
                .clickable { onToggleListening() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) DashboardIcons.Mic else DashboardIcons.MicOff,
                contentDescription = if (isListening) "Pausar micrófono" else "Iniciar micrófono",
                tint = if (isListening) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Tarjeta contenedora de la transcripción en tiempo real o campo editable
    OutlinedTextField(
        value = textInput,
        onValueChange = onTextChange,
        placeholder = {
            Text(
                text = "Ejemplo: \"Gasté 45 mil pesos en un almuerzo en Crepes pagado con Bancolombia\"",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 150.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.35f else 0.7f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    )

    // Chips de sugerencias rápidas
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Sugerencias de dictado:",
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(6.dp))

    val quickExamples = listOf(
        "Gasté 45 mil en almuerzo con Bancolombia",
        "Pagué 15000 de Uber en efectivo",
        "Ayer pagué 50 mil de supermercado",
        "Me pagaron 1.5 millones de salario"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickExamples) { example ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.4f else 0.7f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onTextChange(example) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = example,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Botones de acción
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onClearText,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            enabled = textInput.isNotBlank()
        ) {
            Text("Limpiar", fontSize = 14.sp)
        }

        Button(
            onClick = onProcessWithAi,
            modifier = Modifier.weight(1.5f),
            shape = RoundedCornerShape(14.dp),
            enabled = textInput.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DashboardIcons.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Procesar con IA", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Vista de carga con animación mientras Gemini IA procesa la voz y extrae la información.
 */
@Composable
private fun AnalyzingView(isDark: Boolean) {
    Spacer(modifier = Modifier.height(30.dp))

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(46.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Icon(
            imageVector = DashboardIcons.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Analizando con Inteligencia Artificial...",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Extrayendo monto, categoría y cuenta bancaria de tu dictado",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(30.dp))
}

/**
 * Vista previa editable donde el usuario puede ver los resultados de la IA, modificar cualquier campo
 * y confirmar manualmente el guardado.
 */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EditablePreviewView(
    isDark: Boolean,
    uiState: dev.ajvanegasv.kontio.presentation.transactions.voice.VoiceTransactionUiState,
    onTypeChange: (TransactionType) -> Unit,
    onAmountChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onNoteChange: (String) -> Unit,
    onRetryVoice: () -> Unit,
    onSubmit: () -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val draft = uiState.draft

    // Encabezado con badge de IA
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DashboardIcons.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Detectado por IA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Vista Previa del Movimiento",
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = "Puedes editar cualquier dato antes de guardar la transacción",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Cita del texto original dictado
    if (draft.rawVoiceText.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.3f else 0.6f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DashboardIcons.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "«${draft.rawVoiceText}»",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Selector Gasto vs Ingreso
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
                    if (draft.type == TransactionType.EXPENSE)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.35f else 0.8f)
                    else Color.Transparent
                )
                .clickable { onTypeChange(TransactionType.EXPENSE) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Gasto (-)",
                fontWeight = if (draft.type == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal,
                color = if (draft.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Píldora Ingreso
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (draft.type == TransactionType.INCOME)
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.35f else 0.8f)
                    else Color.Transparent
                )
                .clickable { onTypeChange(TransactionType.INCOME) }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ingreso (+)",
                fontWeight = if (draft.type == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal,
                color = if (draft.type == TransactionType.INCOME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Display del Monto editable
    Text(
        text = "Monto:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(
        value = draft.amountString,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        prefix = {
            Text(
                text = "$ ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (draft.type == TransactionType.INCOME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Selector de Cuenta
    Text(
        text = "Cuenta de cargo/depósito:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(6.dp))

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(uiState.accounts) { account ->
            val isSelected = account.id == draft.selectedAccountId
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
                    .clickable { onAccountChange(account.id) }
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

    val currentCategories = uiState.categories.filter { it.type == draft.type }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(currentCategories) { category ->
            val isSelected = category.id == draft.selectedCategoryId
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
                    .clickable { onCategoryChange(category.id) }
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

    // Selector de Fecha
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Fecha:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (isDark) 0.35f else 0.6f))
                .clickable { onOpenDatePicker() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = DashboardIcons.CalendarToday,
                contentDescription = "Cambiar fecha",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = DateFormatter.formatDateGroup(draft.timestamp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Píldoras rápidas de fecha: Hoy | Ayer
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val oneDayMillis = 86_400_000L

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f))
                .clickable { onDateChange(now) }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Hoy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (isDark) 0.25f else 0.5f))
                .clickable { onDateChange(now - oneDayMillis) }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Ayer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Campo Nota / Concepto
    Text(
        text = "Concepto o Nota:",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.align(Alignment.Start)
    )
    Spacer(modifier = Modifier.height(4.dp))

    OutlinedTextField(
        value = draft.note,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Ej. Almuerzo Crepes", fontSize = 13.sp) },
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )

    if (!uiState.errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = uiState.errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Botones inferiores: Dictar de nuevo y Guardar
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onRetryVoice,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = DashboardIcons.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reintentar", fontSize = 13.sp)
            }
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier.weight(1.6f),
            shape = RoundedCornerShape(14.dp),
            enabled = !uiState.isSubmitting && draft.numericAmount > 0.0,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (uiState.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = DashboardIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar Transacción", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
