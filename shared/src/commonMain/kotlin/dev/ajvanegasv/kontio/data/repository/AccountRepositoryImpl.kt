package dev.ajvanegasv.kontio.data.repository

import dev.ajvanegasv.kontio.data.local.dao.AccountDao
import dev.ajvanegasv.kontio.data.local.entity.AccountEntity
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAccounts(): Flow<List<Account>> {
        return accountDao.getActiveAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAccountById(id: String): Flow<Account?> {
        return accountDao.getAccountById(id).map { it?.toDomain() }
    }

    override suspend fun insertAccount(account: Account) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val toInsert = if (account.createdAt == 0L) {
            account.copy(createdAt = now, updatedAt = now)
        } else {
            account.copy(updatedAt = now)
        }
        accountDao.insertAccount(AccountEntity.fromDomain(toInsert))
    }

    override suspend fun updateAccount(account: Account) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        accountDao.updateAccount(AccountEntity.fromDomain(account.copy(updatedAt = now)))
    }

    override suspend fun updateBalance(accountId: String, newBalance: Double) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        accountDao.updateBalance(accountId, newBalance, now)
    }

    override suspend fun deleteAccount(id: String) {
        accountDao.deleteAccount(id)
    }

    override suspend fun getAccountsCount(): Int {
        return accountDao.getAccountsCount()
    }
}
