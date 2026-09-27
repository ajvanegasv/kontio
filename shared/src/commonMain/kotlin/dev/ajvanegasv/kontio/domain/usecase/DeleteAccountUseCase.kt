package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository

class DeleteAccountUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(accountId: String): Result<Unit> {
        return runCatching {
            require(accountId.isNotBlank()) { "El ID de la cuenta no puede estar vacío" }
            transactionRepository.deleteTransactionsByAccountId(accountId)
            accountRepository.deleteAccount(accountId)
        }
    }

    suspend operator fun invoke(account: Account): Result<Unit> = invoke(account.id)
}
