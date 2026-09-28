package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.data.local.AiConfigStorage
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiStatementParser
import dev.ajvanegasv.kontio.domain.model.ParsedStatementResult
import dev.ajvanegasv.kontio.domain.model.StatementFile
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlin.math.abs

class AnalyzeBankStatementUseCase(
    private val geminiStatementParser: GeminiStatementParser,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val aiConfigStorage: AiConfigStorage
) {
    suspend operator fun invoke(file: StatementFile): Result<ParsedStatementResult> {
        return runCatching {
            if (!aiConfigStorage.isAiEnabled()) {
                throw IllegalStateException("La Inteligencia Artificial está desactivada en la configuración. Puedes habilitarla en Más > Configuración de IA.")
            }

            val apiKey = aiConfigStorage.getApiKey()
            if (apiKey.isNullOrBlank()) {
                throw IllegalStateException("No has configurado una API Key de Google Gemini. Por favor agrégala en Más > Configuración de IA.")
            }

            val categories = categoryRepository.getCategories().first()
            val accounts = accountRepository.getAccounts().first()

            if (file.isPdf && file.bytes.size > 15 * 1024 * 1024) {
                throw IllegalArgumentException("El archivo PDF supera los 15MB. Por favor selecciona un extracto de menor tamaño o con menos páginas.")
            }

            val storedModel = aiConfigStorage.getModel().removePrefix("models/").trim()
            val model = if (storedModel.contains("1.5-flash") || storedModel.contains("2.0-flash") || storedModel.isBlank()) {
                val defaultModel = dev.ajvanegasv.kontio.data.remote.gemini.GeminiApiClient.DEFAULT_MODEL
                aiConfigStorage.setModel(defaultModel)
                defaultModel
            } else {
                storedModel
            }

            val rawResult = geminiStatementParser.parseStatement(
                file = file,
                apiKey = apiKey,
                categories = categories,
                accounts = accounts,
                model = model
            ).getOrThrow()

            // Detección de duplicados cruzando con transacciones existentes en base de datos
            val allExistingTx = transactionRepository.getAllTransactions().first()

            val enrichedItems = rawResult.items.map { item ->
                val isDuplicate = allExistingTx.any { existing ->
                    val timeDiff = abs(existing.timestamp - item.date)
                    val amountDiff = abs(existing.amount - item.amount)
                    timeDiff <= 36 * 3600 * 1000L && amountDiff < 0.01 && existing.type == item.type
                }

                if (isDuplicate) {
                    item.copy(isDuplicate = true, isSelected = false)
                } else {
                    item
                }
            }

            rawResult.copy(items = enrichedItems)
        }
    }
}
