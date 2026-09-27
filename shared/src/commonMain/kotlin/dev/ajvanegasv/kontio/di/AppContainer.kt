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
import dev.ajvanegasv.kontio.domain.usecase.CreateTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteAccountUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteTransactionUseCase
import dev.ajvanegasv.kontio.domain.usecase.GetDashboardSummaryUseCase
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

    val getDashboardSummaryUseCase: GetDashboardSummaryUseCase by lazy {
        GetDashboardSummaryUseCase(
            accountRepository = accountRepository,
            transactionRepository = transactionRepository
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
