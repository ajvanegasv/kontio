package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Periodos temporales relativos reconocidos en lenguaje natural.
 */
enum class RelativePeriod(val label: String) {
    CURRENT_MONTH("Este Mes"),
    PREVIOUS_MONTH("El Mes Pasado"),
    CURRENT_YEAR("Este Año"),
    PREVIOUS_YEAR("El Año Pasado"),
    LAST_3_MONTHS("Últimos 3 Meses"),
    LAST_6_MONTHS("Últimos 6 Meses"),
    CURRENT_WEEK("Esta Semana"),
    TODAY("Hoy"),
    YESTERDAY("Ayer")
}

/**
 * Intención financiera estructurada extraída a partir de una consulta en lenguaje natural.
 */
data class FinancialIntent(
    val cleanKeyword: String,
    val year: Int? = null,
    val month: Int? = null, // 1..12
    val relativePeriod: RelativePeriod? = null,
    val type: TransactionType? = null,
    val displayPeriodLabel: String? = null,
    val startDateMillis: Long? = null,
    val endDateMillis: Long? = null,
    val originalQuery: String = ""
)

/**
 * Parser semántico para consultas financieras en español.
 * Desacopla conceptos o comercios ("Uber", "Netflix") de restricciones temporales ("en 2026", "este mes").
 */
object FinancialQueryParser {

    private val spanishMonths = mapOf(
        "enero" to 1,
        "febrero" to 2,
        "marzo" to 3,
        "abril" to 4,
        "mayo" to 5,
        "junio" to 6,
        "julio" to 7,
        "agosto" to 8,
        "septiembre" to 9,
        "setiembre" to 9,
        "octubre" to 10,
        "noviembre" to 11,
        "diciembre" to 12
    )

    private val questionPreambles = listOf(
        "cuánto he gastado en",
        "cuanto he gastado en",
        "cuánto gasté en",
        "cuanto gaste en",
        "cuánto he pagado en",
        "cuanto he pagado en",
        "cuánto pagué en",
        "cuanto pague en",
        "cuánto va en",
        "cuanto va en",
        "cuánto fue en",
        "cuanto fue en",
        "cuánto dinero en",
        "cuanto dinero en",
        "gastos en",
        "gastos de",
        "pagos en",
        "pagos de",
        "total en",
        "total de",
        "resumen de",
        "buscar",
        "mostrar",
        "reporte de",
        "reporte"
    )

