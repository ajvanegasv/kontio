package dev.ajvanegasv.kontio.data.remote.gemini

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.StatementFile
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeGeminiApiClientWithJson(
    private val jsonResponse: String
) : GeminiApiClient() {
    override suspend fun generateContent(apiKey: String, request: GeminiRequest, model: String): Result<String> {
        return Result.success(jsonResponse)
    }
}

class GeminiStatementParserTest {

    @Test
    fun testAccountMatchingPrioritizesBankAndAccountNumberOverBankOnly() = runBlocking {
        val json = """
            {
              "detectedBank": "Bancolombia",
              "detectedAccountNumber": "4321",
              "currency": "COP",
              "transactions": []
            }
        """.trimIndent()

        val parser = GeminiStatementParser(geminiApiClient = FakeGeminiApiClientWithJson(json))
        val accounts = listOf(
            Account(id = "acc_bancolombia_gen", name = "Bancolombia Ahorros General", type = AccountType.SAVINGS, balance = 0.0, currency = "COP"),
            Account(id = "acc_bancolombia_spec", name = "Bancolombia Nómina *4321", type = AccountType.CHECKING, balance = 0.0, currency = "COP")
        )

        val file = StatementFile("extracto.csv", "text/csv", "dummy".encodeToByteArray())
        val result = parser.parseStatement(file, "test-key", emptyList(), accounts)

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Bancolombia", parsed.detectedBankName)
        assertEquals("4321", parsed.detectedAccountNumber)
        assertEquals("acc_bancolombia_spec", parsed.suggestedAccountId)
    }

    @Test
    fun testAccountMatchingMatchesByAccountNumberWhenBankDoesNotMatchNameDirectly() = runBlocking {
        val json = """
            {
              "detectedBank": "Banco Inexistente",
              "detectedAccountNumber": "7788",
              "currency": "USD",
              "transactions": []
            }
        """.trimIndent()

        val parser = GeminiStatementParser(geminiApiClient = FakeGeminiApiClientWithJson(json))
        val accounts = listOf(
            Account(id = "acc_1", name = "Efectivo", type = AccountType.CASH, balance = 0.0, currency = "USD"),
            Account(id = "acc_2", name = "Tarjeta Visa 7788", type = AccountType.CREDIT_CARD, balance = 0.0, currency = "USD")
        )

        val file = StatementFile("extracto.csv", "text/csv", "dummy".encodeToByteArray())
        val result = parser.parseStatement(file, "test-key", emptyList(), accounts)

        assertTrue(result.isSuccess)
        assertEquals("acc_2", result.getOrThrow().suggestedAccountId)
    }

    @Test
    fun testAccountMatchingFallsBackToBankNameWhenNoAccountNumber() = runBlocking {
        val json = """
            {
              "detectedBank": "Nu Bank",
              "detectedAccountNumber": null,
              "currency": "COP",
              "transactions": []
            }
        """.trimIndent()

        val parser = GeminiStatementParser(geminiApiClient = FakeGeminiApiClientWithJson(json))
        val accounts = listOf(
            Account(id = "acc_davivienda", name = "Davivienda", type = AccountType.SAVINGS, balance = 0.0, currency = "COP"),
            Account(id = "acc_nu", name = "Cuenta Nu Bank Ahorros", type = AccountType.SAVINGS, balance = 0.0, currency = "COP")
        )

        val file = StatementFile("extracto.csv", "text/csv", "dummy".encodeToByteArray())
        val result = parser.parseStatement(file, "test-key", emptyList(), accounts)

        assertTrue(result.isSuccess)
        assertEquals("acc_nu", result.getOrThrow().suggestedAccountId)
    }
}
