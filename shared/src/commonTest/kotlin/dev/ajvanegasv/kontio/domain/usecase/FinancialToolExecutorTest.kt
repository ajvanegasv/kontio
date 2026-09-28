package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinancialToolExecutorTest {

    @Test
    fun executeSearchTransactions_returnsAccurateReportAndChartBarsForUber() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val transportCat = Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)

        val txList = listOf(
            Transaction(id = "tx1", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 25.0, timestamp = now - 86400000L * 2, note = "Uber Trip Aeropuerto", category = transportCat),
            Transaction(id = "tx2", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 15.0, timestamp = now - 86400000L, note = "Uber Oficina", category = transportCat),
            Transaction(id = "tx3", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 35.0, timestamp = now, note = "Uber Noche", category = transportCat),
            Transaction(id = "tx4", accountId = "acc1", categoryId = "cat_food", type = TransactionType.EXPENSE, amount = 50.0, timestamp = now, note = "Restaurante Italiano")
        )

        val catRepo = FakeAnalyticsCategoryRepository(listOf(transportCat))
        val txRepo = FakeAnalyticsTransactionRepository(txList)
        val executor = FinancialToolExecutor(txRepo, catRepo)

        val report = executor.executeSearchTransactions("Uber")

        assertEquals("Uber", report.query)
        assertEquals(75.0, report.totalAmount)
        assertEquals(3, report.transactionCount)
        assertEquals(25.0, report.averageAmount)
        assertEquals(3, report.chartBars.size)
        assertEquals(3, report.matchingTransactions.size)
        assertTrue(report.aiAdvice.contains("Uber"))
        assertTrue(report.chartBars.all { it.heightRatio in 0.1f..1.0f })
    }

    @Test
    fun executeSearchTransactions_returnsFriendlyEmptyStateWhenNoMatches() = runBlocking {
        val catRepo = FakeAnalyticsCategoryRepository()
        val txRepo = FakeAnalyticsTransactionRepository(emptyList())
        val executor = FinancialToolExecutor(txRepo, catRepo)

        val report = executor.executeSearchTransactions("Inexistente")

        assertEquals(0.0, report.totalAmount)
        assertEquals(0, report.transactionCount)
        assertTrue(report.chartBars.isEmpty())
        assertTrue(report.matchingTransactions.isEmpty())
        assertTrue(report.aiAdvice.contains("No se encontraron movimientos"))
    }

    @Test
    fun executeSearchTransactions_withYearFilter2026_excludesOtherYears() = runBlocking {
        val transportCat = Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)

        // 2025: e.g. 15 de junio 2025
        val tx2025 = Transaction(id = "tx2025", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 100.0, timestamp = 1750000000000L, note = "Uber 2025", category = transportCat)
        // 2026: e.g. 10 de marzo 2026 y 15 de marzo 2026
        val tx2026_1 = Transaction(id = "tx2026_1", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 30.0, timestamp = 1773187200000L, note = "Uber Trabajo", category = transportCat)
        val tx2026_2 = Transaction(id = "tx2026_2", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 20.0, timestamp = 1773619200000L, note = "Uber Casa", category = transportCat)

        val catRepo = FakeAnalyticsCategoryRepository(listOf(transportCat))
        val txRepo = FakeAnalyticsTransactionRepository(listOf(tx2025, tx2026_1, tx2026_2))
        val executor = FinancialToolExecutor(txRepo, catRepo)

        val intent = FinancialQueryParser.parseQuery("¿cuánto he gastado en Uber en 2026?")

        val report = executor.executeSearchTransactions(
            query = intent.cleanKeyword,
            year = intent.year,
            month = intent.month,
            startDate = intent.startDateMillis,
            endDate = intent.endDateMillis,
            displayPeriodLabel = intent.displayPeriodLabel
        )

        assertEquals("Uber", report.query)
        assertEquals("Reporte de Gastos: Uber (2026)", report.title)
        assertEquals(50.0, report.totalAmount)
        assertEquals(2, report.transactionCount)
        assertEquals(25.0, report.averageAmount)
        assertEquals(2, report.matchingTransactions.size)
    }

    @Test
    fun executeSearchTransactions_whenNoMatchesInYear_indicatesMatchesInOtherPeriods() = runBlocking {
        val transportCat = Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)
        val tx2025 = Transaction(id = "tx2025", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 50.0, timestamp = 1750000000000L, note = "Uber 2025", category = transportCat)

        val catRepo = FakeAnalyticsCategoryRepository(listOf(transportCat))
        val txRepo = FakeAnalyticsTransactionRepository(listOf(tx2025))
        val executor = FinancialToolExecutor(txRepo, catRepo)

        val intent = FinancialQueryParser.parseQuery("¿cuánto he gastado en Uber en 2026?")

        val report = executor.executeSearchTransactions(
            query = intent.cleanKeyword,
            year = intent.year,
            month = intent.month,
            startDate = intent.startDateMillis,
            endDate = intent.endDateMillis,
            displayPeriodLabel = intent.displayPeriodLabel
        )

        assertEquals(0.0, report.totalAmount)
        assertEquals(0, report.transactionCount)
        assertTrue(report.aiAdvice.contains("No se encontraron movimientos de 'Uber' en 2026"))
        assertTrue(report.aiAdvice.contains("otros periodos anteriores"))
    }
}
