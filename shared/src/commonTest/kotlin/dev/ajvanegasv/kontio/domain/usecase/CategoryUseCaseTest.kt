package dev.ajvanegasv.kontio.domain.usecase

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow<Map<String, Category>>(emptyMap())

    override fun getCategories(type: TransactionType?): Flow<List<Category>> = categories.map { map ->
        if (type != null) map.values.filter { it.type == type }
        else map.values.toList()
    }

    override fun getCategoryById(id: String): Flow<Category?> = categories.map { it[id] }

    override suspend fun insertCategory(category: Category) {
        categories.value = categories.value + (category.id to category)
    }

    override suspend fun deleteCategory(id: String) {
        categories.value = categories.value - id
    }

    override suspend fun seedDefaultCategoriesIfEmpty() {
        if (categories.value.isEmpty()) {
            val defaults = Category.defaultCategories().associateBy { it.id }
            categories.value = defaults
        }
    }

    override suspend fun getCategoriesCount(): Int = categories.value.size
}

class FakeCategoryTestTransactionRepository : TransactionRepository {
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())

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
        transactions.value = transactions.value.filterNot {
            it.accountId == accountId || it.targetAccountId == accountId
        }
    }

    override suspend fun getTransactionsCount(): Int = transactions.value.size

    override suspend fun getTransactionsCountByCategory(categoryId: String): Int =
        transactions.value.count { it.categoryId == categoryId }
}

class CategoryUseCaseTest {

    @Test
    fun testCreateCategorySuccess(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val useCase = CreateCategoryUseCase(categoryRepo)

        val result = useCase(
            name = "Gimnasio",
            iconName = "fitness",
            colorHex = "#EF4444",
            type = TransactionType.EXPENSE
        )

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertTrue(created.id.startsWith("cat_"))
        assertEquals("Gimnasio", created.name)
        assertEquals("fitness", created.iconName)
        assertEquals("#EF4444", created.colorHex)
        assertEquals(TransactionType.EXPENSE, created.type)
        assertFalse(created.isDefault)

        // Verificamos que se guardó en el repositorio
        val categoriesInRepo = categoryRepo.getCategories().first()
        assertEquals(1, categoriesInRepo.size)
        assertEquals("Gimnasio", categoriesInRepo[0].name)
    }

    @Test
    fun testCreateCategoryFailsWhenNameIsBlank(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val useCase = CreateCategoryUseCase(categoryRepo)

        val resultEmpty = useCase(
            name = "",
            iconName = "restaurant",
            colorHex = "#F59E0B",
            type = TransactionType.EXPENSE
        )
        assertTrue(resultEmpty.isFailure)
        assertTrue(resultEmpty.exceptionOrNull() is IllegalArgumentException)

        val resultWhitespace = useCase(
            name = "   ",
            iconName = "restaurant",
            colorHex = "#F59E0B",
            type = TransactionType.EXPENSE
        )
        assertTrue(resultWhitespace.isFailure)
        assertTrue(resultWhitespace.exceptionOrNull() is IllegalArgumentException)

        assertEquals(0, categoryRepo.getCategoriesCount())
    }

    @Test
    fun testCreateCategoryWithDefaultFallbackColorAndIcon(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val useCase = CreateCategoryUseCase(categoryRepo)

        val result = useCase(
            name = "Test Fallbacks",
            iconName = "",
            colorHex = "",
            type = TransactionType.INCOME
        )

        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("category", created.iconName)
        assertEquals("#3B82F6", created.colorHex)
    }

    @Test
    fun testCreateCategoryWithCategoryObject(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val useCase = CreateCategoryUseCase(categoryRepo)

        val customCategory = Category(
            id = "cat_custom_1",
            name = "Freelance Extra",
            iconName = "work",
            colorHex = "#06B6D4",
            type = TransactionType.INCOME,
            isDefault = false
        )

        val result = useCase(customCategory)
        assertTrue(result.isSuccess)

        val inRepo = categoryRepo.getCategoryById("cat_custom_1").first()
        assertEquals("Freelance Extra", inRepo?.name)
    }

