package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
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

class FakeBudgetRepository(
    private val categoryRepository: CategoryRepository? = null,
    private val transactionsFlow: Flow<List<Transaction>>? = null
) : BudgetRepository {
    private val budgetsMap = MutableStateFlow<Map<String, Budget>>(emptyMap())

    override fun getBudgets(): Flow<List<Budget>> = budgetsMap.map { it.values.toList() }

    override fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>> {
        return budgetsMap.map { map ->
            map.values.map { budget ->
                val spent = 0.0
                val limit = budget.limitAmount
                BudgetWithProgress(
                    budget = budget,
                    category = budget.category,
                    spentAmount = spent,
                    limitAmount = limit,
                    remainingAmount = limit,
                    percentage = 0f,
                    isExceeded = false,
                    exceededAmount = 0.0,
                    transactionsCount = 0
                )
            }
        }
    }

    override fun getBudgetById(id: String): Flow<Budget?> = budgetsMap.map { it[id] }

    override suspend fun getBudgetByIdDirect(id: String): Budget? = budgetsMap.value[id]

    override suspend fun createBudget(budget: Budget): Result<Unit> {
        budgetsMap.value = budgetsMap.value + (budget.id to budget)
        return Result.success(Unit)
    }

    override suspend fun updateBudget(budget: Budget): Result<Unit> {
        if (!budgetsMap.value.containsKey(budget.id)) {
            return Result.failure(IllegalArgumentException("Budget not found"))
        }
        budgetsMap.value = budgetsMap.value + (budget.id to budget)
        return Result.success(Unit)
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        budgetsMap.value = budgetsMap.value - id
        return Result.success(Unit)
    }

    override suspend fun getBudgetsCount(): Int = budgetsMap.value.size
}

class BudgetUseCaseTest {

    private val sampleCategory = Category(
        id = "cat_services",
        name = "Servicios",
        iconName = "water_drop",
        colorHex = "#3B82F6",
        type = TransactionType.EXPENSE
    )

    private fun setupEnvironment(): Triple<FakeBudgetRepository, FakeCategoryRepository, Category> {
        val categoryRepo = FakeCategoryRepository()
        runBlocking {
            categoryRepo.insertCategory(sampleCategory)
        }
        val budgetRepo = FakeBudgetRepository(categoryRepo)
        return Triple(budgetRepo, categoryRepo, sampleCategory)
    }

    @Test
    fun testCreateBudgetSuccess(): Unit = runBlocking {
        val (budgetRepo, categoryRepo, category) = setupEnvironment()
        val createUseCase = CreateBudgetUseCase(budgetRepo, categoryRepo)

        val result = createUseCase(
            name = "Pago de Agua y Luz",
            categoryId = category.id,
            limitAmount = 150.0,
            period = BudgetPeriod.MONTHLY,
            currency = "USD",
            note = "Mensual para servicios básicos"
        )

        assertTrue(result.isSuccess)
        val budget = result.getOrNull()
        assertNotNull(budget)
        assertEquals("Pago de Agua y Luz", budget.name)
        assertEquals(category.id, budget.categoryId)
        assertEquals(150.0, budget.limitAmount)
        assertEquals(BudgetPeriod.MONTHLY, budget.period)
        assertEquals("USD", budget.currency)
        assertEquals("Mensual para servicios básicos", budget.note)

        // Verify stored in repository
        val stored = budgetRepo.getBudgetByIdDirect(budget.id)
        assertNotNull(stored)
        assertEquals(1, budgetRepo.getBudgetsCount())
    }

