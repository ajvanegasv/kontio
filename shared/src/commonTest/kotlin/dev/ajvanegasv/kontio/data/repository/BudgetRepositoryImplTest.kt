package dev.ajvanegasv.kontio.data.repository

import dev.ajvanegasv.kontio.data.local.dao.BudgetDao
import dev.ajvanegasv.kontio.data.local.dao.BudgetTransactionDao
import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.dao.TransactionDao
import dev.ajvanegasv.kontio.data.local.entity.BudgetEntity
import dev.ajvanegasv.kontio.data.local.entity.BudgetTransactionEntity
import dev.ajvanegasv.kontio.data.local.entity.CategoryEntity
import dev.ajvanegasv.kontio.data.local.entity.TransactionEntity
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeBudgetDao : BudgetDao {
    val budgets = MutableStateFlow<Map<String, BudgetEntity>>(emptyMap())

    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgets.map { it.values.toList() }
    override suspend fun getAllBudgetsDirect(): List<BudgetEntity> = budgets.value.values.toList()
    override fun getBudgetById(id: String): Flow<BudgetEntity?> = budgets.map { it[id] }
    override suspend fun getBudgetByIdDirect(id: String): BudgetEntity? = budgets.value[id]
    override fun getBudgetsByCategoryId(categoryId: String): Flow<List<BudgetEntity>> =
        budgets.map { it.values.filter { b -> b.categoryId == categoryId } }

    override suspend fun insertBudget(budget: BudgetEntity) {
        budgets.value = budgets.value + (budget.id to budget)
    }

    override suspend fun insertBudgets(budgetsList: List<BudgetEntity>) {
        budgets.value = budgets.value + budgetsList.associateBy { it.id }
    }

    override suspend fun updateBudget(budget: BudgetEntity) {
        budgets.value = budgets.value + (budget.id to budget)
    }

    override suspend fun deleteBudget(id: String) {
        budgets.value = budgets.value - id
    }

    override suspend fun deleteAllBudgets() {
        budgets.value = emptyMap()
    }

    override suspend fun getBudgetsCount(): Int = budgets.value.size
}

private class FakeCategoryDao : CategoryDao {
    val categories = MutableStateFlow<Map<String, CategoryEntity>>(emptyMap())

    override fun getAllCategories(): Flow<List<CategoryEntity>> = categories.map { it.values.toList() }
    override fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categories.map { it.values.filter { c -> c.type == type } }
    override fun getCategoryById(id: String): Flow<CategoryEntity?> = categories.map { it[id] }
    override suspend fun getCategoryByIdDirect(id: String): CategoryEntity? = categories.value[id]
    override suspend fun getAllCategoriesDirect(): List<CategoryEntity> = categories.value.values.toList()
    override suspend fun insertCategory(category: CategoryEntity) {
        categories.value = categories.value + (category.id to category)
    }
    override suspend fun insertCategories(categoriesList: List<CategoryEntity>) {
        categories.value = categories.value + categoriesList.associateBy { it.id }
    }
    override suspend fun deleteAllCategories() {
        categories.value = emptyMap()
    }
    override suspend fun getCategoriesCount(): Int = categories.value.size
    override suspend fun deleteCategoryById(id: String) {
        categories.value = categories.value - id
    }
}

private class FakeTransactionDao : TransactionDao {
    val transactions = MutableStateFlow<Map<String, TransactionEntity>>(emptyMap())

    override fun getAllTransactions(): Flow<List<TransactionEntity>> = transactions.map { it.values.toList() }
    override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactions.map { it.values.sortedByDescending { t -> t.timestamp }.take(limit) }
    override fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>> =
        transactions.map { it.values.filter { t -> t.accountId == accountId } }
    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactions.map { it.values.filter { t -> t.timestamp in startDate..endDate } }
    override suspend fun getAllTransactionsDirect(): List<TransactionEntity> = transactions.value.values.toList()

    override suspend fun insertTransaction(transaction: TransactionEntity) {
        transactions.value = transactions.value + (transaction.id to transaction)
    }

    override suspend fun insertTransactions(transactionsList: List<TransactionEntity>) {
        transactions.value = transactions.value + transactionsList.associateBy { it.id }
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value - id
    }

