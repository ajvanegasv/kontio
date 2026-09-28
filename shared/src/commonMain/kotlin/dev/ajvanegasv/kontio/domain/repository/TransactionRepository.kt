package dev.ajvanegasv.kontio.domain.repository

import dev.ajvanegasv.kontio.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getRecentTransactions(limit: Int = 20): Flow<List<Transaction>>
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun deleteTransaction(id: String)
    suspend fun getTransactionById(id: String): Transaction?
    suspend fun deleteTransactionsByAccountId(accountId: String)
    suspend fun getTransactionsCount(): Int
    suspend fun getTransactionsCountByCategory(categoryId: String): Int
    suspend fun searchTransactions(query: String): List<Transaction> = emptyList()
    suspend fun getTransactionsInDateRangeDirect(startDate: Long, endDate: Long): List<Transaction> = emptyList()
}
