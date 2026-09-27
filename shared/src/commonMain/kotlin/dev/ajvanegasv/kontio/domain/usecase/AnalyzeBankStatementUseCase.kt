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
            val apiKey = aiConfigStorage.getApiKey()
            if (apiKey.isNullOrBlank()) {
                throw IllegalStateException("No has configurado una API Key de Google Gemini. Por favor agrégala en Más > Configuración de IA.")
            }

            val categories = categoryRepository.getCategories().first()
            val accounts = accountRepository.getAccounts().first()

            val rawResult = geminiStatementParser.parseStatement(
                file = file,
                apiKey = apiKey,
                categories = categories,
                accounts = accounts
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
