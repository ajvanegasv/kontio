package dev.ajvanegasv.kontio.presentation.util

import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateFormatterTest {

    @Test
    fun testFormatDateGroupForToday() {
        val now = Clock.System.now().toEpochMilliseconds()
        val result = DateFormatter.formatDateGroup(now)
        assertEquals("Hoy", result)
    }

    @Test
    fun testFormatDateGroupForYesterday() {
        val yesterday = Clock.System.now().toEpochMilliseconds() - 86_400_000L
        val result = DateFormatter.formatDateGroup(yesterday)
        assertEquals("Ayer", result)
    }

    @Test
    fun testFormatTimeNotEmpty() {
        val now = Clock.System.now().toEpochMilliseconds()
        val time = DateFormatter.formatTime(now)
        assertTrue(time.contains(":"))
        assertEquals(5, time.length)
    }

    @Test
    fun testFormatDisplayDateForTodayAndYesterday() {
        val now = Clock.System.now().toEpochMilliseconds()
        val todayStr = DateFormatter.formatDisplayDate(now)
        assertTrue(todayStr.startsWith("Hoy"))

        val yesterday = now - 86_400_000L
        val yesterdayStr = DateFormatter.formatDisplayDate(yesterday)
        assertTrue(yesterdayStr.startsWith("Ayer"))
    }
}

