package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.SavingsCashWidgetSummary
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

class GetSavingsCashWidgetDataUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(clock: Clock = Clock.System): Flow<SavingsCashWidgetSummary> {
        val now = clock.now()
        val timeZone = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(timeZone).date
        val startOfMonth = LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(timeZone).toEpochMilliseconds()

        return combine(
            accountRepository.getAccounts(),
            transactionRepository.getAllTransactions()
        ) { accounts, allTransactions ->
            val targetAccounts = accounts.filter { !it.isArchived && (it.type == AccountType.SAVINGS || it.type == AccountType.CASH) }
            val targetAccountIds = targetAccounts.map { it.id }.toSet()

            var totalBalance = 0.0
            for (acc in targetAccounts) {
                totalBalance += acc.balance
            }

            var incomeSum = 0.0
            var expensesSum = 0.0

            for (tx in allTransactions) {
                if (tx.accountId in targetAccountIds && tx.timestamp >= startOfMonth) {
                    when (tx.type) {
                        TransactionType.INCOME -> incomeSum += tx.amount
                        TransactionType.EXPENSE -> expensesSum += tx.amount
                        TransactionType.TRANSFER -> Unit
                    }
                }
            }

            val currency = targetAccounts.firstOrNull()?.currency
                ?: accounts.firstOrNull()?.currency
                ?: "USD"

            SavingsCashWidgetSummary(
                totalBalance = totalBalance,
                monthlyIncome = incomeSum,
                monthlyExpenses = expensesSum,
                netMonthlyFlow = incomeSum - expensesSum,
                accountsCount = targetAccounts.size,
                currency = currency
            )
        }
    }
}
