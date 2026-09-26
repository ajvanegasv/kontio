package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.model.DashboardSummary
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetDashboardSummaryUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<DashboardSummary> {
        return combine(
            accountRepository.getAccounts(),
            transactionRepository.getRecentTransactions(limit = 10)
        ) { accounts, recentTransactions ->
            // Calcular balance neto consolidado
            // Cuentas de débito/ahorros/efectivo suman; tarjetas de crédito representan deuda (restan del patrimonio neto)
            var netBalance = 0.0
            for (acc in accounts) {
                if (acc.isArchived) continue
                if (acc.type == AccountType.CREDIT_CARD) {
                    netBalance -= acc.balance
                } else {
                    netBalance += acc.balance
                }
            }

            // Calcular ingresos y egresos de las transacciones recientes / mes
            var incomeTotal = 0.0
            var expensesTotal = 0.0

            for (tx in recentTransactions) {
                when (tx.type) {
                    TransactionType.INCOME -> incomeTotal += tx.amount
                    TransactionType.EXPENSE -> expensesTotal += tx.amount
                    TransactionType.TRANSFER -> Unit
                }
            }

            val defaultCurrency = accounts.firstOrNull()?.currency ?: "USD"

            DashboardSummary(
                totalBalance = netBalance,
                monthlyIncome = incomeTotal,
                monthlyExpenses = expensesTotal,
                accounts = accounts,
                recentTransactions = recentTransactions,
                currency = defaultCurrency
            )
        }
    }
}