    @Test
    fun testDeleteCategorySuccessWhenNoTransactions(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val txRepo = FakeCategoryTestTransactionRepository()
        val deleteUseCase = DeleteCategoryUseCase(categoryRepo, txRepo)

        val category = Category(
            id = "cat_to_delete",
            name = "Viajes",
            iconName = "flight",
            colorHex = "#EC4899",
            type = TransactionType.EXPENSE,
            isDefault = false
        )
        categoryRepo.insertCategory(category)

        assertEquals(1, categoryRepo.getCategoriesCount())

        val result = deleteUseCase("cat_to_delete")
        assertTrue(result.isSuccess)

        // Verificamos que se eliminó
        assertEquals(0, categoryRepo.getCategoriesCount())
        assertNull(categoryRepo.getCategoryById("cat_to_delete").first())
    }

    @Test
    fun testDeleteCategoryFailsWhenTransactionsExist(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val txRepo = FakeCategoryTestTransactionRepository()
        val deleteUseCase = DeleteCategoryUseCase(categoryRepo, txRepo)

        val category = Category(
            id = "cat_with_tx",
            name = "Alimentación",
            iconName = "restaurant",
            colorHex = "#F59E0B",
            type = TransactionType.EXPENSE,
            isDefault = false
        )
        categoryRepo.insertCategory(category)

        // Insertamos transacción vinculada a esta categoría
        txRepo.insertTransaction(
            Transaction(
                id = "tx_101",
                accountId = "acc_cash",
                categoryId = "cat_with_tx",
                type = TransactionType.EXPENSE,
                amount = 45.0,
                timestamp = 1000L
            )
        )

        val result = deleteUseCase("cat_with_tx")
        assertTrue(result.isFailure)

        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertEquals("No se puede eliminar la categoría porque tiene transacciones asociadas", exception.message)

        // La categoría NO debe haber sido eliminada
        assertEquals(1, categoryRepo.getCategoriesCount())
        assertNotNull(categoryRepo.getCategoryById("cat_with_tx").first())
    }

    @Test
    fun testDeleteCategoryFailsWhenIdIsBlank(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val txRepo = FakeCategoryTestTransactionRepository()
        val deleteUseCase = DeleteCategoryUseCase(categoryRepo, txRepo)

        val result = deleteUseCase("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testDeleteDefaultCategorySuccessWhenNoTransactions(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val txRepo = FakeCategoryTestTransactionRepository()
        val deleteUseCase = DeleteCategoryUseCase(categoryRepo, txRepo)

        // Poblamos las categorías por defecto
        categoryRepo.seedDefaultCategoriesIfEmpty()
        val initialCount = categoryRepo.getCategoriesCount()
        assertEquals(13, initialCount)

        // Verificamos que cat_food es por defecto
        val foodBefore = categoryRepo.getCategoryById("cat_food").first()
        assertNotNull(foodBefore)
        assertTrue(foodBefore.isDefault)

        // Eliminamos la categoría por defecto
        val result = deleteUseCase("cat_food")
        assertTrue(result.isSuccess)

        // Verificamos que se eliminó correctamente y las demás se mantienen
        assertEquals(initialCount - 1, categoryRepo.getCategoriesCount())
        assertNull(categoryRepo.getCategoryById("cat_food").first())
        assertNotNull(categoryRepo.getCategoryById("cat_transport").first())
    }

    @Test
    fun testDeleteDefaultCategoryFailsWhenTransactionsExist(): Unit = runBlocking {
        val categoryRepo = FakeCategoryRepository()
        val txRepo = FakeCategoryTestTransactionRepository()
        val deleteUseCase = DeleteCategoryUseCase(categoryRepo, txRepo)

        // Poblamos las categorías por defecto
        categoryRepo.seedDefaultCategoriesIfEmpty()

        // Asociamos una transacción a cat_salary
        txRepo.insertTransaction(
            Transaction(
                id = "tx_salary_1",
                accountId = "acc_cash",
                categoryId = "cat_salary",
                type = TransactionType.INCOME,
                amount = 2500.0,
                timestamp = 2000L
            )
        )

        // Intentar eliminar la categoría debe fallar
        val result = deleteUseCase("cat_salary")
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IllegalStateException)
        assertEquals("No se puede eliminar la categoría porque tiene transacciones asociadas", ex.message)

        // La categoría por defecto debe permanecer
        assertNotNull(categoryRepo.getCategoryById("cat_salary").first())
    }
}
