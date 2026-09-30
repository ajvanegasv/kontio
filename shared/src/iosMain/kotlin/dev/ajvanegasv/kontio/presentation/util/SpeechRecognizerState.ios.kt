package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class IosSpeechRecognizerState : SpeechRecognizerState {
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

    override val isAvailable: Boolean = true
    override val hasPermission: Boolean = true

    override fun requestPermissionAndStart() {
        startListening()
    }

    override fun startListening() {
        isListening = true
        errorMessage = null
    }

    override fun stopListening() {
        isListening = false
        rmsLevel = 0f
    }

    override fun cancelListening() {
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
}

@Composable
actual fun rememberSpeechRecognizer(): SpeechRecognizerState {
    return remember { IosSpeechRecognizerState() }
}
