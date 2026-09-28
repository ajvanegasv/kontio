package dev.ajvanegasv.kontio.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeModeTest {

    @Test
    fun themeModeValues_containsExpectedOptions() {
        val modes = ThemeMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertTrue(modes.contains(ThemeMode.LIGHT))
        assertTrue(modes.contains(ThemeMode.DARK))
    }

    @Test
    fun themeModeParsing_handlesValidNames() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
    }
}
