package dev.ajvanegasv.kontio.presentation.categories

import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateCategoryUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteCategoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeTestCategoryRepo : CategoryRepository {
    private val categoriesMap = MutableStateFlow<Map<String, Category>>(emptyMap())

    override fun getCategories(type: TransactionType?): Flow<List<Category>> {
        return categoriesMap.map { map ->
            val list = map.values.toList()
            if (type != null) list.filter { it.type == type } else list
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return categoriesMap.map { it[id] }
    }

    override suspend fun insertCategory(category: Category) {
        categoriesMap.value = categoriesMap.value + (category.id to category)
    }

    override suspend fun deleteCategory(id: String) {
        categoriesMap.value = categoriesMap.value - id
    }

    override suspend fun seedDefaultCategoriesIfEmpty() {
        if (categoriesMap.value.isEmpty()) {
            val defaults = Category.defaultCategories().associateBy { it.id }
            categoriesMap.value = defaults
        }
    }

    override suspend fun getCategoriesCount(): Int = categoriesMap.value.size
}

private class FakeTestTransactionRepo : TransactionRepository {
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactions.map { it.take(limit) }

    override fun getAllTransactions(): Flow<List<Transaction>> = transactions

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.accountId == accountId } }

    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.timestamp in startDate..endDate } }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactions.value = transactions.value + transaction
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.value = transactions.value.filter { it.id != id }
    }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactions.value.firstOrNull { it.id == id }

    override suspend fun deleteTransactionsByAccountId(accountId: String) {
        transactions.value = transactions.value.filter { it.accountId != accountId }
    }

    override suspend fun getTransactionsCount(): Int = transactions.value.size

    override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
        transactions.value.count { it.categoryId == categoryId }
}

class CategoriesViewModelTest {

    @Test
    fun testInitialStateLoadsSeededCategories(): Unit = runBlocking {
        val categoryRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTransactionRepo()
        categoryRepo.seedDefaultCategoriesIfEmpty()

        val createCategoryUseCase = CreateCategoryUseCase(categoryRepo)
        val deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepo, txRepo)

        val viewModel = CategoriesViewModel(
            categoryRepository = categoryRepo,
            createCategoryUseCase = createCategoryUseCase,
            deleteCategoryUseCase = deleteCategoryUseCase,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = viewModel.uiState.first { it.allCategories.isNotEmpty() }
        assertEquals(13, state.allCategories.size)
        assertEquals(13, state.filteredCategories.size)
        assertEquals(CategoryFilter.ALL, state.filter)
        assertFalse(state.isAddCategoryOpen)
        assertNull(state.categoryPendingDelete)
    }

    @Test
    fun testFilteringByExpenseAndIncome(): Unit = runBlocking {
        val categoryRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTransactionRepo()
        categoryRepo.seedDefaultCategoriesIfEmpty()

        val viewModel = CategoriesViewModel(
            categoryRepository = categoryRepo,
            createCategoryUseCase = CreateCategoryUseCase(categoryRepo),
            deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepo, txRepo),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        // Filter by EXPENSE
        viewModel.setFilter(CategoryFilter.EXPENSE)
        val expenseState = viewModel.uiState.first { it.filter == CategoryFilter.EXPENSE }
        assertEquals(8, expenseState.filteredCategories.size)
        assertTrue(expenseState.filteredCategories.all { it.type == TransactionType.EXPENSE })

        // Filter by INCOME
        viewModel.setFilter(CategoryFilter.INCOME)
        val incomeState = viewModel.uiState.first { it.filter == CategoryFilter.INCOME }
        assertEquals(5, incomeState.filteredCategories.size)
        assertTrue(incomeState.filteredCategories.all { it.type == TransactionType.INCOME })

        // Back to ALL
        viewModel.setFilter(CategoryFilter.ALL)
        val allState = viewModel.uiState.first { it.filter == CategoryFilter.ALL }
        assertEquals(13, allState.filteredCategories.size)
    }

