package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        return runCatching {
            require(transaction.amount > 0) { "El monto debe ser mayor a cero" }
            require(transaction.accountId.isNotBlank()) { "Debes seleccionar una cuenta" }

            val sourceAccount = accountRepository.getAccountById(transaction.accountId).firstOrNull()
                ?: throw IllegalArgumentException("La cuenta de origen no existe")

            // Actualizar balance de la cuenta según el tipo de producto y transacción
            when (transaction.type) {
                TransactionType.EXPENSE -> {
                    val newBalance = if (sourceAccount.type == AccountType.CREDIT_CARD) {
                        // En tarjetas de crédito, un gasto aumenta el saldo adeudado
                        sourceAccount.balance + transaction.amount
                    } else {
                        // En cuentas de ahorro, corriente o efectivo, el gasto reduce el saldo
                        sourceAccount.balance - transaction.amount
                    }
                    accountRepository.updateBalance(sourceAccount.id, newBalance)
                }
                TransactionType.INCOME -> {
                    val newBalance = if (sourceAccount.type == AccountType.CREDIT_CARD) {
                        // En tarjeta de crédito, un pago reduce la deuda
                        (sourceAccount.balance - transaction.amount).coerceAtLeast(0.0)
                    } else {
                        // En cuentas corrientes o ahorro, el ingreso aumenta el saldo
                        sourceAccount.balance + transaction.amount
                    }
                    accountRepository.updateBalance(sourceAccount.id, newBalance)
                }
                TransactionType.TRANSFER -> {
                    val targetId = transaction.targetAccountId
                        ?: throw IllegalArgumentException("Se requiere una cuenta de destino para transferencias")
                    val targetAccount = accountRepository.getAccountById(targetId).firstOrNull()
                        ?: throw IllegalArgumentException("La cuenta de destino no existe")

                    // Origen debita
                    accountRepository.updateBalance(sourceAccount.id, sourceAccount.balance - transaction.amount)
                    // Destino acredita
                    accountRepository.updateBalance(targetAccount.id, targetAccount.balance + transaction.amount)
                }
            }

            // Registrar transacción en la base de datos
            transactionRepository.insertTransaction(transaction)
        }
    }
}