    @Test
    fun testCreateBudgetValidationFailures(): Unit = runBlocking {
        val (budgetRepo, categoryRepo, category) = setupEnvironment()
        val createUseCase = CreateBudgetUseCase(budgetRepo, categoryRepo)

        // Blank name
        val blankNameResult = createUseCase(
            name = "   ",
            categoryId = category.id,
            limitAmount = 100.0
        )
        assertTrue(blankNameResult.isFailure)
        assertEquals("El nombre del presupuesto no puede estar vacío", blankNameResult.exceptionOrNull()?.message)

        // Zero limit
        val zeroLimitResult = createUseCase(
            name = "Comida",
            categoryId = category.id,
            limitAmount = 0.0
        )
        assertTrue(zeroLimitResult.isFailure)
        assertEquals("El monto límite debe ser mayor a 0", zeroLimitResult.exceptionOrNull()?.message)

        // Negative limit
        val negativeLimitResult = createUseCase(
            name = "Comida",
            categoryId = category.id,
            limitAmount = -25.0
        )
        assertTrue(negativeLimitResult.isFailure)
        assertEquals("El monto límite debe ser mayor a 0", negativeLimitResult.exceptionOrNull()?.message)

        // Non-existent category
        val invalidCategoryResult = createUseCase(
            name = "Streaming",
            categoryId = "non_existent_category",
            limitAmount = 50.0
        )
        assertTrue(invalidCategoryResult.isFailure)
        assertEquals("La categoría seleccionada no existe", invalidCategoryResult.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateBudgetSuccess(): Unit = runBlocking {
        val (budgetRepo, categoryRepo, category) = setupEnvironment()
        val createUseCase = CreateBudgetUseCase(budgetRepo, categoryRepo)
        val updateUseCase = UpdateBudgetUseCase(budgetRepo, categoryRepo)

        val created = createUseCase(
            name = "Supermercado",
            categoryId = category.id,
            limitAmount = 300.0
        ).getOrThrow()

        val updateResult = updateUseCase(
            id = created.id,
            name = "Supermercado y Mercado",
            categoryId = category.id,
            limitAmount = 450.0,
            period = BudgetPeriod.MONTHLY,
            note = "Incremento por compras adicionales"
        )

        assertTrue(updateResult.isSuccess)
        val updated = updateResult.getOrThrow()
        assertEquals("Supermercado y Mercado", updated.name)
        assertEquals(450.0, updated.limitAmount)
        assertEquals("Incremento por compras adicionales", updated.note)

        val stored = budgetRepo.getBudgetByIdDirect(created.id)
        assertNotNull(stored)
        assertEquals(450.0, stored.limitAmount)
        assertEquals("Supermercado y Mercado", stored.name)
    }

    @Test
    fun testUpdateBudgetNotFoundAndValidation(): Unit = runBlocking {
        val (budgetRepo, categoryRepo, category) = setupEnvironment()
        val updateUseCase = UpdateBudgetUseCase(budgetRepo, categoryRepo)

        // Non-existent ID
        val notFoundResult = updateUseCase(
            id = "non_existent_id",
            name = "Presupuesto",
            categoryId = category.id,
            limitAmount = 100.0
        )
        assertTrue(notFoundResult.isFailure)
        assertTrue(notFoundResult.exceptionOrNull()?.message?.contains("no existe") == true)

        // Blank name
        val blankNameResult = updateUseCase(
            id = "some_id",
            name = "",
            categoryId = category.id,
            limitAmount = 100.0
        )
        assertTrue(blankNameResult.isFailure)

        // Invalid limit
        val invalidLimitResult = updateUseCase(
            id = "some_id",
            name = "Valido",
            categoryId = category.id,
            limitAmount = -10.0
        )
        assertTrue(invalidLimitResult.isFailure)
    }

    @Test
    fun testDeleteBudgetSuccess(): Unit = runBlocking {
        val (budgetRepo, categoryRepo, category) = setupEnvironment()
        val createUseCase = CreateBudgetUseCase(budgetRepo, categoryRepo)
        val deleteUseCase = DeleteBudgetUseCase(budgetRepo)

        val created = createUseCase(
            name = "Gimnasio",
            categoryId = category.id,
            limitAmount = 60.0
        ).getOrThrow()

        assertEquals(1, budgetRepo.getBudgetsCount())

        val deleteResult = deleteUseCase(created.id)
        assertTrue(deleteResult.isSuccess)
        assertEquals(0, budgetRepo.getBudgetsCount())
        assertNull(budgetRepo.getBudgetByIdDirect(created.id))
    }

    @Test
    fun testGetBudgetsWithProgressUseCase(): Unit = runBlocking {
        val (budgetRepo, categoryRepo, category) = setupEnvironment()
        val createUseCase = CreateBudgetUseCase(budgetRepo, categoryRepo)
        val getWithProgressUseCase = GetBudgetsWithProgressUseCase(budgetRepo)

        createUseCase("Presupuesto 1", category.id, 100.0)
        createUseCase("Presupuesto 2", category.id, 200.0)

        val list = getWithProgressUseCase().first()
        assertEquals(2, list.size)
        assertEquals(100.0, list[0].limitAmount)
        assertEquals(200.0, list[1].limitAmount)
    }
}
