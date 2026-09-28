package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AnalyticsTimeframe
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeAnalyticsCategoryRepository(
    initialCategories: List<Category> = Category.defaultCategories()
) : CategoryRepository {
    private val categories = MutableStateFlow(initialCategories)

    override fun getCategories(type: TransactionType?): Flow<List<Category>> {
        return categories.map { list ->
            if (type != null) list.filter { it.type == type } else list
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> =
        categories.map { it.firstOrNull { cat -> cat.id == id } }

    override suspend fun insertCategory(category: Category) {
        categories.value = categories.value + category
    }

    override suspend fun deleteCategory(id: String) {
        categories.value = categories.value.filterNot { it.id == id }
    }

    override suspend fun seedDefaultCategoriesIfEmpty() {}
    override suspend fun getCategoriesCount(): Int = categories.value.size
}

class FakeAnalyticsTransactionRepository(
    initialTransactions: List<Transaction> = emptyList()
) : TransactionRepository {
    private val transactions = MutableStateFlow(initialTransactions)

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactions.map { it.take(limit) }

    override fun getAllTransactions(): Flow<List<Transaction>> = transactions

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.accountId == accountId } }

    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.timestamp in startDate..endDate } }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactions.value = listOf(transaction) + transactions.value
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        this.transactions.value = transactions + this.transactions.value
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value.filterNot { it.id == id }
    }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactions.value.firstOrNull { it.id == id }

    override suspend fun deleteTransactionsByAccountId(accountId: String) {
        transactions.value = transactions.value.filterNot { it.accountId == accountId }
    }

    override suspend fun getTransactionsCount(): Int = transactions.value.size

    override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
        transactions.value.count { it.categoryId == categoryId }

    override suspend fun searchTransactions(query: String): List<Transaction> {
        val q = query.lowercase().trim()
        return transactions.value.filter {
            it.note.lowercase().contains(q) || (it.category?.name?.lowercase()?.contains(q) == true)
        }
    }

    override suspend fun getTransactionsInDateRangeDirect(startDate: Long, endDate: Long): List<Transaction> =
        transactions.value.filter { it.timestamp in startDate..endDate }
}

class GetAnalyticsSummaryUseCaseTest {

    @Test
    fun calculateSummary_correctlyGroupsExpensesAndComputesPercentages() = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        val foodCat = Category(id = "cat_food", name = "Alimentación", iconName = "restaurant", colorHex = "#F59E0B", type = TransactionType.EXPENSE)
        val transportCat = Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE)

        val txList = listOf(
            Transaction(id = "tx1", accountId = "acc1", categoryId = "cat_food", type = TransactionType.EXPENSE, amount = 150.0, timestamp = now, note = "Supermercado", category = foodCat),
            Transaction(id = "tx2", accountId = "acc1", categoryId = "cat_food", type = TransactionType.EXPENSE, amount = 50.0, timestamp = now, note = "Cena", category = foodCat),
            Transaction(id = "tx3", accountId = "acc1", categoryId = "cat_transport", type = TransactionType.EXPENSE, amount = 100.0, timestamp = now, note = "Gasolina", category = transportCat),
            Transaction(id = "tx4", accountId = "acc1", categoryId = "cat_salary", type = TransactionType.INCOME, amount = 1000.0, timestamp = now, note = "Sueldo")
        )

        val catRepo = FakeAnalyticsCategoryRepository(listOf(foodCat, transportCat))
        val txRepo = FakeAnalyticsTransactionRepository(txList)
        val useCase = GetAnalyticsSummaryUseCase(txRepo, catRepo)

        val summary = useCase(AnalyticsTimeframe.ALL_TIME).first()

        assertEquals(1000.0, summary.totalIncome)
        assertEquals(300.0, summary.totalExpenses)
        assertEquals(700.0, summary.netSavings)
        assertEquals(70.0f, summary.savingsRate)
        assertEquals(2, summary.categorySpendings.size)

        // Verificamos orden descendente por gasto: Alimentación ($200) primero, Transporte ($100) segundo
        val top1 = summary.categorySpendings[0]
        assertEquals("cat_food", top1.categoryId)
        assertEquals(200.0, top1.totalAmount)
        assertEquals(2, top1.transactionCount)
        assertTrue(top1.percentage > 66.0f && top1.percentage < 67.0f) // 200/300 = 66.6%

        val top2 = summary.categorySpendings[1]
        assertEquals("cat_transport", top2.categoryId)
        assertEquals(100.0, top2.totalAmount)
        assertEquals(1, top2.transactionCount)
        assertTrue(top2.percentage > 33.0f && top2.percentage < 34.0f) // 100/300 = 33.3%
    }
}
