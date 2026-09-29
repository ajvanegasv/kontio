package dev.ajvanegasv.kontio.domain.agent.tools

import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionDeclaration
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionParameters
import dev.ajvanegasv.kontio.data.remote.gemini.GeminiFunctionProperty
import dev.ajvanegasv.kontio.domain.agent.model.AgentToolResult
import dev.ajvanegasv.kontio.domain.agent.model.AgentVisualPayload
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import dev.ajvanegasv.kontio.domain.repository.AccountRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository
import dev.ajvanegasv.kontio.presentation.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Herramienta de solo lectura para consultar el resumen consolidado de cuentas bancarias,
 * tarjetas de crédito, billeteras digitales y saldo total en efectivo.
 */
class GetAccountsSummaryTool(
    private val accountRepository: AccountRepository
) : KontioAgentTool {

    override val name: String = "get_accounts_summary"

    override val description: String = """
        Consulta el listado completo de cuentas financieras y bancos del usuario, incluyendo saldos
        actuales, cupo disponible en tarjetas de crédito, moneda de cada cuenta y el total neto consolidado.
        Úsalo cuando el usuario pregunte: ¿cuánto dinero tengo?, ¿cuáles son mis bancos?, mis saldos, etc.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "filter_type" to GeminiFunctionProperty(
                    type = "STRING",
                    description = "Filtro opcional por tipo de cuenta.",
                    enum = listOf("ALL", "SAVINGS", "CHECKING", "CREDIT_CARD", "CASH", "DIGITAL_WALLET")
                )
            ),
            required = emptyList()
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val filterTypeStr = args["filter_type"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "ALL"
        val allAccounts = accountRepository.getAccounts().first()

        val filteredAccounts = if (filterTypeStr != "ALL") {
            allAccounts.filter { it.type.name == filterTypeStr }
        } else {
            allAccounts
        }

        if (filteredAccounts.isEmpty()) {
            val emptySummary = if (allAccounts.isEmpty()) {
                "No tienes cuentas ni bancos registrados en Kontio todavía."
            } else {
                "No se encontraron cuentas del tipo '$filterTypeStr'."
            }
            return AgentToolResult(
                toolName = name,
                success = true,
                dataSummary = buildJsonObject {
                    put("count", 0)
                    put("message", emptySummary)
                },
                naturalLanguageSummary = emptySummary,
                speechSummary = emptySummary,
                visualPayload = AgentVisualPayload.AccountsSummaryPayload(
                    accounts = emptyList(),
                    totalNetWorthByCurrency = emptyMap(),
                    formattedNetWorth = "$0.00"
                )
            )
        }

        // Agrupar saldo neto por divisa (efectivo, ahorros, corriente suman; tarjeta de crédito resta si representa deuda)
        val netWorthByCurrency = mutableMapOf<String, Double>()
        filteredAccounts.forEach { acc ->
            val current = netWorthByCurrency.getOrElse(acc.currency) { 0.0 }
            val balanceContribution = if (acc.type == AccountType.CREDIT_CARD) -acc.balance else acc.balance
            netWorthByCurrency[acc.currency] = current + balanceContribution
        }

        val formattedNetWorth = netWorthByCurrency.entries.joinToString(" / ") { (currency, amount) ->
            CurrencyFormatter.format(amount, currency)
        }

        val accountItems = filteredAccounts.map { acc ->
            AgentVisualPayload.AccountSummaryItem(
                id = acc.id,
                name = acc.name,
                type = acc.type,
                balanceFormatted = CurrencyFormatter.format(acc.balance, acc.currency),
                balanceRaw = acc.balance,
                currency = acc.currency,
                availableCreditFormatted = acc.availableCredit?.let { CurrencyFormatter.format(it, acc.currency) },
                creditLimitFormatted = acc.creditLimit?.let { CurrencyFormatter.format(it, acc.currency) },
                colorHex = acc.colorHex,
                iconName = acc.iconName
            )
        }

        val jsonSummary = buildJsonObject {
            put("count", filteredAccounts.size)
            putJsonArray("accounts") {
                filteredAccounts.forEach { acc ->
                    add(buildJsonObject {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("type", acc.type.name)
                        put("balance", acc.balance)
                        put("currency", acc.currency)
                        acc.creditLimit?.let { put("creditLimit", it) }
                        acc.availableCredit?.let { put("availableCredit", it) }
                    })
                }
            }
            putJsonObject("netWorthByCurrency") {
                netWorthByCurrency.forEach { (curr, total) ->
                    put(curr, total)
                }
            }
        }

        val naturalSummary = buildString {
            append("Tienes ${filteredAccounts.size} cuenta(s) registrada(s). Saldo consolidado: $formattedNetWorth.\n\n")
            filteredAccounts.forEach { acc ->
                val typeName = when (acc.type) {
                    AccountType.SAVINGS -> "Ahorros"
                    AccountType.CHECKING -> "Corriente"
                    AccountType.CREDIT_CARD -> "Tarjeta de Crédito"
                    AccountType.CASH -> "Efectivo"
                    AccountType.DIGITAL_WALLET -> "Billetera Digital"
                }
                val formattedBalance = CurrencyFormatter.format(acc.balance, acc.currency)
                append("• **${acc.name}** ($typeName): $formattedBalance")
                if (acc.type == AccountType.CREDIT_CARD && acc.availableCredit != null) {
                    val formattedCredit = CurrencyFormatter.format(acc.availableCredit ?: 0.0, acc.currency)
                    append(" (Cupo disponible: $formattedCredit)")
                }
                append("\n")
            }
        }.trimEnd()

        val speechSummary = "Tienes ${filteredAccounts.size} cuentas con un saldo consolidado de $formattedNetWorth."

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = naturalSummary,
            speechSummary = speechSummary,
            visualPayload = AgentVisualPayload.AccountsSummaryPayload(
                accounts = accountItems,
                totalNetWorthByCurrency = netWorthByCurrency,
                formattedNetWorth = formattedNetWorth
            )
        )
    }
}

/**
 * Herramienta de solo lectura para consultar los detalles y movimientos recientes de una cuenta específica.
 */
class GetAccountDetailTool(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : KontioAgentTool {

    override val name: String = "get_account_detail"

    override val description: String = """
        Obtiene los detalles específicos de una cuenta o banco por su nombre o identificador,
        junto con sus últimos 5 movimientos.
        Ejemplo: para '¿cuánto tengo en Bancolombia?', usa account_query='Bancolombia'.
    """.trimIndent()

    override val declaration: GeminiFunctionDeclaration = GeminiFunctionDeclaration(
        name = name,
        description = description,
        parameters = GeminiFunctionParameters(
            properties = mapOf(
                "account_query" to GeminiFunctionProperty(
                    type = "STRING",
                    description = "Nombre o identificador de la cuenta a buscar (ej. 'Bancolombia', 'Efectivo', 'Tarjeta')."
                )
            ),
            required = listOf("account_query")
        )
    )

    override suspend fun execute(args: Map<String, JsonElement>): AgentToolResult {
        val query = args["account_query"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val allAccounts = accountRepository.getAccounts().first()

        val matchedAccount = allAccounts.firstOrNull { acc ->
            acc.name.contains(query, ignoreCase = true) || acc.id.equals(query, ignoreCase = true)
        }

        if (matchedAccount == null) {
            val notFoundMsg = "No encontré ninguna cuenta que coincida con '$query'."
            return AgentToolResult(
                toolName = name,
                success = false,
                dataSummary = buildJsonObject {
                    put("error", "ACCOUNT_NOT_FOUND")
                    put("message", notFoundMsg)
                },
                naturalLanguageSummary = notFoundMsg,
                speechSummary = notFoundMsg
            )
        }

        val recentTransactions = transactionRepository.getTransactionsByAccount(matchedAccount.id).first().take(5)
        val balanceFormatted = CurrencyFormatter.format(matchedAccount.balance, matchedAccount.currency)

        val jsonSummary = buildJsonObject {
            put("id", matchedAccount.id)
            put("name", matchedAccount.name)
            put("type", matchedAccount.type.name)
            put("balance", matchedAccount.balance)
            put("currency", matchedAccount.currency)
            matchedAccount.availableCredit?.let { put("availableCredit", it) }
            put("recentTxCount", recentTransactions.size)
        }

        val naturalSummary = buildString {
            append("Cuenta **${matchedAccount.name}**: Saldo actual de $balanceFormatted.")
            if (matchedAccount.type == AccountType.CREDIT_CARD && matchedAccount.availableCredit != null) {
                append(" Cupo disponible: ${CurrencyFormatter.format(matchedAccount.availableCredit ?: 0.0, matchedAccount.currency)}.")
            }
            if (recentTransactions.isNotEmpty()) {
                append("\n\nÚltimos movimientos:")
                recentTransactions.forEach { tx ->
                    val sign = if (tx.type.name == "INCOME") "+" else "-"
                    val amountFormatted = CurrencyFormatter.format(tx.amount, tx.currency)
                    append("\n• ${tx.note.ifBlank { "Movimiento" }}: $sign$amountFormatted")
                }
            }
        }

        val speechSummary = "La cuenta ${matchedAccount.name} tiene un saldo de $balanceFormatted."

        val singleAccountItem = AgentVisualPayload.AccountSummaryItem(
            id = matchedAccount.id,
            name = matchedAccount.name,
            type = matchedAccount.type,
            balanceFormatted = balanceFormatted,
            balanceRaw = matchedAccount.balance,
            currency = matchedAccount.currency,
            availableCreditFormatted = matchedAccount.availableCredit?.let { CurrencyFormatter.format(it, matchedAccount.currency) },
            creditLimitFormatted = matchedAccount.creditLimit?.let { CurrencyFormatter.format(it, matchedAccount.currency) },
            colorHex = matchedAccount.colorHex,
            iconName = matchedAccount.iconName
        )

        return AgentToolResult(
            toolName = name,
            success = true,
            dataSummary = jsonSummary,
            naturalLanguageSummary = naturalSummary,
            speechSummary = speechSummary,
            visualPayload = AgentVisualPayload.AccountsSummaryPayload(
                accounts = listOf(singleAccountItem),
                totalNetWorthByCurrency = mapOf(matchedAccount.currency to matchedAccount.balance),
                formattedNetWorth = balanceFormatted
            )
        )
    }
}
