package dev.ajvanegasv.kontio.data.repository

import dev.ajvanegasv.kontio.data.local.dao.AccountDao
import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.dao.TransactionDao
import dev.ajvanegasv.kontio.data.local.entity.TransactionEntity
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao
) : TransactionRepository {

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return combine(
            transactionDao.getRecentTransactions(limit),
            categoryDao.getAllCategories(),
            accountDao.getActiveAccounts()
        ) { txEntities, catEntities, accEntities ->
            val catMap = catEntities.associate { it.id to it.toDomain() }
            val accMap = accEntities.associate { it.id to it.toDomain() }

            txEntities.map { entity ->
                val domain = entity.toDomain()
                domain.copy(
                    category = catMap[domain.categoryId],
                    account = accMap[domain.accountId]
                )
            }
        }
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return combine(
            transactionDao.getAllTransactions(),
            categoryDao.getAllCategories(),
            accountDao.getActiveAccounts()
        ) { txEntities, catEntities, accEntities ->
            val catMap = catEntities.associate { it.id to it.toDomain() }
            val accMap = accEntities.associate { it.id to it.toDomain() }

            txEntities.map { entity ->
                val domain = entity.toDomain()
                domain.copy(
                    category = catMap[domain.categoryId],
                    account = accMap[domain.accountId]
                )
            }
        }
    }

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsByAccount(accountId),
            categoryDao.getAllCategories(),
            accountDao.getActiveAccounts()
        ) { txEntities, catEntities, accEntities ->
            val catMap = catEntities.associate { it.id to it.toDomain() }
            val accMap = accEntities.associate { it.id to it.toDomain() }

            txEntities.map { entity ->
                val domain = entity.toDomain()
                domain.copy(
                    category = catMap[domain.categoryId],
                    account = accMap[domain.accountId]
                )
            }
        }
    }

    override fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsInDateRange(startDate, endDate),
            categoryDao.getAllCategories(),
            accountDao.getActiveAccounts()
        ) { txEntities, catEntities, accEntities ->
            val catMap = catEntities.associate { it.id to it.toDomain() }
            val accMap = accEntities.associate { it.id to it.toDomain() }

            txEntities.map { entity ->
                val domain = entity.toDomain()
                domain.copy(
                    category = catMap[domain.categoryId],
                    account = accMap[domain.accountId]
                )
            }
        }
    }

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
    }

    override suspend fun deleteTransaction(id: String) {
        transactionDao.deleteTransaction(id)
    }

    override suspend fun getTransactionById(id: String): Transaction? {
        val entity = transactionDao.getTransactionByIdDirect(id) ?: return null
        val domain = entity.toDomain()
        val category = categoryDao.getCategoryByIdDirect(domain.categoryId)?.toDomain()
            ?: categoryDao.getAllCategoriesDirect().firstOrNull { it.id == domain.categoryId }?.toDomain()
        val account = accountDao.getAccountByIdDirect(domain.accountId)?.toDomain()
        return domain.copy(
            category = category,
            account = account
        )
    }

    override suspend fun deleteTransactionsByAccountId(accountId: String) {
        transactionDao.deleteTransactionsByAccountId(accountId)
    }

    override suspend fun getTransactionsCount(): Int {
        return transactionDao.getTransactionsCount()
    }
}
