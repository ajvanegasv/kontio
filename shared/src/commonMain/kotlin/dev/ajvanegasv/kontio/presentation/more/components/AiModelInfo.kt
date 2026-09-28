package dev.ajvanegasv.kontio.presentation.more.components

data class GeminiModelOption(
    val id: String,
    val displayName: String,
    val description: String,
    val badge: String? = null,
    val isRecommended: Boolean = false
)

val PREDEFINED_GEMINI_MODELS = listOf(
    GeminiModelOption(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Modelo más reciente y recomendado. Alta velocidad, precisión y optimizado para extractos financieros.",
        badge = "Recomendado",
        isRecommended = true
    ),
    GeminiModelOption(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Rápido y eficiente para reconocimiento de comercios y categorización diaria.",
        badge = "Estable"
    ),
    GeminiModelOption(
        id = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "Ultra ligero y diseñado para mínima latencia y menor consumo.",
        badge = "Ultra Rápido"
    ),
    GeminiModelOption(
        id = "gemini-2.5-pro",
        displayName = "Gemini 2.5 Pro",
        description = "Máxima capacidad de razonamiento para análisis profundos y extractos complejos.",
        badge = "Pro"
    ),
    GeminiModelOption(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash",
        description = "Excelente balance entre velocidad, rendimiento y precisión probada.",
        badge = "Balanceado"
    )
)

fun formatGeminiModelName(modelId: String): String {
    val clean = modelId.removePrefix("models/").trim()
    val predefined = PREDEFINED_GEMINI_MODELS.firstOrNull { it.id.equals(clean, ignoreCase = true) }
    if (predefined != null) return predefined.displayName

    return when (clean) {
        "gemini-3.8-flash" -> "Gemini 3.8 Flash"
        "gemini-3.5-flash" -> "Gemini 3.5 Flash"
        "gemini-3.5-flash-lite" -> "Gemini 3.5 Flash-Lite"
        "gemini-2.5-pro" -> "Gemini 2.5 Pro"
        "gemini-2.5-flash" -> "Gemini 2.5 Flash"
        "gemini-2.5-flash-lite" -> "Gemini 2.5 Flash-Lite"
        else -> clean.replaceFirstChar { it.uppercase() }
    }
}
