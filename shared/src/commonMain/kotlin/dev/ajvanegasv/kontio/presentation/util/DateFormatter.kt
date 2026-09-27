package dev.ajvanegasv.kontio.presentation.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object DateFormatter {

    fun toLocalDate(timestamp: Long): LocalDate {
        val timeZone = TimeZone.currentSystemDefault()
        return Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date
    }

    fun formatDateGroup(timestamp: Long): String {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val txDate = toLocalDate(timestamp)

        return when {
            txDate == today -> "Hoy"
            txDate.toEpochDays() == today.toEpochDays() - 1 -> "Ayer"
            else -> {
                val monthName = getSpanishMonthName(txDate.monthNumber)
                "${txDate.dayOfMonth} de $monthName, ${txDate.year}"
            }
        }
    }

    fun formatDisplayDate(timestamp: Long): String {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val txDate = toLocalDate(timestamp)

        return when {
            txDate == today -> "Hoy (${txDate.dayOfMonth} de ${getSpanishMonthName(txDate.monthNumber)})"
            txDate.toEpochDays() == today.toEpochDays() - 1 -> "Ayer (${txDate.dayOfMonth} de ${getSpanishMonthName(txDate.monthNumber)})"
            else -> "${txDate.dayOfMonth} de ${getSpanishMonthName(txDate.monthNumber)}, ${txDate.year}"
        }
    }


    fun formatTime(timestamp: Long): String {
        val timeZone = TimeZone.currentSystemDefault()
        val ldt = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone)
        val hour = ldt.hour.toString().padStart(2, '0')
        val minute = ldt.minute.toString().padStart(2, '0')
        return "$hour:$minute"
    }

    private fun getSpanishMonthName(monthNumber: Int): String {
        return when (monthNumber) {
            1 -> "Enero"
            2 -> "Febrero"
            3 -> "Marzo"
            4 -> "Abril"
            5 -> "Mayo"
            6 -> "Junio"
            7 -> "Julio"
            8 -> "Agosto"
            9 -> "Septiembre"
            10 -> "Octubre"
            11 -> "Noviembre"
            12 -> "Diciembre"
            else -> ""
        }
    }
}