    fun parseQuery(
        rawQuery: String,
        clockNowMillis: Long = Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): FinancialIntent {
        var text = rawQuery.trim('?', '¿', '!', '¡', '.', ':', ',', ';', ' ')
        val lowerText = text.lowercase()

        // 1. Detección de tipo de transacción
        val detectedType = when {
            listOf("gastado", "gaste", "gasté", "gastos", "pague", "pagué", "pagado", "pagos", "compras").any { lowerText.contains(it) } -> TransactionType.EXPENSE
            listOf("ganado", "ingresos", "sueldo", "nómina", "nomina", "cobrado", "depósitos", "depositos").any { lowerText.contains(it) } -> TransactionType.INCOME
            else -> null
        }

        // 2. Extracción de año (e.g. 2024..2035)
        val yearRegex = Regex("""\b(20[2-3][0-9])\b""")
        val yearMatch = yearRegex.find(text)
        val detectedYear = yearMatch?.value?.toIntOrNull()

        // 3. Extracción de mes en español
        var detectedMonth: Int? = null
        var monthStringMatched: String? = null
        for ((mName, mNum) in spanishMonths) {
            val monthRegex = Regex("""\b$mName\b""", RegexOption.IGNORE_CASE)
            if (monthRegex.containsMatchIn(text)) {
                detectedMonth = mNum
                monthStringMatched = mName
                break
            }
        }

        // 4. Extracción de periodos relativos
        var detectedRelativePeriod: RelativePeriod? = null
        val lower = text.lowercase()
        when {
            lower.contains("este mes") || lower.contains("mes actual") || lower.contains("del mes") -> detectedRelativePeriod = RelativePeriod.CURRENT_MONTH
            lower.contains("el mes pasado") || lower.contains("mes pasado") || lower.contains("mes anterior") -> detectedRelativePeriod = RelativePeriod.PREVIOUS_MONTH
            lower.contains("este año") || lower.contains("año actual") || lower.contains("del año") -> detectedRelativePeriod = RelativePeriod.CURRENT_YEAR
            lower.contains("el año pasado") || lower.contains("año pasado") || lower.contains("año anterior") -> detectedRelativePeriod = RelativePeriod.PREVIOUS_YEAR
            lower.contains("últimos 3 meses") || lower.contains("ultimos 3 meses") || lower.contains("últimos tres meses") -> detectedRelativePeriod = RelativePeriod.LAST_3_MONTHS
            lower.contains("últimos 6 meses") || lower.contains("ultimos 6 meses") || lower.contains("últimos seis meses") -> detectedRelativePeriod = RelativePeriod.LAST_6_MONTHS
            lower.contains("esta semana") -> detectedRelativePeriod = RelativePeriod.CURRENT_WEEK
            lower.contains("hoy") -> detectedRelativePeriod = RelativePeriod.TODAY
            lower.contains("ayer") -> detectedRelativePeriod = RelativePeriod.YESTERDAY
        }

        // 5. Limpieza del término clave (eliminar preámbulos, año, mes y frases relativas)
        // Eliminar preámbulo inicial
        val lowerForPreamble = text.lowercase()
        for (preamble in questionPreambles) {
            if (lowerForPreamble.startsWith(preamble)) {
                text = text.substring(preamble.length).trim()
                break
            }
        }

        // Eliminar fragmentos temporales del texto
        if (detectedRelativePeriod != null) {
            val phrasesToRemove = listOf(
                "este mes", "mes actual", "del mes",
                "el mes pasado", "mes pasado", "mes anterior",
                "este año", "año actual", "del año",
                "el año pasado", "año pasado", "año anterior",
                "últimos 3 meses", "ultimos 3 meses", "últimos tres meses",
                "últimos 6 meses", "ultimos 6 meses", "últimos seis meses",
                "esta semana", "hoy", "ayer"
            )
            for (p in phrasesToRemove) {
                text = text.replace(Regex("""\b$p\b""", RegexOption.IGNORE_CASE), " ")
            }
        }

        if (detectedYear != null) {
            // Eliminar "en 2026", "de 2026", "del 2026", "año 2026", o solo "2026"
            text = text.replace(Regex("""\b(en|de|del|año)\s+$detectedYear\b""", RegexOption.IGNORE_CASE), " ")
            text = text.replace(Regex("""\b$detectedYear\b"""), " ")
        }

        if (monthStringMatched != null) {
            // Eliminar "en marzo", "de marzo", "del mes de marzo", "marzo"
            text = text.replace(Regex("""\b(del\s+mes\s+de|en|de)\s+$monthStringMatched\b""", RegexOption.IGNORE_CASE), " ")
            text = text.replace(Regex("""\b$monthStringMatched\b""", RegexOption.IGNORE_CASE), " ")
        }

        // Limpiar conectores residuales al principio o final ("en", "de", "del", "durante", etc.)
        val cleanKeyword = cleanEntityKeyword(text)

        // 6. Cálculo de rangos de fecha y etiqueta visual del periodo
        val (startDate, endDate, periodLabel) = calculateDateBounds(
            year = detectedYear,
            month = detectedMonth,
            relative = detectedRelativePeriod,
            nowMillis = clockNowMillis,
            timeZone = timeZone
        )

        return FinancialIntent(
            cleanKeyword = cleanKeyword.ifBlank { rawQuery.trim('?', '¿', '!', '¡', ' ') },
            year = detectedYear,
            month = detectedMonth,
            relativePeriod = detectedRelativePeriod,
            type = detectedType,
            displayPeriodLabel = periodLabel,
            startDateMillis = startDate,
            endDateMillis = endDate,
            originalQuery = rawQuery
        )
    }

