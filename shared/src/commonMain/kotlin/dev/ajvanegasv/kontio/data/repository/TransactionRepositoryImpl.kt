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

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions.map { TransactionEntity.fromDomain(it) })
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

    override suspend fun getTransactionsCountByCategory(categoryId: String): Int {
        return transactionDao.getTransactionsCountByCategoryId(categoryId)
    }

    override suspend fun searchTransactions(query: String): List<Transaction> {
        val trimmed = query.trim().lowercase()
        val allCategories = categoryDao.getAllCategoriesDirect().map { it.toDomain() }
        val allAccounts = accountDao.getAllAccountsDirect().map { it.toDomain() }
        val catMap = allCategories.associateBy { it.id }
        val accMap = allAccounts.associateBy { it.id }

        // Si la búsqueda coincide con un nombre de categoría, traemos transacciones de esa categoría
        val matchingCatIds = allCategories
            .filter { it.name.lowercase().contains(trimmed) }
            .map { it.id }
            .toSet()

        val byNote = transactionDao.searchTransactionsByNote(trimmed)
        val allMatching = if (matchingCatIds.isNotEmpty()) {
            transactionDao.getAllTransactionsDirect().filter { it.categoryId in matchingCatIds }
        } else {
            emptyList()
        }

        val combined = (byNote + allMatching).distinctBy { it.id }.sortedByDescending { it.timestamp }
        return combined.map { entity ->
            val domain = entity.toDomain()
            domain.copy(
                category = catMap[domain.categoryId],
                account = accMap[domain.accountId]
            )
        }
    }

    override suspend fun getTransactionsInDateRangeDirect(startDate: Long, endDate: Long): List<Transaction> {
        val allCategories = categoryDao.getAllCategoriesDirect().associate { it.id to it.toDomain() }
        val allAccounts = accountDao.getAllAccountsDirect().associate { it.id to it.toDomain() }
        val entities = transactionDao.getTransactionsInDateRangeDirect(startDate, endDate)
        return entities.map { entity ->
            val domain = entity.toDomain()
            domain.copy(
                category = allCategories[domain.categoryId],
                account = allAccounts[domain.accountId]
            )
        }
    }
}
