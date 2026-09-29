package dev.ajvanegasv.kontio.domain.agent

import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.agent.tools.GetFinancialOverviewTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetRecentTransactionsTool
import dev.ajvanegasv.kontio.domain.agent.tools.SearchTransactionsTool
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsCategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsTransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.FinancialToolExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransactionQueryToolsTest {

    @Test
    fun searchTransactionsTool_findsMatchesAndConstructsVisualPayload() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val cat = Category(id = "c1", name = "Transporte", iconName = "car", colorHex = "#123", type = TransactionType.EXPENSE)
        val txList = listOf(
            Transaction(id = "tx1", accountId = "a1", categoryId = "c1", type = TransactionType.EXPENSE, amount = 25.0, timestamp = now, note = "Uber Oficina", category = cat),
            Transaction(id = "tx2", accountId = "a1", categoryId = "c1", type = TransactionType.EXPENSE, amount = 15.0, timestamp = now, note = "Uber Casa", category = cat)
        )
        val catRepo = FakeAnalyticsCategoryRepository(listOf(cat))
        val txRepo = FakeAnalyticsTransactionRepository(txList)
        val executor = FinancialToolExecutor(txRepo, catRepo)

        val tool = SearchTransactionsTool(executor)
        val result = tool.execute(mapOf("query" to JsonPrimitive("Uber")))

        assertTrue(result.success)
        assertEquals("search_transactions", result.toolName)
        val payload = result.visualPayload as? AgentVisualPayload.TransactionListPayload
        assertNotNull(payload)
        assertEquals(2, payload.count)
        assertEquals(40.0, payload.totalAmountRaw)
    }

    @Test
    fun getRecentTransactionsTool_respectsLimit() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val txList = (1..15).map { idx ->
            Transaction(id = "tx$idx", accountId = "a1", categoryId = "c1", type = TransactionType.EXPENSE, amount = idx.toDouble(), timestamp = now + idx, note = "Gasto $idx")
        }
        val txRepo = FakeAnalyticsTransactionRepository(txList)
        val tool = GetRecentTransactionsTool(txRepo)

        val result = tool.execute(mapOf("limit" to JsonPrimitive(5)))

        assertTrue(result.success)
        val payload = result.visualPayload as? AgentVisualPayload.TransactionListPayload
        assertNotNull(payload)
        assertEquals(5, payload.count)
    }

    @Test
    fun getFinancialOverviewTool_computesIncomeExpenseAndSavingsRate() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val txList = listOf(
            Transaction(id = "t1", accountId = "a1", categoryId = "c1", type = TransactionType.INCOME, amount = 1000.0, timestamp = now, note = "Sueldo"),
            Transaction(id = "t2", accountId = "a1", categoryId = "c2", type = TransactionType.EXPENSE, amount = 400.0, timestamp = now, note = "Arriendo")
        )
        val txRepo = FakeAnalyticsTransactionRepository(txList)
        val tool = GetFinancialOverviewTool(txRepo)

        val result = tool.execute(mapOf("period" to JsonPrimitive("CURRENT_MONTH")))

        assertTrue(result.success)
        val payload = result.visualPayload as? AgentVisualPayload.FinancialOverviewPayload
        assertNotNull(payload)
        assertEquals(60.0f, payload.savingsRate)
    }
}
