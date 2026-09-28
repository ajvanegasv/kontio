package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FinancialQueryParserTest {

    @Test
    fun parseQuery_extractsUberAndYear2026() {
        val query = "¿Cuánto he gastado en Uber en 2026?"
        val intent = FinancialQueryParser.parseQuery(query)

        assertEquals("Uber", intent.cleanKeyword)
        assertEquals(2026, intent.year)
        assertNull(intent.month)
        assertEquals("2026", intent.displayPeriodLabel)
        assertEquals(TransactionType.EXPENSE, intent.type)
        assertNotNull(intent.startDateMillis)
        assertNotNull(intent.endDateMillis)
    }

    @Test
    fun parseQuery_extractsNetflixAndPreviousMonth() {
        val query = "Gastos en Netflix el mes pasado"
        val intent = FinancialQueryParser.parseQuery(query)

        assertEquals("Netflix", intent.cleanKeyword)
        assertEquals(RelativePeriod.PREVIOUS_MONTH, intent.relativePeriod)
        assertEquals("El Mes Pasado", intent.displayPeriodLabel)
        assertEquals(TransactionType.EXPENSE, intent.type)
        assertNotNull(intent.startDateMillis)
        assertNotNull(intent.endDateMillis)
    }

    @Test
    fun parseQuery_extractsMonthAndYear() {
        val query = "¿Cuánto pagué en restaurantes en marzo de 2026?"
        val intent = FinancialQueryParser.parseQuery(query)

        assertEquals("restaurantes", intent.cleanKeyword)
        assertEquals(3, intent.month)
        assertEquals(2026, intent.year)
        assertEquals("Marzo 2026", intent.displayPeriodLabel)
        assertEquals(TransactionType.EXPENSE, intent.type)
    }

    @Test
    fun parseQuery_extractsCurrentYear() {
        val query = "Compras este año"
        val intent = FinancialQueryParser.parseQuery(query)

        assertEquals("Compras", intent.cleanKeyword)
        assertEquals(RelativePeriod.CURRENT_YEAR, intent.relativePeriod)
        assertEquals("Este Año", intent.displayPeriodLabel)
    }

    @Test
    fun parseQuery_plainMerchantWithoutDates() {
        val query = "Uber"
        val intent = FinancialQueryParser.parseQuery(query)

        assertEquals("Uber", intent.cleanKeyword)
        assertNull(intent.year)
        assertNull(intent.month)
        assertNull(intent.relativePeriod)
        assertNull(intent.displayPeriodLabel)
    }
}
