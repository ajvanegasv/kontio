package dev.ajvanegasv.kontio.domain.repository

import dev.ajvanegasv.kontio.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getAccountById(id: String): Flow<Account?>
    suspend fun insertAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun updateBalance(accountId: String, newBalance: Double)
    suspend fun deleteAccount(id: String)
    suspend fun getAccountsCount(): Int
}
