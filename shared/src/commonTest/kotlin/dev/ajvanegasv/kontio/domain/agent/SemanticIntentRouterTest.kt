package dev.ajvanegasv.kontio.domain.agent

import dev.ajvanegasv.kontio.domain.agent.service.SemanticIntentRouter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SemanticIntentRouterTest {

    @Test
    fun route_rejectsMutationOperationsWithReadOnlyWarning() {
        val deleteQuery = SemanticIntentRouter.route("Por favor elimina la transacción de Uber")
        val msg = deleteQuery.rejectionMessage
        assertNotNull(msg)
        assertTrue(msg.contains("solo lectura"))

        val createQuery = SemanticIntentRouter.route("crear una cuenta con 500 dólares")
        assertTrue(createQuery.isDisallowedMutation)
        assertTrue(createQuery.rejectionMessage?.contains("solo lectura") == true)

        val modifyQuery = SemanticIntentRouter.route("modificar el saldo de mi tarjeta")
        assertTrue(modifyQuery.isDisallowedMutation)
    }

    @Test
    fun route_identifiesAccountsAndBankQueries() {
        val generalBalance = SemanticIntentRouter.route("¿cuánto dinero tengo en total?")
        assertEquals("get_accounts_summary", generalBalance.toolName)

        val banksQuery = SemanticIntentRouter.route("¿cuáles son mis bancos y cuentas?")
        assertEquals("get_accounts_summary", banksQuery.toolName)

        val cardsQuery = SemanticIntentRouter.route("¿cuál es el cupo de mis tarjetas de crédito?")
        assertEquals("get_accounts_summary", cardsQuery.toolName)
        assertEquals("CREDIT_CARD", cardsQuery.arguments["filter_type"]?.content)

        val specificBank = SemanticIntentRouter.route("¿cuánto tengo en Bancolombia?")
        assertEquals("get_account_detail", specificBank.toolName)
        assertEquals("bancolombia", specificBank.arguments["account_query"]?.content?.lowercase())
    }

    @Test
    fun route_identifiesCategoryQueries() {
        val rankingQuery = SemanticIntentRouter.route("¿en qué categorías gasto más este mes?")
        assertEquals("get_category_spending", rankingQuery.toolName)

        val catalogQuery = SemanticIntentRouter.route("¿qué categorías de gastos tengo?")
        assertEquals("get_categories", catalogQuery.toolName)
    }

    @Test
    fun route_identifiesRecentTransactionsAndOverview() {
        val recentQuery = SemanticIntentRouter.route("muéstrame mis últimos movimientos")
        assertEquals("get_recent_transactions", recentQuery.toolName)

        val overviewQuery = SemanticIntentRouter.route("¿cómo van mis finanzas este mes?")
        assertEquals("get_financial_overview", overviewQuery.toolName)
    }

    @Test
    fun route_defaultsToSearchTransactions() {
        val searchQuery = SemanticIntentRouter.route("¿cuánto he gastado en Uber en 2026?")
        assertEquals("search_transactions", searchQuery.toolName)
    }
}
