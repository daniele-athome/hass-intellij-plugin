package it.casaricci.hass.plugin

import com.intellij.openapi.module.Module
import it.casaricci.hass.plugin.facet.HassFacetState
import it.casaricci.hass.plugin.facet.getFacetState

/**
 * Returns the module configuration from our facet or from project settings
 * (where facets are not available, e.g. PyCharm)
 */
fun getConfiguration(module: Module): HassFacetState? {
    // TODO read from project configuration if no facet is found
    return getFacetState(module)
}
