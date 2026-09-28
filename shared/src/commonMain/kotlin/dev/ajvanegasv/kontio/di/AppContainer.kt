package dev.ajvanegasv.kontio.di

import dev.ajvanegasv.kontio.data.backup.GoogleDriveBackupManager
import dev.ajvanegasv.kontio.data.local.KontioDatabase
import dev.ajvanegasv.kontio.data.local.createRoomDatabase
import dev.ajvanegasv.kontio.data.repository.AccountRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.BackupRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.CategoryRepositoryImpl
import dev.ajvanegasv.kontio.data.repository.TransactionRepositoryImpl
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.BackupRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateCategoryUseCase
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteCategoryUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetDashboardSummaryUseCase
import dev.ajvanegasv.kontio.domain.usecase.ReassignTransactionsAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.UpdateTransactionUseCase
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

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            driveManager = GoogleDriveBackupManager()
        )
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
