package it.casaricci.hass.plugin

import it.casaricci.hass.plugin.services.MdiIconsRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class MdiIconsRepositoryTest {
    companion object {
        // as of master @ 2025-01-23
        const val ICONS_COUNT = 13670
    }

    @Test
    fun testLoadIcons() {
        val icons = MdiIconsRepository.loadIcons()
        assertEquals(ICONS_COUNT, icons.size)
    }
}
