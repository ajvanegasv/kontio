package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.Transaction
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull

class DeleteTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(transactionId: String): Result<Unit> {
        return runCatching {
            val transaction = transactionRepository.getTransactionById(transactionId)
            if (transaction != null) {
                when (transaction.type) {
                    TransactionType.EXPENSE -> {
                        val account = accountRepository.getAccountById(transaction.accountId).firstOrNull()
                        if (account != null) {
                            val newBalance = if (account.type == AccountType.CREDIT_CARD) {
                                (account.balance - transaction.amount).coerceAtLeast(0.0)
                            } else {
                                account.balance + transaction.amount
                            }
                            accountRepository.updateBalance(account.id, newBalance)
                        }
                    }
                    TransactionType.INCOME -> {
                        val account = accountRepository.getAccountById(transaction.accountId).firstOrNull()
                        if (account != null) {
                            val newBalance = if (account.type == AccountType.CREDIT_CARD) {
                                account.balance + transaction.amount
                            } else {
                                account.balance - transaction.amount
                            }
                            accountRepository.updateBalance(account.id, newBalance)
                        }
                    }
                    TransactionType.TRANSFER -> {
                        val sourceAccount = accountRepository.getAccountById(transaction.accountId).firstOrNull()
                        if (sourceAccount != null) {
                            accountRepository.updateBalance(
                                sourceAccount.id,
                                sourceAccount.balance + transaction.amount
                            )
                        }
                        val targetId = transaction.targetAccountId
                        if (targetId != null) {
                            val targetAccount = accountRepository.getAccountById(targetId).firstOrNull()
                            if (targetAccount != null) {
                                accountRepository.updateBalance(
                                    targetAccount.id,
                                    targetAccount.balance - transaction.amount
                                )
                            }
                        }
                    }
                }
            }
            transactionRepository.deleteTransaction(transactionId)
        }
    }

    suspend operator fun invoke(transaction: Transaction): Result<Unit> = invoke(transaction.id)
}
