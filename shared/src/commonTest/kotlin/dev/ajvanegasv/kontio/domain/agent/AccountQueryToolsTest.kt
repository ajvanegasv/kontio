package dev.ajvanegasv.kontio.domain.agent

import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.agent.tools.GetAccountDetailTool
import dev.ajvanegasv.kontio.domain.agent.tools.GetAccountsSummaryTool
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.usecase.FakeAccountRepository
import dev.ajvanegasv.kontio.domain.usecase.FakeTransactionRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccountQueryToolsTest {

    @Test
    fun getAccountsSummary_calculatesNetWorthAndFormatsAccounts() = runBlocking {
        val accountRepo = FakeAccountRepository()
        accountRepo.insertAccount(
            Account(id = "acc1", name = "Bancolombia Ahorros", type = AccountType.SAVINGS, balance = 1500.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc2", name = "Efectivo", type = AccountType.CASH, balance = 200.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc3", name = "Tarjeta Visa", type = AccountType.CREDIT_CARD, balance = 300.0, creditLimit = 1000.0, currency = "USD")
        )

        val tool = GetAccountsSummaryTool(accountRepo)
        val result = tool.execute(emptyMap())

        assertTrue(result.success)
        assertEquals("get_accounts_summary", result.toolName)
        assertTrue(result.naturalLanguageSummary.contains("Bancolombia Ahorros"))
        assertTrue(result.naturalLanguageSummary.contains("Efectivo"))
        assertTrue(result.naturalLanguageSummary.contains("Tarjeta Visa"))

        val payload = result.visualPayload as? AgentVisualPayload.AccountsSummaryPayload
        assertNotNull(payload)
        assertEquals(3, payload.accounts.size)
        // 1500 (ahorros) + 200 (efectivo) - 300 (tarjeta de crédito deuda) = 1400
        assertEquals(1400.0, payload.totalNetWorthByCurrency["USD"])
    }

    @Test
    fun getAccountsSummary_withFilter_returnsOnlyFilteredType() = runBlocking {
        val accountRepo = FakeAccountRepository()
        accountRepo.insertAccount(
            Account(id = "acc1", name = "Bancolombia Ahorros", type = AccountType.SAVINGS, balance = 1500.0, currency = "USD")
        )
        accountRepo.insertAccount(
            Account(id = "acc2", name = "Tarjeta Visa", type = AccountType.CREDIT_CARD, balance = 300.0, creditLimit = 1000.0, currency = "USD")
        )

        val tool = GetAccountsSummaryTool(accountRepo)
        val result = tool.execute(mapOf("filter_type" to JsonPrimitive("CREDIT_CARD")))

        assertTrue(result.success)
        val payload = result.visualPayload as? AgentVisualPayload.AccountsSummaryPayload
        assertNotNull(payload)
        assertEquals(1, payload.accounts.size)
        assertEquals("Tarjeta Visa", payload.accounts.first().name)
    }

    @Test
    fun getAccountDetail_returnsAccountAndRecentTransactions() = runBlocking {
        val accountRepo = FakeAccountRepository()
        val txRepo = FakeTransactionRepository()

        val acc = Account(id = "acc_banco", name = "Banco de Bogotá", type = AccountType.SAVINGS, balance = 500.0, currency = "USD")
        accountRepo.insertAccount(acc)

        txRepo.insertTransaction(
            Transaction(id = "tx1", accountId = "acc_banco", categoryId = "cat1", type = TransactionType.EXPENSE, amount = 45.0, timestamp = 1000L, note = "Cena")
        )

        val tool = GetAccountDetailTool(accountRepo, txRepo)
        val result = tool.execute(mapOf("account_query" to JsonPrimitive("Bogotá")))

        assertTrue(result.success)
        assertTrue(result.naturalLanguageSummary.contains("Banco de Bogotá"))
        assertTrue(result.naturalLanguageSummary.contains("Cena"))
    }
}
