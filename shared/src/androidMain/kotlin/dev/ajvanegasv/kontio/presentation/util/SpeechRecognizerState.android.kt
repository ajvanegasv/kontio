package dev.ajvanegasv.kontio.presentation.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

class AndroidSpeechRecognizerState(
    private val context: Context,
    private val onRequestPermission: () -> Unit
) : SpeechRecognizerState {

    override var isListening by mutableStateOf(false)
        private set

    override var spokenText by mutableStateOf("")
        private set

    override var partialText by mutableStateOf("")
        private set

    override var rmsLevel by mutableFloatStateOf(0f)
        private set

    override var errorMessage by mutableStateOf<String?>(null)
        private set

    override val isAvailable: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private var speechRecognizer: SpeechRecognizer? = null

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null && isAvailable) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        }
        return speechRecognizer
    }

    override fun requestPermissionAndStart() {
        if (hasPermission) {
            startListening()
        } else {
            onRequestPermission()
        }
    }

    override fun startListening() {
        if (!hasPermission) {
            errorMessage = "Se requiere permiso de micrófono para dictar transacciones"
            onRequestPermission()
            return
        }

        if (!isAvailable) {
            errorMessage = "El reconocimiento de voz no está disponible en este dispositivo"
            return
        }

        errorMessage = null
        partialText = ""

        try {
            val recognizer = ensureRecognizer() ?: return
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Preferir idioma español o el del sistema
                val defaultTag = Locale.getDefault().toLanguageTag()
                val langTag = if (defaultTag.startsWith("es", ignoreCase = true)) defaultTag else "es-ES"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            }
            recognizer.startListening(intent)
            isListening = true
        } catch (e: Exception) {
            isListening = false
            errorMessage = "No se pudo iniciar el micrófono: ${e.message}"
        }
    }

    override fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignorar error al detener
        }
        isListening = false
        rmsLevel = 0f
    }

    override fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignorar
        }
        isListening = false
        rmsLevel = 0f
        partialText = ""
    }

    override fun updateSpokenText(text: String) {
        spokenText = text
    }

    override fun clearError() {
        errorMessage = null
    }

    override fun reset() {
        cancelListening()
        spokenText = ""
        partialText = ""
        rmsLevel = 0f
        errorMessage = null
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignorar
        }
        speechRecognizer = null
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isListening = true
            errorMessage = null
        }

        override fun onBeginningOfSpeech() {
            isListening = true
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalizar dB (-2dB a 10dB aprox) a rango 0f..1f para animación de onda
            rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            rmsLevel = 0f
        }

        override fun onError(error: Int) {
            isListening = false
            rmsLevel = 0f

            // No tratar ERROR_NO_MATCH o TIMEOUT como fallos críticos
            errorMessage = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó audio claro. Intenta hablar nuevamente."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de silencio agotado. Toca el micrófono para continuar."
                SpeechRecognizer.ERROR_AUDIO -> "Error en la captura de audio del micrófono."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Error de conexión de red para el reconocimiento de voz."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "Permiso de grabación de audio denegado."
                SpeechRecognizer.ERROR_CLIENT -> null // Cancelaciones normales de cliente
                else -> "Error en el reconocimiento de voz ($error)."
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            rmsLevel = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognized = matches[0].trim()
                if (recognized.isNotBlank()) {
                    spokenText = if (spokenText.isBlank()) {
                        recognized
                    } else {
                        "$spokenText $recognized"
                    }
                }
            }
            partialText = ""
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                partialText = matches[0].trim()
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}

@Composable
actual fun rememberSpeechRecognizer(): SpeechRecognizerState {
    val context = LocalContext.current
    var hasPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermissionGranted = isGranted
        if (isGranted && pendingStartAfterPermission) {
            pendingStartAfterPermission = false
        }
    }

    val recognizerState = remember(context) {
        AndroidSpeechRecognizerState(
            context = context,
            onRequestPermission = {
                pendingStartAfterPermission = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }

    // Si se acaba de otorgar permiso tras solicitud previa, iniciar escucha
    if (hasPermissionGranted && pendingStartAfterPermission) {
        pendingStartAfterPermission = false
        recognizerState.startListening()
    }

    DisposableEffect(recognizerState) {
        onDispose {
            recognizerState.destroy()
        }
    }

    return recognizerState
}
