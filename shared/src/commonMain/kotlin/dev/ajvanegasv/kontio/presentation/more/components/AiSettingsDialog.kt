package dev.ajvanegasv.kontio.presentation.more.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import kotlinx.coroutines.launch

@Composable
fun AiSettingsDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    currentApiKey: String?,
    currentModel: String,
    isAiEnabled: Boolean,
    onSave: (apiKey: String, model: String, enabled: Boolean) -> Unit,
    onDisconnect: () -> Unit
) {
    if (!isOpen) return

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val hasExistingKey = !currentApiKey.isNullOrBlank()
    var newApiKeyInput by remember { mutableStateOf("") }
    var selectedModel by remember(currentModel) {
        mutableStateOf(currentModel.removePrefix("models/").trim().ifBlank { "gemini-3.8-flash" })
    }
    var enabledState by remember(isAiEnabled) { mutableStateOf(isAiEnabled) }

    // Estado de prueba de conexión y búsqueda de modelos
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultSuccess by remember { mutableStateOf<Boolean?>(null) }
    var testResultMessage by remember { mutableStateOf<String?>(null) }

    // Modelos dinámicos detectados
    var dynamicModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCustomModelActive by remember {
        mutableStateOf(
            PREDEFINED_GEMINI_MODELS.none { it.id.equals(selectedModel, ignoreCase = true) }
        )
    }
    var customModelInput by remember {
        mutableStateOf(if (isCustomModelActive) selectedModel else "")
    }

    val allModelOptions = remember(dynamicModels) {
        val list = PREDEFINED_GEMINI_MODELS.toMutableList()
        dynamicModels.forEach { dynamicId ->
            val cleanId = dynamicId.removePrefix("models/").trim()
            if (list.none { it.id.equals(cleanId, ignoreCase = true) }) {
                list.add(
                    GeminiModelOption(
                        id = cleanId,
                        displayName = formatGeminiModelName(cleanId),
                        description = "Modelo detectado de tu cuenta de Google Gemini",
                        badge = "Detectado"
                    )
                )
            }
        }
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = DashboardIcons.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Configuración de IA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Google Gemini en Kontio",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Interruptor de activación general de IA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Habilitar Inteligencia Artificial",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lectura de extractos PDF/CSV y categorización inteligente.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Switch(
                        checked = enabledState,
                        onCheckedChange = { enabledState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                // 2. Campo de API Key de Gemini
                val effectiveKey = newApiKeyInput.trim().ifBlank { currentApiKey.orEmpty() }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "API KEY DE GOOGLE GEMINI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (hasExistingKey) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = DashboardIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Clave API configurada (••••••••••••••••)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Por seguridad, la clave actual no se muestra. Escribe abajo para modificarla.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = newApiKeyInput,
                        onValueChange = {
                            newApiKeyInput = it
                            testResultSuccess = null
                            testResultMessage = null
                        },
                        label = { Text(if (hasExistingKey) "Modificar API Key" else "API Key de Google Gemini") },
                        placeholder = { Text(if (hasExistingKey) "Escribe nueva clave para reemplazar..." else "AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        trailingIcon = if (newApiKeyInput.isNotEmpty()) {
                            {
                                IconButton(onClick = { newApiKeyInput = "" }) {
                                    Icon(
                                        imageVector = DashboardIcons.Close,
                                        contentDescription = "Limpiar texto",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Obtener clave gratuita",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                uriHandler.openUri("https://aistudio.google.com/app/apikey")
                            }
                        )

                        // Botón para probar la conexión
                        Button(
                            onClick = {
                                if (effectiveKey.isNotBlank()) {
                                    scope.launch {
                                        isTestingConnection = true
                                        testResultSuccess = null
                                        testResultMessage = null
                                        val client = GeminiApiClient()
                                        val result = client.fetchAvailableModels(effectiveKey)
                                        isTestingConnection = false
                                        result.onSuccess { models ->
                                            testResultSuccess = true
                                            testResultMessage = "¡Conexión exitosa! (${models.size} modelos detectados)"
                                            dynamicModels = models
                                            enabledState = true
                                        }.onFailure { err ->
                                            testResultSuccess = false
                                            testResultMessage = err.message ?: "Error al verificar la clave"
                                        }
                                    }
                                }
                            },
                            enabled = effectiveKey.isNotBlank() && !isTestingConnection,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Verificando...", fontSize = 11.sp)
                            } else {
                                Text("Probar conexión", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Mensaje de resultado de prueba
                    if (testResultMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (testResultSuccess == true)
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = testResultMessage.orEmpty(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (testResultSuccess == true)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // 3. Selector de Modelos de Gemini
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SELECCIONAR MODELO DE IA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        if (effectiveKey.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        isTestingConnection = true
                                        val client = GeminiApiClient()
                                        client.fetchAvailableModels(effectiveKey)
                                            .onSuccess { models ->
                                                dynamicModels = models
                                            }
                                        isTestingConnection = false
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isTestingConnection) "Buscando..." else "Detectar modelos",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allModelOptions.forEach { modelOption ->
                            val isSelected = !isCustomModelActive &&
                                    modelOption.id.equals(selectedModel, ignoreCase = true)

                            ModelSelectionRow(
                                option = modelOption,
                                isSelected = isSelected,
                                onClick = {
                                    selectedModel = modelOption.id
                                    isCustomModelActive = false
                                }
                            )
                        }

                        // Opción de modelo personalizado
                        val isCustomSelected = isCustomModelActive
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isCustomSelected) 1.5.dp else 1.dp,
                                    color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .background(
                                    if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)
                                )
                                .clickable {
                                    isCustomModelActive = true
                                    if (customModelInput.isNotBlank()) {
                                        selectedModel = customModelInput.trim()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Modelo personalizado",
                                    fontSize = 14.sp,
                                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ingresa manualmente el nombre de otro modelo de Gemini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isCustomSelected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = customModelInput,
                                        onValueChange = {
                                            customModelInput = it
                                            selectedModel = it.trim()
                                        },
                                        placeholder = { Text("Ej: gemini-2.0-flash-exp") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }

                            if (isCustomSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = DashboardIcons.Check,
                                        contentDescription = "Seleccionado",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalModel = if (isCustomModelActive && customModelInput.isNotBlank()) {
                        customModelInput.trim()
                    } else {
                        selectedModel
                    }
                    val finalKey = newApiKeyInput.trim().ifBlank { currentApiKey.orEmpty() }
                    onSave(finalKey, finalModel, enabledState)
                    onDismiss()
                },
                enabled = (newApiKeyInput.isNotBlank() || hasExistingKey) || !enabledState,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Guardar configuración", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!currentApiKey.isNullOrBlank()) {
                    TextButton(
                        onClick = {
                            onDisconnect()
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "Desconectar",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun ModelSelectionRow(
    option: GeminiModelOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.40f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.displayName,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                if (option.badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (option.isRecommended)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = option.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (option.isRecommended)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = option.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DashboardIcons.Check,
                    contentDescription = "Seleccionado",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
