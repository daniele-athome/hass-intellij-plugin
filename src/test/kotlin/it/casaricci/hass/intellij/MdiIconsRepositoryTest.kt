package it.casaricci.hass.intellij

import it.casaricci.hass.intellij.services.MdiIconsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class MdiIconsRepositoryTest {

    companion object {
        // as of master @ 2025-01-09
        const val ICONS_COUNT = 7447
    }

    @Test
    fun testLoadIcons() {
        val icons = MdiIconsRepository.loadIcons()
        assertEquals(ICONS_COUNT, icons.size)
    }

}
