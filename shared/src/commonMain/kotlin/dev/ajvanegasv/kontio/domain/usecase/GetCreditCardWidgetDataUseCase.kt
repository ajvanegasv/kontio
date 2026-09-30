package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.CreditCardWidgetSummary
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

class GetCreditCardWidgetDataUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(clock: Clock = Clock.System): Flow<CreditCardWidgetSummary> {
        val now = clock.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        val startOfMonth = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()

        return combine(
            accountRepository.getAccounts(),
            transactionRepository.getAllTransactions()
        ) { accounts, allTransactions ->
            val creditAccounts = accounts.filter { !it.isArchived && it.type == AccountType.CREDIT_CARD }
            val creditAccountIds = creditAccounts.map { it.id }.toSet()

            var totalDebt = 0.0
            var totalCreditLimit = 0.0
            var availableCredit = 0.0

            for (acc in creditAccounts) {
                totalDebt += acc.balance
                if (acc.creditLimit != null) {
                    totalCreditLimit += acc.creditLimit
                    availableCredit += acc.availableCredit ?: 0.0
                }
            }

            var monthlyExpenses = 0.0

            for (tx in allTransactions) {
                if (tx.accountId in creditAccountIds && tx.timestamp >= startOfMonth) {
                    if (tx.type == TransactionType.EXPENSE) {
                        monthlyExpenses += tx.amount
                    }
                }
            }

            val currency = creditAccounts.firstOrNull()?.currency
                ?: accounts.firstOrNull()?.currency
                ?: "USD"

            CreditCardWidgetSummary(
                totalDebt = totalDebt,
                monthlyExpenses = monthlyExpenses,
                totalCreditLimit = totalCreditLimit,
                availableCredit = availableCredit,
                cardsCount = creditAccounts.size,
                currency = currency
            )
        }
    }
}
