package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiContent
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiGenerationConfig
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiPart
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiRequest
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.ParsedVoiceTransaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

open class ParseVoiceTransactionUseCase(
    private val aiConfigStorage: AiConfigStorage,
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    private data class RawVoiceExtraction(
        val amount: Double = 0.0,
        val type: String = "EXPENSE",
        val suggestedAccountId: String? = null,
        val suggestedCategoryId: String? = null,
        val cleanNote: String = "",
        val date: String? = null, // YYYY-MM-DD
        val confidence: Float = 0.9f
    )

    open suspend operator fun invoke(
        voiceText: String,
        accounts: List<Account>,
        categories: List<Category>
    ): Result<ParsedVoiceTransaction> {
        val trimmed = voiceText.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("El texto dictado no puede estar vacío"))
        }

        val apiKey = aiConfigStorage.getApiKey()?.trim().orEmpty()
        val isAiAvailable = aiConfigStorage.isAiEnabled() && apiKey.isNotBlank()

        // 1. Intentar con Gemini IA si está configurado y habilitado
        if (isAiAvailable) {
            val aiResult = runCatching {
                extractWithGemini(
                    voiceText = trimmed,
                    apiKey = apiKey,
                    accounts = accounts,
                    categories = categories
                )
            }

            if (aiResult.isSuccess) {
                return aiResult
            }
            // Si la IA remota falla (ej. error de red, API key caducada, cuota), recurrir al parser heurístico
        }

        // 2. Respaldo semántico heurístico offline
        return runCatching {
            extractWithLocalHeuristic(
                voiceText = trimmed,
                accounts = accounts,
                categories = categories
            )
        }
    }

    private suspend fun extractWithGemini(
        voiceText: String,
        apiKey: String,
        accounts: List<Account>,
        categories: List<Category>
    ): ParsedVoiceTransaction {
        val categoriesJson = buildJsonArray {
            categories.forEach { cat ->
                add(buildJsonObject {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("type", cat.type.name)
                })
            }
        }.toString()

        val accountsJson = buildJsonArray {
            accounts.forEach { acc ->
                add(buildJsonObject {
                    put("id", acc.id)
                    put("name", acc.name)
                    put("type", acc.type.name)
                })
            }
        }.toString()

        val systemPrompt = """
            Eres un asistente financiero de Kontio especializado en interpretar frases de voz naturales en español para crear transacciones financieras.
            
            Tu objetivo es extraer:
            1. 'amount': Número decimal positivo del valor económico (ej. si dice "45 mil", es 45000; si dice "2 millones", es 2000000; si dice "15 con 50", es 15.50).
            2. 'type': 'EXPENSE' para gastos, compras o pagos. 'INCOME' para ingresos, nómina, sueldos, cobros o depósitos.
            3. 'suggestedAccountId': El ID exacto de la cuenta que el usuario menciona del listado 'cuentas_disponibles' (o null si no menciona ninguna).
            4. 'suggestedCategoryId': El ID exacto de la categoría más adecuada del listado 'categorias_disponibles' según el tipo de movimiento (o null si no hay coincidencia).
            5. 'cleanNote': Un título o concepto normalizado, limpio y comprensible del movimiento (ej. "Almuerzo en restaurante", "Gasolina", "Nómina quincenal", "Uber a casa").
            6. 'date': Fecha en formato 'YYYY-MM-DD' si menciona temporalidad (ej. "ayer", "hoy", fecha explícita), o null para fecha actual.
            7. 'confidence': Puntuación decimal de 0.0 a 1.0.

            Cuentas disponibles:
            $accountsJson

            Categorías disponibles:
            $categoriesJson

            Responde ÚNICAMENTE con un JSON con la estructura:
            {
              "amount": 45000.0,
              "type": "EXPENSE",
              "suggestedAccountId": "acc_1",
              "suggestedCategoryId": "cat_food",
              "cleanNote": "Almuerzo",
              "date": "2026-09-29",
              "confidence": 0.95
            }
        """.trimIndent()

        val request = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = "Transcribe y extrae los datos de esta frase de voz: \"$voiceText\""))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        val model = aiConfigStorage.getModel()
        val rawJson = geminiApiClient.generateContent(
            apiKey = apiKey,
            request = request,
            model = model
        ).getOrThrow()

        val parsed = json.decodeFromString<RawVoiceExtraction>(rawJson)
        val resolvedType = try {
            TransactionType.valueOf(parsed.type.uppercase().trim())
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }

        val resolvedDateMillis = parsed.date?.let { parseDateStringToMillis(it) }
            ?: kotlin.time.Clock.System.now().toEpochMilliseconds()

        val resolvedAccountId = parsed.suggestedAccountId?.takeIf { id -> accounts.any { it.id == id } }
            ?: accounts.firstOrNull()?.id

        val filteredCategories = categories.filter { it.type == resolvedType }
        val resolvedCategoryId = parsed.suggestedCategoryId?.takeIf { id -> filteredCategories.any { it.id == id } }
            ?: filteredCategories.firstOrNull()?.id

        return ParsedVoiceTransaction(
            amount = parsed.amount.coerceAtLeast(0.0),
            type = resolvedType,
            suggestedAccountId = resolvedAccountId,
            suggestedCategoryId = resolvedCategoryId,
            note = parsed.cleanNote.ifBlank { voiceText }.trim(),
            timestamp = resolvedDateMillis,
            rawVoiceText = voiceText,
            confidence = parsed.confidence
        )
    }

    /**
     * Analizador semántico heurístico para funcionamiento sin conexión o sin clave de API.
     */
    fun extractWithLocalHeuristic(
        voiceText: String,
        accounts: List<Account>,
        categories: List<Category>
    ): ParsedVoiceTransaction {
        val lower = voiceText.lowercase()
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()

        // 1. Detectar Tipo (Gasto vs Ingreso)
        val incomeKeywords = listOf(
            "ingreso", "ingresó", "me pagaron", "recibí", "recibi", "nómina", "nomina",
            "sueldo", "abono", "depósito", "deposito", "cobré", "cobre", "consignación",
            "consignacion", "gané", "gane"
        )
        val isIncome = incomeKeywords.any { lower.contains(it) }
        val resolvedType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 2. Extraer Monto
        val extractedAmount = parseAmountFromText(lower)

        // 3. Detectar Fecha (Hoy, Ayer, Anteayer)
        val oneDayMillis = 86_400_000L
        val resolvedTimestamp = when {
            lower.contains("anteayer") || lower.contains("antier") -> now - (oneDayMillis * 2)
            lower.contains("ayer") -> now - oneDayMillis
            else -> now
        }

        // 4. Detectar Cuenta
        val matchedAccount = accounts.firstOrNull { acc ->
            val accNameLower = acc.name.lowercase()
            lower.contains(accNameLower) || (accNameLower.contains("efectivo") && lower.contains("efectivo"))
        } ?: accounts.firstOrNull()

        // 5. Detectar Categoría
        val availableCategories = categories.filter { it.type == resolvedType }
        val matchedCategory = availableCategories.firstOrNull { cat ->
            val catLower = cat.name.lowercase()
            lower.contains(catLower)
        } ?: findCategoryByKeyword(lower, availableCategories)
          ?: availableCategories.firstOrNull()

        // 6. Generar Nota limpia
        val cleanNote = generateCleanNote(voiceText)

        return ParsedVoiceTransaction(
            amount = extractedAmount,
            type = resolvedType,
            suggestedAccountId = matchedAccount?.id,
            suggestedCategoryId = matchedCategory?.id,
            note = cleanNote,
            timestamp = resolvedTimestamp,
            rawVoiceText = voiceText,
            confidence = if (extractedAmount > 0.0) 0.8f else 0.5f
        )
    }

    private fun parseAmountFromText(text: String): Double {
        // Detectar palabras como "45 mil", "2 millones", "50 lucas"
        val millionRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:millones|millon|millón)""")
        val millionMatch = millionRegex.find(text)
        if (millionMatch != null) {
            val num = millionMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return num * 1_000_000.0
        }

        val thousandRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:mil|k|lucas)""")
        val thousandMatch = thousandRegex.find(text)
        if (thousandMatch != null) {
            val num = thousandMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
            return num * 1_000.0
        }

        // Extraer números independientes (ej. "45000", "50.00", "$ 12000", "50.000")
        val numberRegex = Regex("""\$?\s*(\d+(?:[.,]\d+)*)""")
        val match = numberRegex.find(text)
        if (match != null) {
            val raw = match.groupValues[1].trim()
            val dotCount = raw.count { it == '.' }
            val commaCount = raw.count { it == ',' }

            val cleaned = when {
                // Separador de miles con punto (ej. 50.000 o 1.500.000)
                dotCount >= 1 && commaCount == 0 && raw.substringAfterLast('.').length == 3 && raw.length > 4 -> {
                    raw.replace(".", "")
                }
                // Separador de miles con coma (ej. 50,000)
                commaCount >= 1 && dotCount == 0 && raw.substringAfterLast(',').length == 3 && raw.length > 4 -> {
                    raw.replace(",", "")
                }
                // Coma decimal (ej. 15,50)
                commaCount == 1 && dotCount == 0 && raw.substringAfterLast(',').length <= 2 -> {
                    raw.replace(",", ".")
                }
                else -> raw
            }
            return cleaned.toDoubleOrNull() ?: 0.0
        }

        return 0.0
    }

    private fun findCategoryByKeyword(text: String, categories: List<Category>): Category? {
        val keywordMap = mapOf(
            listOf("almuerzo", "cena", "desayuno", "comida", "restaurante", "hamburguesa", "pizza", "café", "cafe") to "comida",
            listOf("uber", "taxi", "gasolina", "transporte", "metro", "bus", "pasaje", "peaje") to "transporte",
            listOf("mercado", "supermercado", "víveres", "viveres", "éxito", "d1", "arauca", "jumbo") to "supermercado",
            listOf("netflix", "cine", "concierto", "juego", "salida", "fiesta", "cerveza", "trago") to "ocio",
            listOf("médico", "medico", "farmacia", "medicina", "droguería", "salud", "cita") to "salud",
            listOf("luz", "agua", "gas", "internet", "arriendo", "alquiler", "celular") to "servicios",
            listOf("nómina", "nomina", "salario", "sueldo", "quincena", "trabajo") to "sueldo"
        )

        for ((keywords, categoryHint) in keywordMap) {
            if (keywords.any { text.contains(it) }) {
                val found = categories.firstOrNull { it.name.lowercase().contains(categoryHint) }
                if (found != null) return found
            }
        }
        return null
    }

    private fun generateCleanNote(rawText: String): String {
        // Remover preámbulos típicos en español
        val prefixesToRemove = listOf(
            Regex("""^(gasté|gaste|pagué|pague|compré|compre|recibí|recibi|ingresé|ingrese|me pagaron)\s+""", RegexOption.IGNORE_CASE),
            Regex("""^(un|una|el|la|los|las)\s+""", RegexOption.IGNORE_CASE),
            Regex("""^\$?\d+[\d.,]*\s*(mil|lucas|millones|pesos|dólares|dolares|usd)?\s*(en|de|para|con)?\s*""", RegexOption.IGNORE_CASE)
        )

        var cleaned = rawText.trim()
        prefixesToRemove.forEach { regex ->
            cleaned = cleaned.replace(regex, "").trim()
        }

        return cleaned.ifBlank { rawText }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun parseDateStringToMillis(dateStr: String): Long {
        return try {
            val localDate = LocalDate.parse(dateStr.trim())
            localDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        } catch (e: Exception) {
            kotlin.time.Clock.System.now().toEpochMilliseconds()
        }
    }
}
