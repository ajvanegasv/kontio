package dev.ajvanegasv.kontio.presentation.budgets

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Budget
import dev.ajvanegasv.kontio.domain.model.BudgetPeriod
import dev.ajvanegasv.kontio.domain.model.BudgetWithProgress
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetBudgetsWithProgressUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateBudgetUseCase
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

private class FakeBudgetTestRepo : BudgetRepository {
    private val budgetsMap = MutableStateFlow<Map<String, Budget>>(emptyMap())

    override fun getBudgets(): Flow<List<Budget>> = budgetsMap.map { it.values.toList() }

    override fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>> {
        return budgetsMap.map { map ->
            map.values.map { budget ->
                BudgetWithProgress(
                    budget = budget,
                    category = budget.category,
                    spentAmount = 0.0,
                    limitAmount = budget.limitAmount,
                    remainingAmount = budget.limitAmount,
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
        budgetsMap.value = budgetsMap.value + (budget.id to budget)
        return Result.success(Unit)
    }

    override suspend fun deleteBudget(id: String): Result<Unit> {
        budgetsMap.value = budgetsMap.value - id
        return Result.success(Unit)
    }

    override suspend fun getBudgetsCount(): Int = budgetsMap.value.size
}

private class FakeCategoryTestRepo : CategoryRepository {
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

    override suspend fun seedDefaultCategoriesIfEmpty() {}

    override suspend fun getCategoriesCount(): Int = categories.value.size
}

private class FakeAccountTestRepo : AccountRepository {
    private val accounts = MutableStateFlow<Map<String, Account>>(emptyMap())

    override fun getAccounts(): Flow<List<Account>> = accounts.map { it.values.toList() }
    override fun getAccountById(id: String): Flow<Account?> = accounts.map { it[id] }

    override suspend fun insertAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateBalance(accountId: String, newBalance: Double) {
        val current = accounts.value[accountId] ?: return
        accounts.value = accounts.value + (accountId to current.copy(balance = newBalance))
    }

    override suspend fun archiveAccount(id: String) {
        val current = accounts.value[id] ?: return
        accounts.value = accounts.value + (id to current.copy(isArchived = true))
    }

    override suspend fun deleteAccount(id: String) {
        accounts.value = accounts.value - id
    }

    override suspend fun getAccountsCount(): Int = accounts.value.size
}

private class FakeTransactionTestRepo : TransactionRepository {
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())

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

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        this.transactions.value = this.transactions.value + transactions
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

class BudgetsViewModelTest {

    private val expenseCategory = Category(
        id = "cat_servicios",
        name = "Servicios",
        iconName = "water_drop",
        colorHex = "#3B82F6",
        type = TransactionType.EXPENSE
    )

    private val incomeCategory = Category(
        id = "cat_salario",
        name = "Salario",
        iconName = "payments",
        colorHex = "#10B981",
        type = TransactionType.INCOME
    )

    private val primaryAccount = Account(
        id = "acc_bank",
        name = "Banco Principal",
        type = AccountType.CHECKING,
        balance = 1000.0,
        currency = "USD",
        colorHex = "#3B82F6"
    )

    private fun setupViewModel(): Triple<BudgetsViewModel, FakeBudgetTestRepo, FakeTransactionTestRepo> {
        val budgetRepo = FakeBudgetTestRepo()
        val categoryRepo = FakeCategoryTestRepo()
        val accountRepo = FakeAccountTestRepo()
        val transactionRepo = FakeTransactionTestRepo()

        runBlocking {
            categoryRepo.insertCategory(expenseCategory)
            categoryRepo.insertCategory(incomeCategory)
            accountRepo.insertAccount(primaryAccount)
        }

        val getBudgetsWithProgressUseCase = GetBudgetsWithProgressUseCase(budgetRepo)
        val createBudgetUseCase = CreateBudgetUseCase(budgetRepo, categoryRepo)
        val updateBudgetUseCase = UpdateBudgetUseCase(budgetRepo, categoryRepo)
        val deleteBudgetUseCase = DeleteBudgetUseCase(budgetRepo)
        val createTransactionUseCase = CreateTransactionUseCase(transactionRepo, accountRepo)

        val viewModel = BudgetsViewModel(
            getBudgetsWithProgressUseCase = getBudgetsWithProgressUseCase,
            categoryRepository = categoryRepo,
            accountRepository = accountRepo,
            createBudgetUseCase = createBudgetUseCase,
            updateBudgetUseCase = updateBudgetUseCase,
            deleteBudgetUseCase = deleteBudgetUseCase,
            createTransactionUseCase = createTransactionUseCase,
            coroutineScope = CoroutineScope(Dispatchers.Unconfined)
        )

        return Triple(viewModel, budgetRepo, transactionRepo)
    }

    @Test
    fun testInitialUiStateAndExpenseCategoriesFilter(): Unit = runBlocking {
        val (viewModel, _, _) = setupViewModel()

        val state = viewModel.uiState.first()
        // Expense categories should be filtered so only EXPENSE appears
        assertEquals(1, state.categories.size)
        assertEquals("cat_servicios", state.categories[0].id)
        assertEquals(1, state.accounts.size)
        assertEquals(0, state.budgets.size)
        assertEquals(0.0, state.totalBudgeted)
        assertFalse(state.isAddBudgetOpen)
        assertNull(state.editingBudget)
        assertNull(state.budgetPendingDelete)
        assertNull(state.budgetForPayment)
    }

    @Test
    fun testOpenAndCloseAddBudgetDialog(): Unit = runBlocking {
        val (viewModel, _, _) = setupViewModel()

        viewModel.openAddBudget()
        var state = viewModel.uiState.first { it.isAddBudgetOpen }
        assertTrue(state.isAddBudgetOpen)
        assertNull(state.editingBudget)

        viewModel.closeAddBudget()
        state = viewModel.uiState.first { !it.isAddBudgetOpen }
        assertFalse(state.isAddBudgetOpen)
    }

    @Test
    fun testCreateBudgetFlow(): Unit = runBlocking {
        val (viewModel, budgetRepo, _) = setupViewModel()

        viewModel.openAddBudget()
        viewModel.saveBudget(
            name = "Servicio de Agua",
            categoryId = expenseCategory.id,
            limitAmount = 75.0,
            period = BudgetPeriod.MONTHLY,
            currency = "USD",
            note = "Pago mensual"
        )

        val state = viewModel.uiState.first { it.budgets.isNotEmpty() }
        assertEquals(1, state.budgets.size)
        assertEquals("Servicio de Agua", state.budgets[0].budget.name)
        assertEquals(75.0, state.budgets[0].limitAmount)
        assertEquals(75.0, state.totalBudgeted)
        assertFalse(state.isAddBudgetOpen)
        assertEquals(1, budgetRepo.getBudgetsCount())
    }

    @Test
    fun testEditBudgetFlow(): Unit = runBlocking {
        val (viewModel, budgetRepo, _) = setupViewModel()

        // Create initial budget
        viewModel.saveBudget(
            name = "Luz",
            categoryId = expenseCategory.id,
            limitAmount = 100.0,
            period = BudgetPeriod.MONTHLY,
            currency = "USD",
            note = ""
        )

        val createdState = viewModel.uiState.first { it.budgets.isNotEmpty() }
        val created = createdState.budgets.first().budget

        viewModel.openEditBudget(created)
        var state = viewModel.uiState.first { it.editingBudget != null }
        assertTrue(state.isAddBudgetOpen)
        assertEquals("Luz", state.editingBudget?.name)

        // Save updated budget
        viewModel.saveBudget(
            name = "Energía Eléctrica",
            categoryId = expenseCategory.id,
            limitAmount = 120.0,
            period = BudgetPeriod.MONTHLY,
            currency = "USD",
            note = "Aumento"
        )

        state = viewModel.uiState.first { it.editingBudget == null && !it.isAddBudgetOpen }
        assertFalse(state.isAddBudgetOpen)
        assertNull(state.editingBudget)

        val updatedBudget = budgetRepo.getBudgetByIdDirect(created.id)
        assertNotNull(updatedBudget)
        assertEquals("Energía Eléctrica", updatedBudget.name)
        assertEquals(120.0, updatedBudget.limitAmount)
    }

    @Test
    fun testDeleteBudgetFlow(): Unit = runBlocking {
        val (viewModel, budgetRepo, _) = setupViewModel()

        viewModel.saveBudget(
            name = "Gasolina",
            categoryId = expenseCategory.id,
            limitAmount = 200.0,
            period = BudgetPeriod.MONTHLY,
            currency = "USD",
            note = ""
        )

        val createdState = viewModel.uiState.first { it.budgets.isNotEmpty() }
        val budgetItem = createdState.budgets.first()

        viewModel.requestDeleteBudget(budgetItem)
        var state = viewModel.uiState.first { it.budgetPendingDelete != null }
        assertEquals(budgetItem.budget.id, state.budgetPendingDelete?.budget?.id)

        // Confirm delete
        viewModel.confirmDeleteBudget()
        state = viewModel.uiState.first { it.budgetPendingDelete == null }
        assertNull(state.budgetPendingDelete)
        assertEquals(0, budgetRepo.getBudgetsCount())
    }

    @Test
    fun testQuickPaymentFlow(): Unit = runBlocking {
        val (viewModel, _, txRepo) = setupViewModel()

        viewModel.saveBudget(
            name = "Internet",
            categoryId = expenseCategory.id,
            limitAmount = 50.0,
            period = BudgetPeriod.MONTHLY,
            currency = "USD",
            note = "Fibra óptica"
        )

        val budgetItem = viewModel.uiState.first { it.budgets.isNotEmpty() }.budgets.first()

        // Open quick payment dialog
        viewModel.openQuickPayment(budgetItem)
        var state = viewModel.uiState.first { it.budgetForPayment != null }
        assertEquals(budgetItem.budget.id, state.budgetForPayment?.budget?.id)

        // Submit quick payment
        viewModel.executeQuickPayment(
            amount = 45.0,
            accountId = primaryAccount.id,
            dateMillis = 1727600000000L,
            note = "Pago del mes de Octubre"
        )

        state = viewModel.uiState.first { it.budgetForPayment == null }
        assertNull(state.budgetForPayment)
        assertFalse(state.isSubmittingPayment)

        // Verify transaction created
        val transactions = txRepo.transactions.value
        assertEquals(1, transactions.size)
        val tx = transactions[0]
        assertEquals(45.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(primaryAccount.id, tx.accountId)
        assertEquals(expenseCategory.id, tx.categoryId)
        assertEquals(budgetItem.budget.id, tx.budgetId)
        assertEquals("Pago del mes de Octubre", tx.note)
    }
}
