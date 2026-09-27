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
            // Calcular balance disponible consolidado (dinero propio)
            // Se suman cuentas de ahorros, corriente, efectivo y billeteras digitales.
            // Las tarjetas de crédito representan una línea de crédito/deuda (no dinero disponible propio)
            // y no se descuentan aquí para no duplicar el egreso cuando el usuario pague la tarjeta desde sus cuentas reales.
            var totalBalance = 0.0
            for (acc in accounts) {
                if (acc.isArchived) continue
                if (acc.type != AccountType.CREDIT_CARD) {
                    totalBalance += acc.balance
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
                totalBalance = totalBalance,
                monthlyIncome = incomeTotal,
                monthlyExpenses = expensesTotal,
                accounts = accounts,
                recentTransactions = recentTransactions,
                currency = defaultCurrency
            )
        }
    }
}
