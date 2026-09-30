package dev.ajvanegasv.kontio.data.remote.gemini

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.ParsedStatementItem
import dev.ajvanegasv.kontio.domain.model.ParsedStatementResult
import dev.ajvanegasv.kontio.domain.model.StatementFile
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

open class GeminiStatementParser(
    private val geminiApiClient: GeminiApiClient = GeminiApiClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @OptIn(ExperimentalEncodingApi::class)
    open suspend fun parseStatement(
        file: StatementFile,
        apiKey: String,
        categories: List<Category>,
        accounts: List<Account>,
        model: String = GeminiApiClient.DEFAULT_MODEL
    ): Result<ParsedStatementResult> {
        return runCatching {
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
                Eres un asistente financiero de alta precisión integrado en Kontio, especializado en extraer y clasificar transacciones a partir de extractos bancarios (en formatos PDF y CSV).
                
                Instrucciones críticas:
                1. Detecta el nombre del banco o institución emisora (ej. Bancolombia, Nu, Davivienda, BBVA, Santander, Chase, etc.) y los últimos dígitos de la cuenta/tarjeta si aparecen en el documento.
                2. Si el banco o cuenta coincide con alguna de las cuentas del usuario provistas en 'cuentas_disponibles', indica su 'suggestedAccountId'.
                3. Extrae todas las transacciones financieras individuales contenidas en el extracto.
                4. Para cada transacción:
                   - 'date': Fecha en formato 'YYYY-MM-DD'.
                   - 'rawDescription': El concepto o texto textual tal como aparece en el extracto bancario.
                   - 'cleanTitle': Nombre normalizado, limpio y comprensible del comercio o movimiento (ej. 'Uber', 'Supermercado Éxito', 'Netflix', 'Nómina', 'Farmacia').
                   - 'amount': Monto numérico positivo (valor absoluto en decimal).
                   - 'type': 'EXPENSE' para débitos, compras, cargos, comisiones o retiros. 'INCOME' para abonos, depósitos, nómina, transferencias recibidas o intereses.
                   - 'suggestedCategoryId': El ID exacto de la categoría más adecuada tomada exclusivamente del listado 'categorias_disponibles'. Si no hay una categoría clara, usa 'cat_other_exp' para gastos o 'cat_other_inc' para ingresos.
                   - 'confidence': Puntuación decimal de confianza (0.0 a 1.0) en la categorización.
                   - 'notes': Detalles relevantes adicionales (ej. ciudad, referencia o tipo de abono).
                
                Categorías disponibles:
                $categoriesJson
                
                Cuentas del usuario disponibles:
                $accountsJson
                
                Responde ÚNICAMENTE con un JSON con la estructura:
                {
                  "detectedBank": "Nombre del Banco",
                  "detectedAccountNumber": "1234",
                  "currency": "USD",
                  "transactions": [
                    {
                      "date": "YYYY-MM-DD",
                      "rawDescription": "CONCEPTO ORIGINAL",
                      "cleanTitle": "Título Limpio",
                      "amount": 123.45,
                      "type": "EXPENSE",
                      "suggestedCategoryId": "cat_food",
                      "confidence": 0.95,
                      "notes": "Detalle opcional"
                    }
                  ]
                }
            """.trimIndent()

            val parts = mutableListOf<GeminiPart>()

            if (file.isPdf) {
                val base64Data = Base64.Default.encode(file.bytes)
                parts.add(
                    GeminiPart(
                        text = "Por favor analiza este extracto bancario en PDF y extrae las transacciones y banco asociado."
                    )
                )
                parts.add(
                    GeminiPart(
                        inlineData = GeminiInlineData(
                            mimeType = "application/pdf",
                            data = base64Data
                        )
                    )
                )
            } else {
                // CSV o texto plano
                val fileText = file.readAsText()
                parts.add(
                    GeminiPart(
                        text = "Analiza el siguiente extracto bancario en formato CSV/Texto:\n\n---\n$fileText\n---"
                    )
                )
            }

            val request = GeminiRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = parts
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1f
                )
            )

            val rawJson = geminiApiClient.generateContent(
                apiKey = apiKey,
                request = request,
                model = model
            ).getOrThrow()
            val analysis = json.decodeFromString<RawStatementAnalysis>(rawJson)

            val nowMillis = Clock.System.now().toEpochMilliseconds()
            val parsedItems = analysis.transactions.mapIndexed { index, rawTx ->
                val type = try {
                    TransactionType.valueOf(rawTx.type.uppercase().trim())
                } catch (e: Exception) {
                    TransactionType.EXPENSE
                }

                val dateMillis = parseStatementDate(rawTx.date)

                ParsedStatementItem(
                    id = "parsed_${nowMillis}_$index",
                    date = dateMillis,
                    originalDescription = rawTx.rawDescription,
                    cleanTitle = rawTx.cleanTitle.ifBlank { rawTx.rawDescription },
                    amount = rawTx.amount.coerceAtLeast(0.0),
                    type = type,
                    suggestedCategoryId = rawTx.suggestedCategoryId,
                    confidenceScore = rawTx.confidence,
                    isDuplicate = false,
                    isSelected = true,
                    note = rawTx.notes ?: ""
                )
            }

            // Encontrar coincidencia de cuenta sugerida si no vino explícita
            val detectedNum = analysis.detectedAccountNumber?.trim()?.takeIf { it.isNotEmpty() }
            val detectedBank = analysis.detectedBank?.trim()?.takeIf { it.isNotEmpty() }

            val matchedAccountId = when {
                detectedNum != null && detectedBank != null -> {
                    accounts.firstOrNull { acc ->
                        acc.name.contains(detectedBank, ignoreCase = true) && acc.name.contains(detectedNum, ignoreCase = true)
                    }?.id ?: accounts.firstOrNull { acc ->
                        acc.name.contains(detectedNum, ignoreCase = true)
                    }?.id ?: accounts.firstOrNull { acc ->
                        acc.name.contains(detectedBank, ignoreCase = true)
                    }?.id
                }
                detectedNum != null -> {
                    accounts.firstOrNull { acc ->
                        acc.name.contains(detectedNum, ignoreCase = true)
                    }?.id
                }
                detectedBank != null -> {
                    accounts.firstOrNull { acc ->
                        acc.name.contains(detectedBank, ignoreCase = true)
                    }?.id
                }
                else -> null
            }

            ParsedStatementResult(
                detectedBankName = analysis.detectedBank,
                detectedAccountNumber = analysis.detectedAccountNumber,
                suggestedAccountId = matchedAccountId,
                currency = analysis.currency ?: "USD",
                items = parsedItems
            )
        }
    }

    private fun parseStatementDate(dateStr: String): Long {
        val trimmed = dateStr.trim()
        return try {
            val parts = trimmed.split("-", "/")
            if (parts.size == 3) {
                val (y, m, d) = if (parts[0].length == 4) {
                    Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                } else if (parts[2].length == 4) {
                    Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                } else {
                    val year = 2000 + parts[2].toInt()
                    Triple(year, parts[1].toInt(), parts[0].toInt())
                }
                LocalDate(y, m, d).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            } else {
                Clock.System.now().toEpochMilliseconds()
            }
        } catch (e: Exception) {
            Clock.System.now().toEpochMilliseconds()
        }
    }
}
