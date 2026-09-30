package dev.ajvanegasv.kontio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ajvanegasv.kontio.data.local.entity.BudgetTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetTransactionDao {

    @Query("SELECT * FROM budget_transactions ORDER BY createdAt DESC")
    fun getAllBudgetTransactions(): Flow<List<BudgetTransactionEntity>>

    @Query("SELECT * FROM budget_transactions ORDER BY createdAt DESC")
    suspend fun getAllBudgetTransactionsDirect(): List<BudgetTransactionEntity>

    @Query("SELECT * FROM budget_transactions WHERE budgetId = :budgetId ORDER BY createdAt DESC")
    fun getTransactionsForBudget(budgetId: String): Flow<List<BudgetTransactionEntity>>

    @Query("SELECT * FROM budget_transactions WHERE budgetId = :budgetId ORDER BY createdAt DESC")
    suspend fun getTransactionsForBudgetDirect(budgetId: String): List<BudgetTransactionEntity>

    @Query("SELECT * FROM budget_transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getByTransactionId(transactionId: String): BudgetTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetTransaction(entity: BudgetTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetTransactions(entities: List<BudgetTransactionEntity>)

    @Query("DELETE FROM budget_transactions WHERE id = :id")
    suspend fun deleteBudgetTransaction(id: String)

    @Query("DELETE FROM budget_transactions WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: String)

    @Query("DELETE FROM budget_transactions WHERE budgetId = :budgetId")
    suspend fun deleteByBudgetId(budgetId: String)

    @Query("DELETE FROM budget_transactions WHERE budgetId = :budgetId AND transactionId = :transactionId")
    suspend fun deleteLink(budgetId: String, transactionId: String)

    @Query("DELETE FROM budget_transactions")
    suspend fun deleteAllBudgetTransactions()

    @Query("SELECT COUNT(*) FROM budget_transactions")
    suspend fun getBudgetTransactionsCount(): Int
}
