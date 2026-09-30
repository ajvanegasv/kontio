package dev.ajvanegasv.kontio.data.repository

import dev.ajvanegasv.kontio.data.backup.GoogleDriveBackupManager
import dev.ajvanegasv.kontio.data.backup.KontioBackupPayload
import dev.ajvanegasv.kontio.data.local.dao.AccountDao
import dev.ajvanegasv.kontio.data.local.dao.BudgetDao
import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.dao.TransactionDao
import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import dev.ajvanegasv.kontio.domain.repository.BackupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BackupRepositoryImpl(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val driveManager: GoogleDriveBackupManager,
    private val budgetDao: BudgetDao? = null
) : BackupRepository {

    private val _latestLocalMetadata = MutableStateFlow<BackupMetadata?>(null)
    override fun getLatestLocalBackupMetadata(): Flow<BackupMetadata?> = _latestLocalMetadata.asStateFlow()

    override fun getConnectedGoogleAccount(): Flow<String?> = driveManager.connectedAccount

    override fun setConnectedGoogleAccount(email: String) {
        driveManager.setConnectedAccount(email)
    }

    override suspend fun connectGoogleAccount(): Result<String> {
        return driveManager.connectAccount()
    }

    override suspend fun disconnectGoogleAccount(): Result<Unit> {
        return driveManager.disconnectAccount()
    }

    override suspend fun createLocalBackup(): Result<ByteArray> {
        return runCatching {
            val accounts = accountDao.getAllAccountsDirect()
            val categories = categoryDao.getAllCategoriesDirect()
            val transactions = transactionDao.getAllTransactionsDirect()
            val budgets = budgetDao?.getAllBudgetsDirect() ?: emptyList()

            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val dummyMetadata = BackupMetadata(
                backupId = "backup_$now",
                timestamp = now,
                sizeBytes = 0L,
                accountCount = accounts.size,
                transactionCount = transactions.size,
                categoryCount = categories.size
            )

            val payload = KontioBackupPayload(
                metadata = dummyMetadata,
                accounts = accounts,
                categories = categories,
                transactions = transactions,
                budgets = budgets
            )
            val bytes = payload.toBytes()
            val finalMetadata = dummyMetadata.copy(sizeBytes = bytes.size.toLong())
            _latestLocalMetadata.value = finalMetadata
            bytes
        }
    }

    override suspend fun restoreFromLocalBackup(backupBytes: ByteArray): Result<Unit> {
        return runCatching {
            val payload = KontioBackupPayload.fromBytes(backupBytes)

            // Reemplazo atómico / secuencial de datos
            transactionDao.deleteAllTransactions()
            accountDao.deleteAllAccounts()
            categoryDao.deleteAllCategories()
            budgetDao?.deleteAllBudgets()

            categoryDao.insertCategories(payload.categories)
            accountDao.insertAccounts(payload.accounts)
            transactionDao.insertTransactions(payload.transactions)
            if (payload.budgets.isNotEmpty()) {
                budgetDao?.insertBudgets(payload.budgets)
            }

            _latestLocalMetadata.value = payload.metadata
        }
    }

    override suspend fun backupToGoogleDrive(): Result<BackupMetadata> {
        return runCatching {
            val accounts = accountDao.getAllAccountsDirect()
            val categories = categoryDao.getAllCategoriesDirect()
            val transactions = transactionDao.getAllTransactionsDirect()
            val budgets = budgetDao?.getAllBudgetsDirect() ?: emptyList()

            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val initialMetadata = BackupMetadata(
                backupId = "drive_backup_$now",
                timestamp = now,
                sizeBytes = 0L,
                accountCount = accounts.size,
                transactionCount = transactions.size,
                categoryCount = categories.size
            )

            val payload = KontioBackupPayload(
                metadata = initialMetadata,
                accounts = accounts,
                categories = categories,
                transactions = transactions,
                budgets = budgets
            )
            val bytes = payload.toBytes()
            val finalMetadata = initialMetadata.copy(sizeBytes = bytes.size.toLong())

            val uploadedMetadata = driveManager.uploadBackup(bytes, finalMetadata).getOrThrow()
            _latestLocalMetadata.value = uploadedMetadata
            uploadedMetadata
        }
    }

    override suspend fun restoreFromGoogleDrive(): Result<Unit> {
        return runCatching {
            val bytes = driveManager.downloadLatestBackup().getOrThrow()
            restoreFromLocalBackup(bytes).getOrThrow()
        }
    }

    override suspend fun getDriveBackupMetadata(): Result<BackupMetadata?> {
        return driveManager.getLatestBackupMetadata().onSuccess { meta ->
            if (meta != null) {
                _latestLocalMetadata.value = meta
            }
        }
    }
}
