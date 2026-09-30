package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable

/**
 * Estado y control de reconocimiento de voz multiplataforma para dictado de transacciones.
 */
interface SpeechRecognizerState {
    val isListening: Boolean
    val spokenText: String
    val partialText: String
    val rmsLevel: Float // 0f..1f para animación de ondas de sonido
    val errorMessage: String?
    val isAvailable: Boolean
    val hasPermission: Boolean

    fun requestPermissionAndStart()
    fun startListening()
    fun stopListening()
    fun cancelListening()
    fun updateSpokenText(text: String)
    fun clearError()
    fun reset()
}

/**
 * Composable que recuerda y gestiona la instancia nativa del reconocedor de voz.
 */
@Composable
expect fun rememberSpeechRecognizer(): SpeechRecognizerState
