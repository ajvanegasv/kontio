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

class GetCreditCardWidgetDataUseCaseTest {

    private val fixedNow = Instant.parse("2026-09-15T12:00:00Z")
    private val fakeClock = object : Clock {
        override fun now(): Instant = fixedNow
    }

    @Test
    fun testCreditCardWidgetCalculations() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetCreditCardWidgetDataUseCase(accountRepo, txRepo)

        accountRepo.insertAccount(
            Account(
                id = "card1",
                name = "Visa Black",
                type = AccountType.CREDIT_CARD,
                balance = 500.0,
                creditLimit = 2000.0,
                currency = "USD"
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "card2",
                name = "Mastercard Gold",
                type = AccountType.CREDIT_CARD,
                balance = 300.0,
                creditLimit = 1000.0,
                currency = "USD"
            )
        )
        // Ahorros (no debe sumarse)
        accountRepo.insertAccount(
            Account(
                id = "acc_sav",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 10000.0,
                currency = "USD"
            )
        )
        // Tarjeta archivada (no debe sumarse)
        accountRepo.insertAccount(
            Account(
                id = "card_archived",
                name = "Tarjeta Vieja",
                type = AccountType.CREDIT_CARD,
                balance = 200.0,
                creditLimit = 1000.0,
                isArchived = true,
                currency = "USD"
            )
        )

        val summary = useCase(clock = fakeClock).first()

        assertEquals(800.0, summary.totalDebt)
        assertEquals(3000.0, summary.totalCreditLimit)
        assertEquals(2200.0, summary.availableCredit)
        assertEquals(2, summary.cardsCount)
        assertEquals("USD", summary.currency)
    }

    @Test
    fun testCalculatesMonthlyExpensesForCreditCards() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetCreditCardWidgetDataUseCase(accountRepo, txRepo)

        accountRepo.insertAccount(
            Account(
                id = "card1",
                name = "Visa Black",
                type = AccountType.CREDIT_CARD,
                balance = 650.0,
                creditLimit = 3000.0,
                currency = "USD"
            )
        )
        accountRepo.insertAccount(
            Account(
                id = "acc_sav",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 1000.0,
                currency = "USD"
            )
        )

        val timeZone = TimeZone.currentSystemDefault()
        val thisMonthTs = Instant.parse("2026-09-08T15:00:00Z").toEpochMilliseconds()
        val lastMonthTs = fixedNow.minus(40, DateTimeUnit.DAY, timeZone).toEpochMilliseconds()

        // Gasto con tarjeta este mes: 450
        txRepo.insertTransaction(
            Transaction(
                id = "tx_card_1",
                accountId = "card1",
                categoryId = "c_shop",
                type = TransactionType.EXPENSE,
                amount = 450.0,
                timestamp = thisMonthTs
            )
        )
        // Gasto con tarjeta mes pasado (debe ignorarse): 200
        txRepo.insertTransaction(
            Transaction(
                id = "tx_card_old",
                accountId = "card1",
                categoryId = "c_shop",
                type = TransactionType.EXPENSE,
                amount = 200.0,
                timestamp = lastMonthTs
            )
        )
        // Gasto con ahorros este mes (debe ignorarse en widget de tarjetas): 150
        txRepo.insertTransaction(
            Transaction(
                id = "tx_sav_1",
                accountId = "acc_sav",
                categoryId = "c_food",
                type = TransactionType.EXPENSE,
                amount = 150.0,
                timestamp = thisMonthTs
            )
        )

        val summary = useCase(clock = fakeClock).first()

        assertEquals(650.0, summary.totalDebt)
        assertEquals(450.0, summary.monthlyExpenses)
    }
}
