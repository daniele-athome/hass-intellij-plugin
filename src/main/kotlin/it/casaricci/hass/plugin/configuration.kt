package it.casaricci.hass.plugin

import com.intellij.openapi.module.Module
import it.casaricci.hass.plugin.facet.HassFacetState
import it.casaricci.hass.plugin.facet.getFacetState
import it.casaricci.hass.plugin.facet.moduleHasFacet
import it.casaricci.hass.plugin.settings.ProjectSettings
import it.casaricci.hass.plugin.settings.usesProjectSettings

/**
 * Returns the module configuration from our facet or from project settings (where facets are not
 * available, e.g. PyCharm)
 */
fun getConfiguration(module: Module): HassFacetState? {
    // IDEs with facet support will use facet configuration
    // otherwise a project-wide configuration will be used
    return if (usesProjectSettings()) {
        ProjectSettings.getInstance(module.project).state
    } else {
        getFacetState(module)
    }
}

/**
 * Returns true if the given module has Home Assistant support. For IDEs not supporting facets we
 * can't but return true.
 */
fun isHomeAssistantModule(module: Module): Boolean = usesProjectSettings() || moduleHasFacet(module)