    override suspend fun deleteAllTransactions() {
        transactions.value = emptyMap()
    }

    override suspend fun getTransactionByIdDirect(id: String): TransactionEntity? = transactions.value[id]
    override fun getTransactionById(id: String): Flow<TransactionEntity?> = transactions.map { it[id] }
    override suspend fun deleteTransactionsByAccountId(accountId: String) {
        transactions.value = transactions.value.filterValues { it.accountId != accountId && it.targetAccountId != accountId }
    }

    override suspend fun getTransactionsCount(): Int = transactions.value.size
    override suspend fun getTransactionsCountByCategoryId(categoryId: String): Int =
        transactions.value.values.count { it.categoryId == categoryId }

    override suspend fun searchTransactionsByNote(query: String): List<TransactionEntity> =
        transactions.value.values.filter { it.note.contains(query, ignoreCase = true) }

    override suspend fun getTransactionsInDateRangeDirect(startDate: Long, endDate: Long): List<TransactionEntity> =
        transactions.value.values.filter { it.timestamp in startDate..endDate }
}

private class FakeBudgetTransactionDao : BudgetTransactionDao {
    val links = MutableStateFlow<Map<String, BudgetTransactionEntity>>(emptyMap())

    override fun getAllBudgetTransactions(): Flow<List<BudgetTransactionEntity>> =
        links.map { it.values.toList() }

    override suspend fun getAllBudgetTransactionsDirect(): List<BudgetTransactionEntity> =
        links.value.values.toList()

    override fun getTransactionsForBudget(budgetId: String): Flow<List<BudgetTransactionEntity>> =
        links.map { it.values.filter { l -> l.budgetId == budgetId } }

    override suspend fun getTransactionsForBudgetDirect(budgetId: String): List<BudgetTransactionEntity> =
        links.value.values.filter { it.budgetId == budgetId }

    override suspend fun getByTransactionId(transactionId: String): BudgetTransactionEntity? =
        links.value.values.firstOrNull { it.transactionId == transactionId }

    override suspend fun insertBudgetTransaction(entity: BudgetTransactionEntity) {
        links.value = links.value + (entity.id to entity)
    }

    override suspend fun insertBudgetTransactions(entities: List<BudgetTransactionEntity>) {
        links.value = links.value + entities.associateBy { it.id }
    }

    override suspend fun deleteBudgetTransaction(id: String) {
        links.value = links.value - id
    }

    override suspend fun deleteByTransactionId(transactionId: String) {
        links.value = links.value.filterValues { it.transactionId != transactionId }
    }

    override suspend fun deleteByBudgetId(budgetId: String) {
        links.value = links.value.filterValues { it.budgetId != budgetId }
    }

    override suspend fun deleteLink(budgetId: String, transactionId: String) {
        links.value = links.value.filterValues { !(it.budgetId == budgetId && it.transactionId == transactionId) }
    }

    override suspend fun deleteAllBudgetTransactions() {
        links.value = emptyMap()
    }

    override suspend fun getBudgetTransactionsCount(): Int = links.value.size
}

class BudgetRepositoryImplTest {

    private val category = CategoryEntity(
        id = "cat-dining",
        name = "Restaurantes",
        iconName = "restaurant",
        colorHex = "#EF4444",
        type = "EXPENSE",
        isDefault = false
    )

    private val budget = Budget(
        id = "bgt-1",
        name = "Presupuesto Restaurantes",
        categoryId = "cat-dining",
        limitAmount = 100.0,
        period = BudgetPeriod.MONTHLY
    )

