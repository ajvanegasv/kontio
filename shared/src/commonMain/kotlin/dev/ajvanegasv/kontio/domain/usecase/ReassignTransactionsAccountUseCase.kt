package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class ReassignTransactionsAccountUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(
        fromAccountId: String,
        toAccountId: String,
        transactionIds: List<String>? = null
    ): Result<Int> {
        return runCatching {
            require(fromAccountId.isNotBlank()) { "Cuenta de origen requerida" }
            require(toAccountId.isNotBlank()) { "Cuenta de destino requerida" }
            require(fromAccountId != toAccountId) { "La cuenta de origen y destino no pueden ser la misma" }

            val fromAccount = accountRepository.getAccountById(fromAccountId).firstOrNull()
                ?: throw IllegalArgumentException("La cuenta de origen no existe")
            val toAccount = accountRepository.getAccountById(toAccountId).firstOrNull()
                ?: throw IllegalArgumentException("La cuenta de destino no existe")

            val allFromTransactions = transactionRepository.getTransactionsByAccount(fromAccountId).firstOrNull() ?: emptyList()
            val transactionsToMove = if (transactionIds != null) {
                allFromTransactions.filter { it.id in transactionIds }
            } else {
                allFromTransactions
            }

            if (transactionsToMove.isEmpty()) return@runCatching 0

            // 1. Revertir impacto de todas las transacciones movidas en fromAccount
            var revertedFromBalance = fromAccount.balance
            for (tx in transactionsToMove) {
                when (tx.type) {
                    TransactionType.EXPENSE -> {
                        revertedFromBalance = if (fromAccount.type == AccountType.CREDIT_CARD) {
                            (revertedFromBalance - tx.amount).coerceAtLeast(0.0)
                        } else {
                            revertedFromBalance + tx.amount
                        }
                    }
                    TransactionType.INCOME -> {
                        revertedFromBalance = if (fromAccount.type == AccountType.CREDIT_CARD) {
                            revertedFromBalance + tx.amount
                        } else {
                            revertedFromBalance - tx.amount
                        }
                    }
                    TransactionType.TRANSFER -> {
                        // Si la cuenta de origen era fromAccount, revertimos el débito
                        revertedFromBalance = if (fromAccount.type == AccountType.CREDIT_CARD) {
                            (revertedFromBalance - tx.amount).coerceAtLeast(0.0)
                        } else {
                            revertedFromBalance + tx.amount
                        }
                    }
                }
            }

            // 2. Aplicar impacto de todas las transacciones en toAccount
            var updatedToBalance = toAccount.balance
            for (tx in transactionsToMove) {
                when (tx.type) {
                    TransactionType.EXPENSE -> {
                        updatedToBalance = if (toAccount.type == AccountType.CREDIT_CARD) {
                            updatedToBalance + tx.amount
                        } else {
                            updatedToBalance - tx.amount
                        }
                    }
                    TransactionType.INCOME -> {
                        updatedToBalance = if (toAccount.type == AccountType.CREDIT_CARD) {
                            (updatedToBalance - tx.amount).coerceAtLeast(0.0)
                        } else {
                            updatedToBalance + tx.amount
                        }
                    }
                    TransactionType.TRANSFER -> {
                        updatedToBalance = if (toAccount.type == AccountType.CREDIT_CARD) {
                            updatedToBalance + tx.amount
                        } else {
                            updatedToBalance - tx.amount
                        }
                    }
                }
            }

            // 3. Persistir saldos actualizados
            accountRepository.updateBalance(fromAccountId, revertedFromBalance)
            accountRepository.updateBalance(toAccountId, updatedToBalance)

            // 4. Actualizar transacciones con la nueva cuenta y su respectiva moneda
            val updatedTransactions = transactionsToMove.map { tx ->
                tx.copy(
                    accountId = toAccountId,
                    currency = toAccount.currency
                )
            }
            transactionRepository.insertTransactions(updatedTransactions)

            updatedTransactions.size
        }
    }
}
