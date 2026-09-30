package dev.ajvanegasv.kontio.presentation.util

import kotlin.math.abs

object CurrencyFormatter {
    fun format(amount: Double, currency: String = "USD", showSign: Boolean = false): String {
        val isNegative = amount < 0
        val absValue = abs(amount)

        // Formato con 2 decimales y separador de miles
        val longPart = absValue.toLong()
        val decimalPart = ((absValue - longPart) * 100).toLong()

        val longString = longPart.toString().reversed().chunked(3).joinToString(",").reversed()
        val decimalString = decimalPart.toString().padStart(2, '0')

        val sign = if (isNegative) "-" else if (showSign && amount > 0) "+" else ""
        val symbol = getSymbol(currency)

        return "$sign$symbol$longString.$decimalString"
    }

    fun getSymbol(currency: String = "USD"): String {
        return when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "COP" -> "$"
            else -> "$"
        }
    }
}
