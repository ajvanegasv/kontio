package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository

class CreateAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Result<Unit> {
        return runCatching {
            require(account.name.isNotBlank()) { "El nombre de la cuenta no puede estar vacío" }
            if (account.type == AccountType.CREDIT_CARD) {
                require(account.creditLimit != null && account.creditLimit > 0) {
                    "Las tarjetas de crédito deben tener un cupo límite asignado mayor a cero"
                }
            }
            accountRepository.insertAccount(account)
        }
    }
}