    @Test
    fun testDeleteDefaultCategoryFlow(): Unit = runBlocking {
        val categoryRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTransactionRepo()
        categoryRepo.seedDefaultCategoriesIfEmpty()

        val viewModel = CategoriesViewModel(
            categoryRepository = categoryRepo,
            createCategoryUseCase = CreateCategoryUseCase(categoryRepo),
            deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepo, txRepo),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = viewModel.uiState.first { it.allCategories.isNotEmpty() }
        val defaultCategory = state.allCategories.first { it.id == "cat_food" }
        assertTrue(defaultCategory.isDefault)

        // Solicitar eliminación de la categoría por defecto
        viewModel.requestDeleteCategory(defaultCategory)
        val pendingState = viewModel.uiState.first { it.categoryPendingDelete != null }
        assertEquals("cat_food", pendingState.categoryPendingDelete?.id)

        // Confirmar eliminación
        viewModel.confirmDeleteCategory()
        val updatedState = viewModel.uiState.first { it.categoryPendingDelete == null }
        assertNull(updatedState.categoryPendingDelete)
        assertNull(updatedState.errorMessage)
        assertEquals(12, updatedState.allCategories.size)
        assertFalse(updatedState.allCategories.any { it.id == "cat_food" })
    }

    @Test
    fun testDeleteDefaultCategoryBlockedWhenTransactionsExist(): Unit = runBlocking {
        val categoryRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTransactionRepo()
        categoryRepo.seedDefaultCategoriesIfEmpty()

        // Asociar transacción
        txRepo.insertTransaction(
            Transaction(
                id = "tx-1",
                accountId = "acc-1",
                categoryId = "cat_salary",
                type = TransactionType.INCOME,
                amount = 1000.0,
                timestamp = 1000L
            )
        )

        val viewModel = CategoriesViewModel(
            categoryRepository = categoryRepo,
            createCategoryUseCase = CreateCategoryUseCase(categoryRepo),
            deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepo, txRepo),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        val state = viewModel.uiState.first { it.allCategories.isNotEmpty() }
        val salaryCategory = state.allCategories.first { it.id == "cat_salary" }

        // Solicitar y confirmar eliminación
        viewModel.requestDeleteCategory(salaryCategory)
        viewModel.confirmDeleteCategory()

        val errorState = viewModel.uiState.first { it.errorMessage != null }
        assertNotNull(errorState.errorMessage)
        assertTrue(errorState.errorMessage.contains("transacciones asociadas"))
        assertEquals(13, errorState.allCategories.size)
        assertTrue(errorState.allCategories.any { it.id == "cat_salary" })

        // Limpiar error
        viewModel.clearError()
        val clearedState = viewModel.uiState.first { it.errorMessage == null }
        assertNull(clearedState.errorMessage)
    }

    @Test
    fun testCreateCategoryAndOpenCloseModal(): Unit = runBlocking {
        val categoryRepo = FakeTestCategoryRepo()
        val txRepo = FakeTestTransactionRepo()

        val viewModel = CategoriesViewModel(
            categoryRepository = categoryRepo,
            createCategoryUseCase = CreateCategoryUseCase(categoryRepo),
            deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepo, txRepo),
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.openAddCategory()
        val openState = viewModel.uiState.first { it.isAddCategoryOpen }
        assertTrue(openState.isAddCategoryOpen)

        var successCalled = false
        viewModel.createCategory(
            name = "Gimnasio",
            iconName = "fitness_center",
            colorHex = "#3B82F6",
            type = TransactionType.EXPENSE,
            onSuccess = { successCalled = true }
        )

        assertTrue(successCalled)
        val stateAfter = viewModel.uiState.first { it.allCategories.isNotEmpty() }
        assertFalse(stateAfter.isAddCategoryOpen)
        assertEquals(1, stateAfter.allCategories.size)
        assertEquals("Gimnasio", stateAfter.allCategories.first().name)
        assertFalse(stateAfter.allCategories.first().isDefault)
    }
}
