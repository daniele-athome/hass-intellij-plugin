package it.casaricci.hass.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.project.Project
import com.intellij.util.PlatformUtils

class ProjectSettingsConfigurableProvider(private val project: Project) : ConfigurableProvider() {

    override fun createConfigurable(): Configurable {
        return ProjectSettingsConfigurable(project)
    }

    override fun canCreateConfigurable(): Boolean {
        return usesProjectSettings()
    }

}

// TODO check for other IDEs not supporting facets
@Suppress("UnstableApiUsage")
fun usesProjectSettings(): Boolean = PlatformUtils.isPyCharm()
