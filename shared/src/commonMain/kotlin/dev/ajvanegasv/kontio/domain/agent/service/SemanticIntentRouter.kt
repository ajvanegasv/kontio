package dev.ajvanegasv.kontio.domain.agent.service

import dev.ajvanegasv.kontio.domain.agent.tools.KontioToolRegistry
import kotlinx.serialization.json.JsonPrimitive

/**
 * Representa la decisión de enrutamiento inferida a partir de una consulta en lenguaje natural.
 */
data class RoutedIntent(
    val toolName: String,
    val arguments: Map<String, JsonPrimitive>,
    val isDisallowedMutation: Boolean = false,
    val rejectionMessage: String? = null
)

/**
 * Enrutador semántico de intenciones financieras para funcionamiento local y offline.
 * Identifica la intención de la consulta del usuario sin requerir conexión a internet ni llamadas a LLMs,
 * garantizando que Kontio AI responda de inmediato y aplique las restricciones de solo lectura.
 */
object SemanticIntentRouter {

    private val disallowedMutationWords = listOf(
        "eliminar", "elimina", "borrar", "borra", "suprimir",
        "crear", "crea", "agregar", "agrega", "añadir", "añade", "nuevo", "nueva",
        "modificar", "modifica", "editar", "edita", "cambiar", "actualizar",
        "transferir", "transfiere", "enviar dinero"
    )

    private val accountKeywords = listOf(
        "cuanto tengo", "cuánto tengo", "cuanto dinero", "cuánto dinero",
        "mis cuentas", "mis bancos", "mi banco", "mis saldos", "saldo total",
        "saldo consolidado", "saldo en", "saldo de", "tarjeta de credito",
        "tarjetas de crédito", "tarjetas", "cupo disponible", "cupo",
        "patrimonio", "efectivo", "billetera"
    )

    private val categoryRankingKeywords = listOf(
        "en que gasto mas", "en qué gasto más", "en que gasto", "en qué gasto",
        "gastos por categoria", "gastos por categoría", "desglose por categoria",
        "desglose por categoría", "distribución de gastos", "distribucion de gastos"
    )

    private val categoryCatalogKeywords = listOf(
        "que categorias", "qué categorías", "mis categorias", "mis categorías",
        "cuales categorias", "cuáles categorías", "lista de categorias", "lista de categorías"
    )

    private val recentTxKeywords = listOf(
        "ultimos movimientos", "últimos movimientos", "movimientos recientes",
        "transacciones recientes", "ultimas transacciones", "últimas transacciones",
        "ultimas compras", "últimas compras", "que he pagado recientemente",
        "qué he pagado recientemente", "ultimos pagos", "últimos pagos"
    )

    private val overviewKeywords = listOf(
        "como van mis finanzas", "cómo van mis finanzas", "resumen financiero",
        "resumen del mes", "resumen de este mes", "ingresos vs gastos",
        "cuanto he ahorrado", "cuánto he ahorrado", "tasa de ahorro", "balance general"
    )

