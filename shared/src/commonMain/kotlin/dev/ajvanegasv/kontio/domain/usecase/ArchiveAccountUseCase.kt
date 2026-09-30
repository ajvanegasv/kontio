package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.repository.AccountRepository

class ArchiveAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(accountId: String): Result<Unit> = runCatching {
        require(accountId.isNotBlank()) { "El ID de la cuenta no puede estar vacío" }
        accountRepository.archiveAccount(accountId)
    }

    suspend operator fun invoke(account: Account): Result<Unit> = invoke(account.id)
}
