package it.casaricci.hass.plugin

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun testSplitEntityId() {
        val (domainName, entityName) = splitEntityId("input_select.house_mode")
        assertEquals("input_select", domainName)
        assertEquals("house_mode", entityName)
    }

}
