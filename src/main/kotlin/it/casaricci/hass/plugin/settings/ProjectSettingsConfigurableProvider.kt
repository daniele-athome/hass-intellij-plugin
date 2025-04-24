package it.casaricci.hass.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project
import it.casaricci.hass.plugin.internal.PlatformUtils

class ProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {
    override fun createConfigurable(): Configurable = ProjectSettingsConfigurable(project)

    override fun canCreateConfigurable(): Boolean = usesProjectSettings()
}

/**
 * Only IntelliJ IDEA supports facets, apparently. For all other IDEs we will use project settings.
 */
fun usesProjectSettings(): Boolean = !PlatformUtils.isIntelliJ()
