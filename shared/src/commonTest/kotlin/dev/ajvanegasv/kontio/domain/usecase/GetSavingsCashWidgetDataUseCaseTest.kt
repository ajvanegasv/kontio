package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals

class GetSavingsCashWidgetDataUseCaseTest {

    private val fixedNow = Instant.parse("2026-09-15T12:00:00Z")
    private val fakeClock = object : Clock {
        override fun now(): Instant = fixedNow
    }

    @Test
    fun testFiltersOnlySavingsAndCashAccountsAndExcludesArchived() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetSavingsCashWidgetDataUseCase(accountRepo, txRepo)

        accountRepo.insertAccount(
            Account(id = "acc_sav", name = "Ahorros", type = AccountType.SAVINGS, balance = 1000.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc_cash", name = "Efectivo", type = AccountType.CASH, balance = 500.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc_chk", name = "Corriente", type = AccountType.CHECKING, balance = 300.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc_cc", name = "Tarjeta", type = AccountType.CREDIT_CARD, balance = 200.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc_archived", name = "Ahorros Viejo", type = AccountType.SAVINGS, balance = 5000.0, isArchived = true, currency = "USD")
        )

        val summary = useCase(clock = fakeClock).first()

        assertEquals(1500.0, summary.totalBalance)
        assertEquals(2, summary.accountsCount)
        assertEquals("USD", summary.currency)
    }

    @Test
    fun testCalculatesMonthlyIncomeExpensesAndNetFlow() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetSavingsCashWidgetDataUseCase(accountRepo, txRepo)

        accountRepo.insertAccount(
            Account(id = "acc_sav", name = "Ahorros", type = AccountType.SAVINGS, balance = 2000.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc_chk", name = "Corriente", type = AccountType.CHECKING, balance = 1000.0, currency = "USD")
        )

        val timeZone = TimeZone.currentSystemDefault()
        // Transacción de este mes en cuenta de ahorros (septiembre 2026)
        val thisMonthTs = Instant.parse("2026-09-05T10:00:00Z").toEpochMilliseconds()
        // Transacción de mes pasado (agosto 2026)
        val lastMonthTs = fixedNow.minus(45, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()

        // Ingreso en ahorros este mes: +1200
        txRepo.insertTransaction(
            Transaction(
                id = "tx1",
                accountId = "acc_sav",
                categoryId = "c1",
                type = TransactionType.INCOME,
                amount = 1200.0,
                timestamp = thisMonthTs
            )
        )
        // Gasto en ahorros este mes: -400
        txRepo.insertTransaction(
            Transaction(
                id = "tx2",
                accountId = "acc_sav",
                categoryId = "c2",
                type = TransactionType.EXPENSE,
                amount = 400.0,
                timestamp = thisMonthTs
            )
        )
        // Gasto en ahorros mes pasado (debe ser ignorado): -900
        txRepo.insertTransaction(
            Transaction(
                id = "tx3",
                accountId = "acc_sav",
                categoryId = "c2",
                type = TransactionType.EXPENSE,
                amount = 900.0,
                timestamp = lastMonthTs
            )
        )
        // Gasto en cuenta corriente este mes (debe ser ignorado porque no es SAVINGS ni CASH): -300
        txRepo.insertTransaction(
            Transaction(
                id = "tx4",
                accountId = "acc_chk",
                categoryId = "c2",
                type = TransactionType.EXPENSE,
                amount = 300.0,
                timestamp = thisMonthTs
            )
        )

        val summary = useCase(clock = fakeClock).first()

        assertEquals(2000.0, summary.totalBalance)
        assertEquals(1200.0, summary.monthlyIncome)
        assertEquals(400.0, summary.monthlyExpenses)
        assertEquals(800.0, summary.netMonthlyFlow)
    }
}
