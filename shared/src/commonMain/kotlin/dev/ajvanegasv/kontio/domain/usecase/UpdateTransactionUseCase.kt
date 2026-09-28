package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class UpdateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(updatedTransaction: Transaction): Result<Unit> {
        return runCatching {
            require(updatedTransaction.amount > 0) { "El monto debe ser mayor a cero" }
            require(updatedTransaction.accountId.isNotBlank()) { "Debes seleccionar una cuenta" }

            val originalTx = transactionRepository.getTransactionById(updatedTransaction.id)
                ?: throw IllegalArgumentException("La transacción a editar no existe")

            // 1. Revertir impacto de la transacción original en las cuentas involucradas
            when (originalTx.type) {
                TransactionType.EXPENSE -> {
                    val originalAccount = accountRepository.getAccountById(originalTx.accountId).firstOrNull()
                    if (originalAccount != null) {
                        val restoredBalance = if (originalAccount.type == AccountType.CREDIT_CARD) {
                            (originalAccount.balance - originalTx.amount).coerceAtLeast(0.0)
                        } else {
                            originalAccount.balance + originalTx.amount
                        }
                        accountRepository.updateBalance(originalAccount.id, restoredBalance)
                    }
                }
                TransactionType.INCOME -> {
                    val originalAccount = accountRepository.getAccountById(originalTx.accountId).firstOrNull()
                    if (originalAccount != null) {
                        val restoredBalance = if (originalAccount.type == AccountType.CREDIT_CARD) {
                            originalAccount.balance + originalTx.amount
                        } else {
                            originalAccount.balance - originalTx.amount
                        }
                        accountRepository.updateBalance(originalAccount.id, restoredBalance)
                    }
                }
                TransactionType.TRANSFER -> {
                    val sourceAccount = accountRepository.getAccountById(originalTx.accountId).firstOrNull()
                    if (sourceAccount != null) {
                        val restoredSourceBalance = if (sourceAccount.type == AccountType.CREDIT_CARD) {
                            (sourceAccount.balance - originalTx.amount).coerceAtLeast(0.0)
                        } else {
                            sourceAccount.balance + originalTx.amount
                        }
                        accountRepository.updateBalance(sourceAccount.id, restoredSourceBalance)
                    }
                    val targetId = originalTx.targetAccountId
                    if (targetId != null) {
                        val targetAccount = accountRepository.getAccountById(targetId).firstOrNull()
                        if (targetAccount != null) {
                            val restoredTargetBalance = if (targetAccount.type == AccountType.CREDIT_CARD) {
                                targetAccount.balance + originalTx.amount
                            } else {
                                targetAccount.balance - originalTx.amount
                            }
                            accountRepository.updateBalance(targetAccount.id, restoredTargetBalance)
                        }
                    }
                }
            }

            // 2. Aplicar impacto de la transacción actualizada en la nueva cuenta (o misma cuenta con saldo revertido)
            val newSourceAccount = accountRepository.getAccountById(updatedTransaction.accountId).firstOrNull()
                ?: throw IllegalArgumentException("La cuenta seleccionada no existe")

            when (updatedTransaction.type) {
                TransactionType.EXPENSE -> {
                    val newBalance = if (newSourceAccount.type == AccountType.CREDIT_CARD) {
                        newSourceAccount.balance + updatedTransaction.amount
                    } else {
                        newSourceAccount.balance - updatedTransaction.amount
                    }
                    accountRepository.updateBalance(newSourceAccount.id, newBalance)
                }
                TransactionType.INCOME -> {
                    val newBalance = if (newSourceAccount.type == AccountType.CREDIT_CARD) {
                        (newSourceAccount.balance - updatedTransaction.amount).coerceAtLeast(0.0)
                    } else {
                        newSourceAccount.balance + updatedTransaction.amount
                    }
                    accountRepository.updateBalance(newSourceAccount.id, newBalance)
                }
                TransactionType.TRANSFER -> {
                    val targetId = updatedTransaction.targetAccountId
                        ?: throw IllegalArgumentException("Se requiere una cuenta de destino para transferencias")
                    val targetAccount = accountRepository.getAccountById(targetId).firstOrNull()
                        ?: throw IllegalArgumentException("La cuenta de destino no existe")

                    val newSourceBalance = if (newSourceAccount.type == AccountType.CREDIT_CARD) {
                        newSourceAccount.balance + updatedTransaction.amount
                    } else {
                        newSourceAccount.balance - updatedTransaction.amount
                    }
                    accountRepository.updateBalance(newSourceAccount.id, newSourceBalance)

                    val newTargetBalance = if (targetAccount.type == AccountType.CREDIT_CARD) {
                        (targetAccount.balance - updatedTransaction.amount).coerceAtLeast(0.0)
                    } else {
                        targetAccount.balance + updatedTransaction.amount
                    }
                    accountRepository.updateBalance(targetAccount.id, newTargetBalance)
                }
            }

            // 3. Guardar la transacción actualizada con la moneda correspondiente a la cuenta
            val finalTx = updatedTransaction.copy(
                currency = newSourceAccount.currency
            )
            transactionRepository.insertTransaction(finalTx)
        }
    }
}
