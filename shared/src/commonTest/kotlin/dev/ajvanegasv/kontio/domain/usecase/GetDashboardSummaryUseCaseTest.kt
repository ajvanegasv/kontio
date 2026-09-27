package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDashboardSummaryUseCaseTest {

    @Test
    fun testTotalBalanceExcludesCreditCardDebtAndArchivedAccounts() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetDashboardSummaryUseCase(accountRepo, txRepo)

        // Cuenta de ahorros con 1,000,000
        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 1_000_000.0,
                currency = "COP"
            )
        )

        // Cuenta de efectivo con 200,000
        accountRepo.insertAccount(
            Account(
                id = "acc_cash",
                name = "Efectivo",
                type = AccountType.CASH,
                balance = 200_000.0,
                currency = "COP"
            )
        )

        // Tarjeta de crédito con deuda de 500,000
        accountRepo.insertAccount(
            Account(
                id = "acc_card",
                name = "Tarjeta de Crédito",
                type = AccountType.CREDIT_CARD,
                balance = 500_000.0,
                creditLimit = 2_000_000.0,
                currency = "COP"
            )
        )

        // Cuenta archivada con saldo de 50,000 (no debe sumar al balance activo)
        accountRepo.insertAccount(
            Account(
                id = "acc_archived",
                name = "Antigua Cuenta",
                type = AccountType.SAVINGS,
                balance = 50_000.0,
                isArchived = true,
                currency = "COP"
            )
        )

        val summary = useCase().first()

        // El Total Balance debe ser únicamente la suma de fondos disponibles reales (1,000,000 + 200,000 = 1,200,000)
        // No debe descontar la deuda de la tarjeta de crédito (500,000)
        assertEquals(1_200_000.0, summary.totalBalance)
        assertEquals(4, summary.accounts.size)
        assertEquals("COP", summary.currency)
    }

    @Test
    fun testSummaryCalculatesIncomeAndExpenses() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()
        val useCase = GetDashboardSummaryUseCase(accountRepo, txRepo)

        accountRepo.insertAccount(
            Account(
                id = "acc_savings",
                name = "Ahorros",
                type = AccountType.SAVINGS,
                balance = 500.0
            )
        )

        txRepo.insertTransaction(
            Transaction(
                id = "tx_inc",
                accountId = "acc_savings",
                categoryId = "cat_salary",
                type = TransactionType.INCOME,
                amount = 1500.0,
                timestamp = 1000L
            )
        )
        txRepo.insertTransaction(
            Transaction(
                id = "tx_exp",
                accountId = "acc_savings",
                categoryId = "cat_groceries",
                type = TransactionType.EXPENSE,
                amount = 300.0,
                timestamp = 2000L
            )
        )
        txRepo.insertTransaction(
            Transaction(
                id = "tx_transfer",
                accountId = "acc_savings",
                targetAccountId = "acc_card",
                categoryId = "cat_transfer",
                type = TransactionType.TRANSFER,
                amount = 200.0,
                timestamp = 3000L
            )
        )

        val summary = useCase().first()

        assertEquals(500.0, summary.totalBalance)
        assertEquals(1500.0, summary.monthlyIncome)
        assertEquals(300.0, summary.monthlyExpenses)
        assertEquals(3, summary.recentTransactions.size)
    }
}
