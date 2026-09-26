package dev.ajvanegasv.kontio.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.ajvanegasv.kontio.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun getAccountById(id: String): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountByIdDirect(id: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccountsDirect(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET balance = :newBalance, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: String, newBalance: Double, updatedAt: Long)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountsCount(): Int
}
