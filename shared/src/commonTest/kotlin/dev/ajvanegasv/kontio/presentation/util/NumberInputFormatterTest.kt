package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.ui.text.AnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberInputFormatterTest {

    @Test
    fun testFormatNumberString_basicNumbers() {
        assertEquals("", NumberInputFormatter.formatNumberString(""))
        assertEquals("0", NumberInputFormatter.formatNumberString("0"))
        assertEquals("5", NumberInputFormatter.formatNumberString("5"))
        assertEquals("50", NumberInputFormatter.formatNumberString("50"))
        assertEquals("500", NumberInputFormatter.formatNumberString("500"))
        assertEquals("1,000", NumberInputFormatter.formatNumberString("1000"))
        assertEquals("50,000", NumberInputFormatter.formatNumberString("50000"))
        assertEquals("500,000", NumberInputFormatter.formatNumberString("500000"))
        assertEquals("1,000,000", NumberInputFormatter.formatNumberString("1000000"))
        assertEquals("12,345,678", NumberInputFormatter.formatNumberString("12345678"))
    }

    @Test
    fun testFormatNumberString_withDecimals() {
        assertEquals("0.", NumberInputFormatter.formatNumberString("0."))
        assertEquals("0.5", NumberInputFormatter.formatNumberString("0.5"))
        assertEquals("0.50", NumberInputFormatter.formatNumberString("0.50"))
        assertEquals("1,234.", NumberInputFormatter.formatNumberString("1234."))
        assertEquals("1,234.5", NumberInputFormatter.formatNumberString("1234.5"))
        assertEquals("1,234.56", NumberInputFormatter.formatNumberString("1234.56"))
        assertEquals("1,000,000.99", NumberInputFormatter.formatNumberString("1000000.99"))
    }

    @Test
    fun testFormatNumberString_negativeNumbers() {
        assertEquals("-", NumberInputFormatter.formatNumberString("-"))
        assertEquals("-500", NumberInputFormatter.formatNumberString("-500"))
        assertEquals("-1,500", NumberInputFormatter.formatNumberString("-1500"))
        assertEquals("-1,500.50", NumberInputFormatter.formatNumberString("-1500.50"))
    }

    @Test
    fun testCleanNumericInput_standardCases() {
        assertEquals("1234", NumberInputFormatter.cleanNumericInput("1234"))
        assertEquals("1234.56", NumberInputFormatter.cleanNumericInput("1234.56"))
        // comma converted to dot
        assertEquals("1234.56", NumberInputFormatter.cleanNumericInput("1234,56"))
        // max decimals limit
        assertEquals("1234.56", NumberInputFormatter.cleanNumericInput("1234.5678"))
        // starting with dot becomes 0.
        assertEquals("0.5", NumberInputFormatter.cleanNumericInput(".5"))
        assertEquals("0.5", NumberInputFormatter.cleanNumericInput(",5"))
        // multiple dots ignored
        assertEquals("12.34", NumberInputFormatter.cleanNumericInput("12.3.4"))
        // leading zeros trimmed
        assertEquals("500", NumberInputFormatter.cleanNumericInput("0500"))
        assertEquals("0", NumberInputFormatter.cleanNumericInput("000"))
        assertEquals("0.5", NumberInputFormatter.cleanNumericInput("0.5"))
    }

    @Test
    fun testCleanNumericInput_pastedFormats() {
        // US formatted paste
        assertEquals("1234567.89", NumberInputFormatter.cleanNumericInput("1,234,567.89"))
        // European formatted paste
        assertEquals("1234567.89", NumberInputFormatter.cleanNumericInput("1.234.567,89"))
        // multiple commas thousands
        assertEquals("1000000", NumberInputFormatter.cleanNumericInput("1,000,000"))
    }

    @Test
    fun testThousandsSeparatorVisualTransformation() {
        val transformation = ThousandsSeparatorVisualTransformation()

        val input = AnnotatedString("1234567.89")
        val result = transformation.filter(input)

        assertEquals("1,234,567.89", result.text.text)

        // Test offset mapping
        // "1234567.89" -> "1,234,567.89"
        // 0 -> 0
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))

        // after '1' (orig offset 1) -> transformed offset 2 (after '1,')
        assertEquals(2, result.offsetMapping.originalToTransformed(1))

        // end of original string (offset 10) -> end of transformed (offset 12)
        assertEquals(12, result.offsetMapping.originalToTransformed(10))
        assertEquals(10, result.offsetMapping.transformedToOriginal(12))
    }
}