    private fun cleanEntityKeyword(text: String): String {
        var result = text.trim('?', '¿', '!', '¡', '.', ':', ',', ';', ' ')
        val connectors = listOf("en", "de", "del", "para", "sobre", "durante")

        var changed = true
        while (changed) {
            changed = false
            val parts = result.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (parts.isEmpty()) break

            if (parts.first().lowercase() in connectors) {
                result = parts.drop(1).joinToString(" ")
                changed = true
            } else if (parts.last().lowercase() in connectors) {
                result = parts.dropLast(1).joinToString(" ")
                changed = true
            }
        }

        return result.trim('?', '¿', '!', '¡', '.', ':', ',', ';', ' ')
    }

    private fun calculateDateBounds(
        year: Int?,
        month: Int?,
        relative: RelativePeriod?,
        nowMillis: Long,
        timeZone: TimeZone
    ): Triple<Long?, Long?, String?> {
        val nowInstant = kotlinx.datetime.Instant.fromEpochMilliseconds(nowMillis)
        val today = nowInstant.toLocalDateTime(timeZone).date

        // Prioridad 1: Año y Mes especificados explícitamente (ej. "marzo de 2026")
        if (year != null && month != null) {
            val start = LocalDate(year, month, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val nextMonthDate = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
            val end = nextMonthDate.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
            val monthName = spanishMonths.entries.firstOrNull { it.value == month }?.key?.replaceFirstChar { it.uppercase() } ?: "Mes $month"
            return Triple(start, end, "$monthName $year")
        }

        // Prioridad 2: Solo Año especificado (ej. "en 2026")
        if (year != null) {
            val start = LocalDate(year, 1, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val end = LocalDate(year + 1, 1, 1).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
            return Triple(start, end, "$year")
        }

        // Prioridad 3: Solo Mes especificado (se asume el año en curso)
        if (month != null) {
            val targetYear = today.year
            val start = LocalDate(targetYear, month, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val nextMonthDate = if (month == 12) LocalDate(targetYear + 1, 1, 1) else LocalDate(targetYear, month + 1, 1)
            val end = nextMonthDate.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
            val monthName = spanishMonths.entries.firstOrNull { it.value == month }?.key?.replaceFirstChar { it.uppercase() } ?: "Mes $month"
            return Triple(start, end, "$monthName $targetYear")
        }

        // Prioridad 4: Periodo relativo
        if (relative != null) {
            return when (relative) {
                RelativePeriod.CURRENT_MONTH -> {
                    val start = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    Triple(start, nowMillis, "Este Mes")
                }
                RelativePeriod.PREVIOUS_MONTH -> {
                    val prevMonthDate = today.minus(1, DateTimeUnit.MONTH)
                    val start = LocalDate(prevMonthDate.year, prevMonthDate.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val end = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
                    Triple(start, end, "El Mes Pasado")
                }
                RelativePeriod.CURRENT_YEAR -> {
                    val start = LocalDate(today.year, 1, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    Triple(start, nowMillis, "Este Año")
                }
                RelativePeriod.PREVIOUS_YEAR -> {
                    val prevYear = today.year - 1
                    val start = LocalDate(prevYear, 1, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val end = LocalDate(today.year, 1, 1).atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
                    Triple(start, end, "$prevYear")
                }
                RelativePeriod.LAST_3_MONTHS -> {
                    val start = nowInstant.minus(90, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()
                    Triple(start, nowMillis, "Últimos 3 Meses")
                }
                RelativePeriod.LAST_6_MONTHS -> {
                    val start = nowInstant.minus(180, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()
                    Triple(start, nowMillis, "Últimos 6 Meses")
                }
                RelativePeriod.CURRENT_WEEK -> {
                    val start = nowInstant.minus(7, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()
                    Triple(start, nowMillis, "Esta Semana")
                }
                RelativePeriod.TODAY -> {
                    val start = today.atStartOfDayIn(timeZone).toEpochMilliseconds()
                    Triple(start, nowMillis, "Hoy")
                }
                RelativePeriod.YESTERDAY -> {
                    val yesterday = today.minus(1, DateTimeUnit.DAY)
                    val start = yesterday.atStartOfDayIn(timeZone).toEpochMilliseconds()
                    val end = today.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
                    Triple(start, end, "Ayer")
                }
            }
        }

        return Triple(null, null, null)
    }
}
