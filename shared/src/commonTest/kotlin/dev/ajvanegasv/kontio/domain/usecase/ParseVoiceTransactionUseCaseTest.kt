package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiRequest
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ParseVoiceTransactionUseCaseTest {

    private class FakeAiConfigStorage(
        private var key: String? = null,
        private var currentModel: String = "gemini-3.8-flash",
        private var enabled: Boolean = true
    ) : AiConfigStorage {
        override val apiKeyFlow: Flow<String?> = flowOf(key)
        override fun getApiKey(): String? = key
        override fun setApiKey(apiKey: String) {
            key = apiKey
            enabled = true
        }
        override fun clearApiKey() {
            key = null
            enabled = false
        }
        override val modelFlow: Flow<String> = flowOf(currentModel)
        override fun getModel(): String = currentModel
        override fun setModel(model: String) { currentModel = model }
        override val isAiEnabledFlow: Flow<Boolean> = flowOf(enabled)
        override fun isAiEnabled(): Boolean = enabled
        override fun setAiEnabled(enabled: Boolean) { this.enabled = enabled }
    }

    private class FakeGeminiApiClient(
        private val cannedResponse: Result<String>
    ) : GeminiApiClient() {
        override suspend fun generateContent(
            apiKey: String,
            request: GeminiRequest,
            model: String
        ): Result<String> = cannedResponse
    }

    private val testAccounts = listOf(
        Account(
            id = "acc_bancolombia",
            name = "Bancolombia",
            type = AccountType.CHECKING,
            balance = 500000.0,
            currency = "COP"
        ),
        Account(
            id = "acc_cash",
            name = "Efectivo",
            type = AccountType.CASH,
            balance = 100000.0,
            currency = "COP"
        )
    )

    private val testCategories = listOf(
        Category(
            id = "cat_food",
            name = "Comida y Restaurantes",
            type = TransactionType.EXPENSE,
            iconName = "restaurant",
            colorHex = "#F59E0B"
        ),
        Category(
            id = "cat_transport",
            name = "Transporte",
            type = TransactionType.EXPENSE,
            iconName = "directions_car",
            colorHex = "#3B82F6"
        ),
        Category(
            id = "cat_salary",
            name = "Sueldo y Nómina",
            type = TransactionType.INCOME,
            iconName = "payments",
            colorHex = "#10B981"
        )
    )

    @Test
    fun testLocalHeuristicExpenseDetection() {
        val useCase = ParseVoiceTransactionUseCase(
            aiConfigStorage = FakeAiConfigStorage(key = null),
            geminiApiClient = FakeGeminiApiClient(Result.failure(IllegalStateException("No API key")))
        )

        val result = useCase.extractWithLocalHeuristic(
            voiceText = "Gasté 45 mil pesos en un almuerzo con Bancolombia",
            accounts = testAccounts,
            categories = testCategories
        )

        assertEquals(45000.0, result.amount)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("acc_bancolombia", result.suggestedAccountId)
        assertEquals("cat_food", result.suggestedCategoryId)
        assertTrue(result.note.isNotBlank())
    }

    @Test
    fun testLocalHeuristicIncomeDetection() {
        val useCase = ParseVoiceTransactionUseCase(
            aiConfigStorage = FakeAiConfigStorage(key = null),
            geminiApiClient = FakeGeminiApiClient(Result.failure(IllegalStateException("No API key")))
        )

        val result = useCase.extractWithLocalHeuristic(
            voiceText = "Me pagaron 2 millones de nómina",
            accounts = testAccounts,
            categories = testCategories
        )

        assertEquals(2000000.0, result.amount)
        assertEquals(TransactionType.INCOME, result.type)
        assertEquals("cat_salary", result.suggestedCategoryId)
    }

    @Test
    fun testLocalHeuristicDateRecognition() {
        val useCase = ParseVoiceTransactionUseCase(
            aiConfigStorage = FakeAiConfigStorage(key = null),
            geminiApiClient = FakeGeminiApiClient(Result.failure(IllegalStateException("No API key")))
        )

        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val result = useCase.extractWithLocalHeuristic(
            voiceText = "Ayer pagué 15000 de transporte",
            accounts = testAccounts,
            categories = testCategories
        )

        assertEquals(15000.0, result.amount)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals("cat_transport", result.suggestedCategoryId)
        // La fecha debe ser aproximadamente 24 horas antes
        assertTrue(result.timestamp < now)
    }

    @Test
    fun testBlankVoiceTextFails() = runBlocking {
        val useCase = ParseVoiceTransactionUseCase(
            aiConfigStorage = FakeAiConfigStorage(key = null),
            geminiApiClient = FakeGeminiApiClient(Result.failure(IllegalStateException("No API key")))
        )

        val result = useCase.invoke("   ", testAccounts, testCategories)
        assertTrue(result.isFailure)
    }

    @Test
    fun testGeminiResponseParsingSuccess() = runBlocking {
        val mockJson = """
            {
              "amount": 35000.0,
              "type": "EXPENSE",
              "suggestedAccountId": "acc_bancolombia",
              "suggestedCategoryId": "cat_food",
              "cleanNote": "Almuerzo Crepes",
              "date": "2026-09-29",
              "confidence": 0.98
            }
        """.trimIndent()

        val useCase = ParseVoiceTransactionUseCase(
            aiConfigStorage = FakeAiConfigStorage(key = "test_gemini_key", enabled = true),
            geminiApiClient = FakeGeminiApiClient(Result.success(mockJson))
        )

        val result = useCase.invoke(
            voiceText = "Pagué 35 mil en Crepes con Bancolombia",
            accounts = testAccounts,
            categories = testCategories
        )

        assertTrue(result.isSuccess)
        val tx = result.getOrThrow()
        assertEquals(35000.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("acc_bancolombia", tx.suggestedAccountId)
        assertEquals("cat_food", tx.suggestedCategoryId)
        assertEquals("Almuerzo Crepes", tx.note)
    }

    @Test
    fun testFallbackToHeuristicWhenGeminiFails() = runBlocking {
        val useCase = ParseVoiceTransactionUseCase(
            aiConfigStorage = FakeAiConfigStorage(key = "test_gemini_key", enabled = true),
            geminiApiClient = FakeGeminiApiClient(Result.failure(RuntimeException("Gemini network error")))
        )

        val result = useCase.invoke(
            voiceText = "Gasté 50 mil en comida",
            accounts = testAccounts,
            categories = testCategories
        )

        // Debe caer en el parser heurístico exitosamente sin fallar
        assertTrue(result.isSuccess)
        val tx = result.getOrThrow()
        assertEquals(50000.0, tx.amount)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("cat_food", tx.suggestedCategoryId)
    }
}
