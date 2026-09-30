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

private class FakeUpdateAccountRepository : AccountRepository {
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

class UpdateAccountUseCaseTest {

    @Test
    fun testUpdateAccountSuccess() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val original = Account(
            id = "acc_1",
            name = "Ahorros Inicial",
            type = AccountType.SAVINGS,
            balance = 500.0,
            currency = "USD",
            colorHex = "#3B82F6"
        )
        repository.insertAccount(original)

        val useCase = UpdateAccountUseCase(repository)
        val updated = original.copy(
            name = "Ahorros Modificado",
            balance = 850.0,
            colorHex = "#10B981"
        )

        val result = useCase(updated)
        assertTrue(result.isSuccess)

        val stored = repository.getAccountById("acc_1").first()
        assertNotNull(stored)
        assertEquals("Ahorros Modificado", stored.name)
        assertEquals(850.0, stored.balance)
        assertEquals("#10B981", stored.colorHex)
    }

    @Test
    fun testUpdateAccountWithBlankIdFails() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val useCase = UpdateAccountUseCase(repository)

        val account = Account(
            id = "   ",
            name = "Ahorros",
            type = AccountType.SAVINGS,
            balance = 100.0
        )

        val result = useCase(account)
        assertTrue(result.isFailure)
        assertEquals("El ID de la cuenta no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateAccountWithBlankNameFails() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val useCase = UpdateAccountUseCase(repository)

        val account = Account(
            id = "acc_1",
            name = "   ",
            type = AccountType.SAVINGS,
            balance = 100.0
        )

        val result = useCase(account)
        assertTrue(result.isFailure)
        assertEquals("El nombre de la cuenta no puede estar vacío", result.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateCreditCardWithoutLimitFails() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val useCase = UpdateAccountUseCase(repository)

        val accountNoLimit = Account(
            id = "card_1",
            name = "Visa Black",
            type = AccountType.CREDIT_CARD,
            balance = 0.0,
            creditLimit = null
        )

        val resultNull = useCase(accountNoLimit)
        assertTrue(resultNull.isFailure)
        assertEquals("Las tarjetas de crédito deben tener un cupo límite asignado mayor a cero", resultNull.exceptionOrNull()?.message)

        val accountZeroLimit = accountNoLimit.copy(creditLimit = 0.0)
        val resultZero = useCase(accountZeroLimit)
        assertTrue(resultZero.isFailure)
        assertEquals("Las tarjetas de crédito deben tener un cupo límite asignado mayor a cero", resultZero.exceptionOrNull()?.message)

        val accountNegativeLimit = accountNoLimit.copy(creditLimit = -100.0)
        val resultNegative = useCase(accountNegativeLimit)
        assertTrue(resultNegative.isFailure)
        assertEquals("Las tarjetas de crédito deben tener un cupo límite asignado mayor a cero", resultNegative.exceptionOrNull()?.message)
    }

    @Test
    fun testUpdateCreditCardWithValidLimitSucceeds() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val useCase = UpdateAccountUseCase(repository)

        val validCard = Account(
            id = "card_1",
            name = "Visa Platinum",
            type = AccountType.CREDIT_CARD,
            balance = 100.0,
            creditLimit = 3500.0
        )

        val result = useCase(validCard)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testUpdateAccountCutoffDayBounds() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val useCase = UpdateAccountUseCase(repository)

        val baseAccount = Account(
            id = "card_1",
            name = "Tarjeta Oro",
            type = AccountType.CREDIT_CARD,
            balance = 0.0,
            creditLimit = 1000.0
        )

        // Limite inferior fuera de rango (0)
        val result0 = useCase(baseAccount.copy(cutoffDay = 0))
        assertTrue(result0.isFailure)
        assertEquals("El día de corte debe estar entre 1 y 31", result0.exceptionOrNull()?.message)

        // Limite superior fuera de rango (32)
        val result32 = useCase(baseAccount.copy(cutoffDay = 32))
        assertTrue(result32.isFailure)
        assertEquals("El día de corte debe estar entre 1 y 31", result32.exceptionOrNull()?.message)

        // Casos válidos: 1, 31, y null
        val result1 = useCase(baseAccount.copy(cutoffDay = 1))
        assertTrue(result1.isSuccess)

        val result31 = useCase(baseAccount.copy(cutoffDay = 31))
        assertTrue(result31.isSuccess)

        val resultNull = useCase(baseAccount.copy(cutoffDay = null))
        assertTrue(resultNull.isSuccess)
    }

    @Test
    fun testUpdateAccountDueDayBounds() = runBlocking {
        val repository = FakeUpdateAccountRepository()
        val useCase = UpdateAccountUseCase(repository)

        val baseAccount = Account(
            id = "card_1",
            name = "Tarjeta Oro",
            type = AccountType.CREDIT_CARD,
            balance = 0.0,
            creditLimit = 1000.0
        )

        // Limite inferior fuera de rango (0)
        val result0 = useCase(baseAccount.copy(dueDay = 0))
        assertTrue(result0.isFailure)
        assertEquals("El día límite de pago debe estar entre 1 y 31", result0.exceptionOrNull()?.message)

        // Limite superior fuera de rango (32)
        val result32 = useCase(baseAccount.copy(dueDay = 32))
        assertTrue(result32.isFailure)
        assertEquals("El día límite de pago debe estar entre 1 y 31", result32.exceptionOrNull()?.message)

        // Casos válidos: 1, 31, y null
        val result1 = useCase(baseAccount.copy(dueDay = 1))
        assertTrue(result1.isSuccess)

        val result31 = useCase(baseAccount.copy(dueDay = 31))
        assertTrue(result31.isSuccess)

        val resultNull = useCase(baseAccount.copy(dueDay = null))
        assertTrue(resultNull.isSuccess)
    }
}
