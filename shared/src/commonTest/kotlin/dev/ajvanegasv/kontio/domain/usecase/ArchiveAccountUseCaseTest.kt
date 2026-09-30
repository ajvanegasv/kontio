package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakeArchiveAccountRepository : AccountRepository {
    private val accounts = MutableStateFlow<Map<String, Account>>(emptyMap())

    override fun getAccounts(): Flow<List<Account>> = accounts.map { it.values.filterNot { it.isArchived }.toList() }
    override fun getAccountById(id: String): Flow<Account?> = accounts.map { it[id] }

    override suspend fun insertAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateAccount(account: Account) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateBalance(accountId: String, newBalance: Double) {
        val current = accounts.value[accountId] ?: return
        accounts.value = accounts.value + (accountId to current.copy(balance = newBalance))
    }

    override suspend fun archiveAccount(id: String) {
        val current = accounts.value[id] ?: return
        accounts.value = accounts.value + (id to current.copy(isArchived = true))
    }

    override suspend fun deleteAccount(id: String) {
        accounts.value = accounts.value - id
    }

    override suspend fun getAccountsCount(): Int = accounts.value.size
}

class ArchiveAccountUseCaseTest {

    @Test
    fun testArchiveAccountSuccess() = runBlocking {
        val repository = FakeArchiveAccountRepository()
        val account = Account(
            id = "acc_1",
            name = "Banco Principal",
            type = AccountType.CHECKING,
            balance = 1000.0,
            currency = "USD"
        )
        repository.insertAccount(account)

        val useCase = ArchiveAccountUseCase(repository)
        val result = useCase("acc_1")

        assertTrue(result.isSuccess)
        val stored = repository.getAccountById("acc_1").first()
        assertNotNull(stored)
        assertTrue(stored.isArchived)
        assertEquals(0, repository.getAccounts().first().size)
    }

    @Test
    fun testArchiveAccountWithEmptyIdFails() = runBlocking {
        val repository = FakeArchiveAccountRepository()
        val useCase = ArchiveAccountUseCase(repository)

        val result = useCase("   ")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testArchiveAccountUsingAccountObject() = runBlocking {
        val repository = FakeArchiveAccountRepository()
        val account = Account(
            id = "acc_obj",
            name = "Tarjeta de Crédito",
            type = AccountType.CREDIT_CARD,
            balance = 0.0,
            currency = "USD"
        )
        repository.insertAccount(account)

        val useCase = ArchiveAccountUseCase(repository)
        val result = useCase(account)

        assertTrue(result.isSuccess)
        val stored = repository.getAccountById("acc_obj").first()
        assertNotNull(stored)
        assertTrue(stored.isArchived)
    }
}
