package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

/**
 * Representa la estructura interpretada por la IA a partir del dictado de voz del usuario.
 *
 * @property amount Monto numérico absoluto de la transacción.
 * @property type Tipo de movimiento (Gasto o Ingreso).
 * @property suggestedAccountId ID de la cuenta bancaria sugerida por la IA o el parser heurístico.
 * @property suggestedCategoryId ID de la categoría sugerida acorde a las categorías activas.
 * @property note Concepto o descripción limpia del movimiento (ej. "Almuerzo en Crepes").
 * @property timestamp Fecha y hora estimada en milisegundos (reconoce hoy, ayer, etc.).
 * @property rawVoiceText Texto original exacto dictado por el usuario.
 * @property confidence Puntuación de confianza (0.0 a 1.0) en la extracción de la IA.
 */
@Serializable
data class ParsedVoiceTransaction(
    val amount: Double = 0.0,
    val type: TransactionType = TransactionType.EXPENSE,
    val suggestedAccountId: String? = null,
    val suggestedCategoryId: String? = null,
    val note: String = "",
    val timestamp: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    val rawVoiceText: String = "",
    val confidence: Float = 1.0f
)