    fun route(query: String): RoutedIntent {
        val clean = query.trim('?', '¿', '!', '¡', '.', ':', ',', ';', ' ').lowercase()

        // 1. Detección de intentos de mutación (Restricción de Solo Lectura)
        val matchedMutationWord = disallowedMutationWords.firstOrNull { word ->
            Regex("""\b$word\b""").containsMatchIn(clean)
        }
        if (matchedMutationWord != null) {
            val rejection = "Kontio AI opera en modo estricto de **solo lectura**. No puedo $matchedMutationWord información financiera. Puedes gestionar tus transacciones y cuentas directamente en sus respectivas pantallas dentro de Kontio."
            return RoutedIntent(
                toolName = "none",
                arguments = emptyMap(),
                isDisallowedMutation = true,
                rejectionMessage = rejection
            )
        }

        // 2. Detección de intenciones sobre categorías (ranking vs catálogo)
        val isCategoryQuery = clean.contains("categor")
        val isExplicitRanking = categoryRankingKeywords.any { clean.contains(it) } ||
                (isCategoryQuery && (clean.contains("más") || clean.contains("mas") || clean.contains("distribución") || clean.contains("desglose") || clean.contains("ranking")))

        if (isExplicitRanking) {
            val period = when {
                clean.contains("el mes pasado") || clean.contains("mes anterior") -> "PREVIOUS_MONTH"
                clean.contains("este año") || clean.contains("del año") -> "CURRENT_YEAR"
                clean.contains("ultimos 3 meses") || clean.contains("últimos 3 meses") -> "LAST_3_MONTHS"
                else -> "CURRENT_MONTH"
            }
            return RoutedIntent(
                toolName = "get_category_spending",
                arguments = mapOf("period" to JsonPrimitive(period))
            )
        }

        if (isCategoryQuery || categoryCatalogKeywords.any { clean.contains(it) }) {
            val typeFilter = when {
                clean.contains("gasto") -> "EXPENSE"
                clean.contains("ingreso") -> "INCOME"
                else -> "ALL"
            }
            return RoutedIntent(
                toolName = "get_categories",
                arguments = mapOf("type" to JsonPrimitive(typeFilter))
            )
        }

        // 3. Detección de intenciones sobre cuentas y bancos
        if (accountKeywords.any { clean.contains(it) }) {
            val isCardQuery = clean.contains("tarjeta") || clean.contains("crédito") || clean.contains("credito")

            if (isCardQuery) {
                return RoutedIntent(
                    toolName = "get_accounts_summary",
                    arguments = mapOf("filter_type" to JsonPrimitive("CREDIT_CARD"))
                )
            }

            // Si el usuario pregunta por una cuenta específica (ej. "saldo de Bancolombia" o "cuánto tengo en Nequi")
            val targetSpecificBank = when {
                clean.contains("en ") -> clean.substringAfter("en ").trim()
                clean.contains("de ") -> clean.substringAfter("de ").trim()
                else -> null
            }

            val genericTerms = listOf("total", "mis cuentas", "mis bancos", "mi cuenta", "efectivo", "dinero", "todas", "mi banco", "mis saldos", "cuentas")
            if (!targetSpecificBank.isNullOrBlank() && genericTerms.none { targetSpecificBank.contains(it) }) {
                return RoutedIntent(
                    toolName = "get_account_detail",
                    arguments = mapOf("account_query" to JsonPrimitive(targetSpecificBank))
                )
            }

            val filterType = when {
                clean.contains("ahorro") || clean.contains("ahorros") -> "SAVINGS"
                clean.contains("efectivo") -> "CASH"
                else -> "ALL"
            }

            return RoutedIntent(
                toolName = "get_accounts_summary",
                arguments = mapOf("filter_type" to JsonPrimitive(filterType))
            )
        }

        // 5. Detección de movimientos recientes
        if (recentTxKeywords.any { clean.contains(it) }) {
            return RoutedIntent(
                toolName = "get_recent_transactions",
                arguments = mapOf("limit" to JsonPrimitive(10))
            )
        }

        // 6. Detección de balance/resumen general
        if (overviewKeywords.any { clean.contains(it) }) {
            val period = when {
                clean.contains("el mes pasado") || clean.contains("mes anterior") -> "PREVIOUS_MONTH"
                clean.contains("este año") -> "CURRENT_YEAR"
                else -> "CURRENT_MONTH"
            }
            return RoutedIntent(
                toolName = "get_financial_overview",
                arguments = mapOf("period" to JsonPrimitive(period))
            )
        }

        // 7. Enrutamiento por defecto: Búsqueda de transacciones con el término extraído
        return RoutedIntent(
            toolName = "search_transactions",
            arguments = mapOf("query" to JsonPrimitive(query.trim()))
        )
    }
}
