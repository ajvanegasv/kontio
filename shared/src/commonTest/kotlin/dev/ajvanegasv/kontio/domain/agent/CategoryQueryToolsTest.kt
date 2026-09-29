package dev.ajvanegasv.kontio.domain.agent

import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.agent.tools.GetCategoriesTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetCategorySpendingTool
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsCategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeAnalyticsTransactionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CategoryQueryToolsTest {

    @Test
    fun getCategories_returnsRegisteredCategoriesGroupedByType() = runBlocking {
        val categories = listOf(
            Category(id = "cat_food", name = "Alimentación", iconName = "restaurant", colorHex = "#F59E0B", type = TransactionType.EXPENSE),
            Category(id = "cat_salary", name = "Salario", iconName = "payments", colorHex = "#10B981", type = TransactionType.INCOME)
        )
        val catRepo = FakeAnalyticsCategoryRepository(categories)
        val tool = GetCategoriesTool(catRepo)

        val result = tool.execute(emptyMap())

        assertTrue(result.success)
        assertEquals("get_categories", result.toolName)
        assertTrue(result.naturalLanguageSummary.contains("Alimentación"))
        assertTrue(result.naturalLanguageSummary.contains("Salario"))
    }

    @Test
    fun getCategorySpending_calculatesPercentagesAndSortsByHighestExpense() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val foodCat = Category(id = "cat_food", name = "Alimentación", iconName = "restaurant", colorHex = "#F59E0B", type = TransactionType.EXPENSE)
        val transportCat = Category(id = "cat_transp", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)

        val transactions = listOf(
            Transaction(id = "t1", accountId = "a1", categoryId = "cat_food", type = TransactionType.EXPENSE, amount = 150.0, timestamp = now, note = "Supermercado"),
            Transaction(id = "t2", accountId = "a1", categoryId = "cat_transp", type = TransactionType.EXPENSE, amount = 50.0, timestamp = now, note = "Uber"),
            Transaction(id = "t3", accountId = "a1", categoryId = "cat_food", type = TransactionType.EXPENSE, amount = 50.0, timestamp = now, note = "Almuerzo")
        )

        val catRepo = FakeAnalyticsCategoryRepository(listOf(foodCat, transportCat))
        val txRepo = FakeAnalyticsTransactionRepository(transactions)
        val tool = GetCategorySpendingTool(catRepo, txRepo)

        val result = tool.execute(mapOf("period" to JsonPrimitive("CURRENT_MONTH")))

        assertTrue(result.success)
        val payload = result.visualPayload as? AgentVisualPayload.CategoryBreakdownPayload
        assertNotNull(payload)
        assertEquals(2, payload.categories.size)
        // Alimentación: 200 / 250 = 80%
        assertEquals("Alimentación", payload.categories[0].name)
        assertEquals(200.0, payload.categories[0].amountRaw)
        assertEquals(80.0f, payload.categories[0].percentage)
        // Transporte: 50 / 250 = 20%
        assertEquals("Transporte", payload.categories[1].name)
        assertEquals(50.0, payload.categories[1].amountRaw)
        assertEquals(20.0f, payload.categories[1].percentage)
    }
}
