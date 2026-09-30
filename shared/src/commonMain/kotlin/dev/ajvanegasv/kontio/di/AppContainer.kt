package dev.ajvanegasv.kontio.di

import dev.ajvanegasv.kontio.data.backup.GoogleDriveBackupManager
import dev.ajvanegasv.kontio.data.local.KontioDatabase
import dev.ajvanegasv.kontio.data.local.createRoomDatabase
import dev.ajvanegasv.kontio.data.repository.AccountRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.BackupRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.BudgetRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.CategoryRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.TransactionRepositoryImpl
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.BackupRepository
import dev.ajvanegasv.kontio.domain.repository.BudgetRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.ArchiveAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateCategoryUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteCategoryUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetBudgetsWithProgressUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetDashboardSummaryUseCase
import dev.ajvanegasv.kontio.domain.usecase.ReassignTransactionsAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateBudgetUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetSavingsCashWidgetDataUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetCreditCardWidgetDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

object AppContainer {
    val database: KontioDatabase by lazy {
        createRoomDatabase()
    }

    val accountRepository: AccountRepository by lazy {
        AccountRepositoryImpl(database.accountDao())
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            accountDao = database.accountDao()
        )
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(
            budgetDao = database.budgetDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            budgetTransactionDao = database.budgetTransactionDao()
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            driveManager = GoogleDriveBackupManager(),
            budgetDao = database.budgetDao(),
            budgetTransactionDao = database.budgetTransactionDao()
        )
    }

    val getBudgetsWithProgressUseCase: GetBudgetsWithProgressUseCase by lazy {
        GetBudgetsWithProgressUseCase(budgetRepository = budgetRepository)
    }

    val createBudgetUseCase: CreateBudgetUseCase by lazy {
        CreateBudgetUseCase(
            budgetRepository = budgetRepository,
            categoryRepository = categoryRepository
        )
    }

    val updateBudgetUseCase: UpdateBudgetUseCase by lazy {
        UpdateBudgetUseCase(
            budgetRepository = budgetRepository,
            categoryRepository = categoryRepository
        )
    }

    val deleteBudgetUseCase: DeleteBudgetUseCase by lazy {
        DeleteBudgetUseCase(budgetRepository = budgetRepository)
    }

    val createTransactionUseCase: CreateTransactionUseCase by lazy {
        CreateTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository
        )
    }

    val updateTransactionUseCase: UpdateTransactionUseCase by lazy {
        UpdateTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository
        )
    }

    val reassignTransactionsAccountUseCase: ReassignTransactionsAccountUseCase by lazy {
        ReassignTransactionsAccountUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository
        )
    }

    val createAccountUseCase: CreateAccountUseCase by lazy {
        CreateAccountUseCase(
            accountRepository = accountRepository
        )
    }

    val updateAccountUseCase: UpdateAccountUseCase by lazy {
        UpdateAccountUseCase(
            accountRepository = accountRepository
        )
    }

    val deleteTransactionUseCase: DeleteTransactionUseCase by lazy {
        DeleteTransactionUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository
        )
    }

    val deleteAccountUseCase: DeleteAccountUseCase by lazy {
        DeleteAccountUseCase(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository
        )
    }

    val archiveAccountUseCase: ArchiveAccountUseCase by lazy {
        ArchiveAccountUseCase(accountRepository)
    }

    val createCategoryUseCase: CreateCategoryUseCase by lazy {
        CreateCategoryUseCase(
            categoryRepository = categoryRepository
        )
    }

    val deleteCategoryUseCase: DeleteCategoryUseCase by lazy {
        DeleteCategoryUseCase(
            categoryRepository = categoryRepository,
            transactionRepository = transactionRepository
        )
    }

    val getDashboardSummaryUseCase: GetDashboardSummaryUseCase by lazy {
        GetDashboardSummaryUseCase(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository
        )
    }

    val getSavingsCashWidgetDataUseCase: GetSavingsCashWidgetDataUseCase by lazy {
        GetSavingsCashWidgetDataUseCase(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository
        )
    }

    val getCreditCardWidgetDataUseCase: GetCreditCardWidgetDataUseCase by lazy {
        GetCreditCardWidgetDataUseCase(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository
        )
    }

    val aiConfigStorage: dev.ajvanegasv.kontio.data.local.AiConfigStorage by lazy {
        dev.ajvanegasv.kontio.data.local.getAiConfigStorage()
    }

    val themeConfigStorage: dev.ajvanegasv.kontio.data.local.ThemeConfigStorage by lazy {
        dev.ajvanegasv.kontio.data.local.getThemeConfigStorage()
    }

    val geminiApiClient: dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient by lazy {
        dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient()
    }

    val geminiStatementParser: dev.ajvanegasv.kontio.data.remote.gemini.GeminiStatementParser by lazy {
        dev.ajvanegasv.kontio.data.remote.gemini.GeminiStatementParser(geminiApiClient)
    }

    val analyzeBankStatementUseCase: dev.ajvanegasv.kontio.domain.usecase.AnalyzeBankStatementUseCase by lazy {
        dev.ajvanegasv.kontio.domain.usecase.AnalyzeBankStatementUseCase(
            geminiStatementParser = geminiStatementParser,
            categoryRepository = categoryRepository,
            accountRepository = accountRepository,
            transactionRepository = transactionRepository,
            aiConfigStorage = aiConfigStorage
        )
    }

    val batchImportTransactionsUseCase: dev.ajvanegasv.kontio.domain.usecase.BatchImportTransactionsUseCase by lazy {
        dev.ajvanegasv.kontio.domain.usecase.BatchImportTransactionsUseCase(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository
        )
    }

    val getAnalyticsSummaryUseCase: dev.ajvanegasv.kontio.domain.usecase.GetAnalyticsSummaryUseCase by lazy {
        dev.ajvanegasv.kontio.domain.usecase.GetAnalyticsSummaryUseCase(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }

    val financialToolExecutor: dev.ajvanegasv.kontio.domain.usecase.FinancialToolExecutor by lazy {
        dev.ajvanegasv.kontio.domain.usecase.FinancialToolExecutor(
            transactionRepository = transactionRepository,
            categoryRepository = categoryRepository
        )
    }

    val kontioToolRegistry: dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry by lazy {
        dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry(
            tools = listOf(
                dev.ajvanegasv.kontio.domain.agent.tools.GetAccountsSummaryTool(accountRepository = accountRepository),
                dev.ajvanegasv.kontio.domain.agent.tools.GetAccountDetailTool(accountRepository = accountRepository, transactionRepository = transactionRepository),
                dev.ajvanegasv.kontio.domain.agent.tools.GetCategoriesTool(categoryRepository = categoryRepository),
                dev.ajvanegasv.kontio.domain.agent.tools.GetCategorySpendingTool(categoryRepository = categoryRepository, transactionRepository = transactionRepository),
                dev.ajvanegasv.kontio.domain.agent.tools.SearchTransactionsTool(financialToolExecutor = financialToolExecutor),
                dev.ajvanegasv.kontio.domain.agent.tools.GetRecentTransactionsTool(transactionRepository = transactionRepository),
                dev.ajvanegasv.kontio.domain.agent.tools.GetFinancialOverviewTool(transactionRepository = transactionRepository)
            )
        )
    }

    val kontioAgentUseCase: dev.ajvanegasv.kontio.domain.agent.service.KontioAgentUseCase by lazy {
        dev.ajvanegasv.kontio.domain.agent.service.KontioAgentUseCase(
            toolRegistry = kontioToolRegistry,
            aiConfigStorage = aiConfigStorage,
            geminiApiClient = geminiApiClient
        )
    }

    val aiFinancialAdvisorUseCase: dev.ajvanegasv.kontio.domain.usecase.AiFinancialAdvisorUseCase by lazy {
        dev.ajvanegasv.kontio.domain.usecase.AiFinancialAdvisorUseCase(
            toolExecutor = financialToolExecutor,
            aiConfigStorage = aiConfigStorage,
            geminiApiClient = geminiApiClient,
            agentUseCase = kontioAgentUseCase
        )
    }

    val getFinancialSuggestionsUseCase: dev.ajvanegasv.kontio.domain.usecase.GetFinancialSuggestionsUseCase by lazy {
        dev.ajvanegasv.kontio.domain.usecase.GetFinancialSuggestionsUseCase(
            toolRegistry = kontioToolRegistry,
            aiConfigStorage = aiConfigStorage,
            geminiApiClient = geminiApiClient
        )
    }

    val parseVoiceTransactionUseCase: dev.ajvanegasv.kontio.domain.usecase.ParseVoiceTransactionUseCase by lazy {
        dev.ajvanegasv.kontio.domain.usecase.ParseVoiceTransactionUseCase(
            aiConfigStorage = aiConfigStorage,
            geminiApiClient = geminiApiClient
        )
    }

    fun initializeApp(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch {
            categoryRepository.seedDefaultCategoriesIfEmpty()
            if (accountRepository.getAccountsCount() == 0) {
                // Crear cuenta de bienvenida para que la app sea inmediatamente funcional
                createAccountUseCase(
                    Account(
                        id = "acc_cash_default",
                        name = "Efectivo",
                        type = AccountType.CASH,
                        balance = 100.0,
                        currency = "USD",
                        colorHex = "#10B981",
                        iconName = "payments"
                    )
                )
            }
        }
    }
}
