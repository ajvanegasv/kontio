package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository

class UpdateAccountUseCase(
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Result<Unit> {
        return runCatching {
            require(account.id.isNotBlank()) { "El ID de la cuenta no puede estar vacío" }
            require(account.name.isNotBlank()) { "El nombre de la cuenta no puede estar vacío" }
            if (account.type == AccountType.CREDIT_CARD) {
                require(account.creditLimit != null && account.creditLimit > 0) {
                    "Las tarjetas de crédito deben tener un cupo límite asignado mayor a cero"
                }
            }
            if (account.cutoffDay != null) {
                require(account.cutoffDay in 1..31) { "El día de corte debe estar entre 1 y 31" }
            }
            if (account.dueDay != null) {
                require(account.dueDay in 1..31) { "El día límite de pago debe estar entre 1 y 31" }
            }
            accountRepository.updateAccount(account)
        }
    }
}
