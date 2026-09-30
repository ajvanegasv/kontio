package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Utilidades para formatear y sanitizar entradas numéricas y monetarias
 * con separador de miles (,) y decimales (.).
 */
object NumberInputFormatter {

    /**
     * Formatea un String numérico crudo (ej: "1234567.89", "5000000", "0.")
     * insertando separadores de miles en la parte entera y preservando la parte decimal.
     */
    fun formatNumberString(text: String): String {
        if (text.isEmpty()) return ""

        val isNegative = text.startsWith("-")
        val raw = if (isNegative) text.substring(1) else text

        if (raw.isEmpty()) return if (isNegative) "-" else ""

        val dotIndex = raw.indexOf('.')
        val integerPart = if (dotIndex != -1) raw.substring(0, dotIndex) else raw
        val decimalPart = if (dotIndex != -1) raw.substring(dotIndex) else ""

        val formattedInt = if (integerPart.isEmpty()) {
            ""
        } else {
            integerPart.reversed().chunked(3).joinToString(",").reversed()
        }

        val sign = if (isNegative) "-" else ""
        return "$sign$formattedInt$decimalPart"
    }

    /**
     * Sanitiza la entrada del usuario en TextFields para que solo contenga
     * dígitos válidos, a lo sumo un punto decimal y hasta [maxDecimals] decimales.
     * Convierte comas individuales a puntos decimales y remueve separadores de miles pegados.
     */
    fun cleanNumericInput(
        input: String,
        allowNegative: Boolean = true,
        allowDecimals: Boolean = true,
        maxDecimals: Int = 2
    ): String {
        if (input.isEmpty()) return ""

        val isNegative = allowNegative && input.startsWith("-")
        val withoutSign = if (input.startsWith("-")) input.substring(1) else input

        // Manejo de valores pegados con formato (ej: "1,234,567.89" o "1.234.567,89")
        var normalized = withoutSign
        if (normalized.contains(',') && normalized.contains('.')) {
            val lastComma = normalized.lastIndexOf(',')
            val lastDot = normalized.lastIndexOf('.')
            if (lastDot > lastComma) {
                // Formato inglés/americano: comas son miles, punto es decimal
                normalized = normalized.replace(",", "")
            } else {
                // Formato europeo/hispano: puntos son miles, coma es decimal
                normalized = normalized.replace(".", "").replace(',', '.')
            }
        } else if (normalized.contains(',')) {
            val commaCount = normalized.count { it == ',' }
            if (commaCount > 1) {
                // Múltiples comas -> separadores de miles pegados
                normalized = normalized.replace(",", "")
            } else {
                // Una sola coma -> usuario la pulsó como separador decimal
                normalized = normalized.replace(',', '.')
            }
        }

        val sb = StringBuilder()
        var hasDot = false
        var decimalDigitsCount = 0

        for (ch in normalized) {
            if (ch in '0'..'9') {
                if (hasDot) {
                    if (decimalDigitsCount < maxDecimals) {
                        sb.append(ch)
                        decimalDigitsCount++
                    }
                } else {
                    sb.append(ch)
                }
            } else if (ch == '.' && allowDecimals && !hasDot) {
                hasDot = true
                if (sb.isEmpty()) {
                    sb.append('0')
                }
                sb.append('.')
            }
        }

        var result = sb.toString()

        // Si empieza con 0 seguido de otro dígito que no sea punto (ej: "05"), quitar el 0 inicial
        if (result.startsWith("0") && result.length > 1 && result[1] != '.') {
            result = result.dropWhile { it == '0' }.ifEmpty { "0" }
        }

        return if (isNegative && result.isNotEmpty()) "-$result" else if (isNegative) "-" else result
    }
}

/**
 * VisualTransformation de Compose para OutlinedTextField / TextField que muestra
 * el número con separadores de miles y decimales en tiempo real manteniendo el texto
 * subyacente limpio para parsing y cálculos.
 */
class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val orig = text.text
        if (orig.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val trans = NumberInputFormatter.formatNumberString(orig)
        if (trans == orig) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val origToTrans = IntArray(orig.length + 1)
        val transToOrig = IntArray(trans.length + 1)

        var origIdx = 0
        origToTrans[0] = 0
        transToOrig[0] = 0

        for (transIdx in 0 until trans.length) {
            val transChar = trans[transIdx]
            if (origIdx < orig.length && transChar == orig[origIdx]) {
                origIdx++
                origToTrans[origIdx] = transIdx + 1
                transToOrig[transIdx + 1] = origIdx
            } else {
                transToOrig[transIdx + 1] = origIdx
                if (origIdx <= orig.length) {
                    origToTrans[origIdx] = transIdx + 1
                }
            }
        }

        while (origIdx < orig.length) {
            origIdx++
            origToTrans[origIdx] = trans.length
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return origToTrans[offset.coerceIn(0, origToTrans.lastIndex)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transToOrig[offset.coerceIn(0, transToOrig.lastIndex)]
            }
        }

        return TransformedText(AnnotatedString(trans), offsetMapping)
    }
}