    @Test
    fun testBudgetSpendingDoesNotIncludeUnlinkedCategoryTransactions(): Unit = runBlocking {
        val budgetDao = FakeBudgetDao()
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetTxDao = FakeBudgetTransactionDao()

        val repository = BudgetRepositoryImpl(
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetTransactionDao = budgetTxDao
        )

        categoryDao.insertCategory(category)
        repository.createBudget(budget)

        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        // Transacción genérica en la misma categoría pero NO vinculada al presupuesto
        val genericTx = TransactionEntity(
            id = "tx-generic-1",
            accountId = "acc-1",
            categoryId = "cat-dining",
            type = "EXPENSE",
            amount = 150.0,
            currency = "USD",
            timestamp = now,
            note = "Cena familiar",
            targetAccountId = null,
            budgetId = null,
            aiMetadataJson = null
        )
        transactionDao.insertTransaction(genericTx)

        val budgetsWithProgress = repository.getBudgetsWithProgress().first()
        assertEquals(1, budgetsWithProgress.size)
        val progress = budgetsWithProgress.first()

        // El presupuesto no debe tomar la transacción genérica no vinculada
        assertEquals(0.0, progress.spentAmount)
        assertEquals(100.0, progress.remainingAmount)
        assertFalse(progress.isExceeded)
        assertEquals(0.0, progress.exceededAmount)
        assertEquals(0, progress.transactionsCount)
    }

    @Test
    fun testBudgetSpendingIncludesLinkedTransactionsAndCalculatesExceeded(): Unit = runBlocking {
        val budgetDao = FakeBudgetDao()
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetTxDao = FakeBudgetTransactionDao()

        val repository = BudgetRepositoryImpl(
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetTransactionDao = budgetTxDao
        )

        categoryDao.insertCategory(category)
        repository.createBudget(budget)

        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val tx1 = TransactionEntity(
            id = "tx-1",
            accountId = "acc-1",
            categoryId = "cat-dining",
            type = "EXPENSE",
            amount = 60.0,
            currency = "USD",
            timestamp = now - 5000,
            note = "Almuerzo de trabajo",
            targetAccountId = null,
            budgetId = null,
            aiMetadataJson = null
        )
        transactionDao.insertTransaction(tx1)
        repository.linkTransactionToBudget(budget.id, tx1.id)

        var list = repository.getBudgetsWithProgress().first()
        var progress = list.first()
        assertEquals(60.0, progress.spentAmount)
        assertEquals(40.0, progress.remainingAmount)
        assertFalse(progress.isExceeded)
        assertEquals(0.0, progress.exceededAmount)
        assertEquals(1, progress.transactionsCount)
        assertNotNull(progress.lastTransaction)
        assertEquals(60.0, progress.lastTransaction?.amount)

        // Registrar y vincular segunda transacción de $50 (Total: 110 > límite 100)
        val tx2 = TransactionEntity(
            id = "tx-2",
            accountId = "acc-1",
            categoryId = "cat-dining",
            type = "EXPENSE",
            amount = 50.0,
            currency = "USD",
            timestamp = now,
            note = "Cena de fin de semana",
            targetAccountId = null,
            budgetId = null,
            aiMetadataJson = null
        )
        transactionDao.insertTransaction(tx2)
        repository.linkTransactionToBudget(budget.id, tx2.id)

        list = repository.getBudgetsWithProgress().first()
        progress = list.first()
        assertEquals(110.0, progress.spentAmount)
        assertEquals(0.0, progress.remainingAmount)
        assertTrue(progress.isExceeded)
        assertEquals(10.0, progress.exceededAmount)
        assertEquals(2, progress.transactionsCount)
        assertNotNull(progress.lastTransaction)
        assertEquals(50.0, progress.lastTransaction?.amount)
        assertEquals("Cena de fin de semana", progress.lastTransaction?.note)
    }

    @Test
    fun testDeleteBudgetRemovesAssociatedLinks(): Unit = runBlocking {
        val budgetDao = FakeBudgetDao()
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val budgetTxDao = FakeBudgetTransactionDao()

        val repository = BudgetRepositoryImpl(
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            budgetTransactionDao = budgetTxDao
        )

        categoryDao.insertCategory(category)
        repository.createBudget(budget)
        repository.linkTransactionToBudget(budget.id, "tx-dummy-1")
        repository.linkTransactionToBudget(budget.id, "tx-dummy-2")

        assertEquals(2, budgetTxDao.links.value.size)

        repository.deleteBudget(budget.id)

        assertEquals(0, budgetDao.budgets.value.size)
        assertEquals(0, budgetTxDao.links.value.size)
    }
}
