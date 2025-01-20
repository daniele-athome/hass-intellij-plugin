package it.casaricci.hass.plugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.util.Urls
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.facet.HassFacetSettingsComponent
import it.casaricci.hass.plugin.services.HassRemoteRepository
import javax.swing.JComponent

class ProjectSettingsConfigurable(private val project: Project) : Configurable {

    private var settingsComponent: HassFacetSettingsComponent? = null

    override fun createComponent(): JComponent {
        settingsComponent = HassFacetSettingsComponent()
        return settingsComponent!!.panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return settingsComponent!!.preferredFocusedComponent
    }

    override fun isModified(): Boolean {
        val state = ProjectSettings.getInstance(project).state
        val other = settingsComponent!!
        return state.instanceUrl != other.instanceUrl ||
                state.token != other.token
    }

    override fun apply() {
        val other = settingsComponent!!

        val url = try {
            Urls.parse(other.instanceUrl, false)
        } catch (_: Exception) {
            null
        }

        if (url == null) {
            @Suppress("DialogTitleCapitalization")
            throw ConfigurationException(MyBundle.message("hass.facet.editor.instanceUrl.invalid"))
        }

        if (other.token.trim().isEmpty()) {
            throw ConfigurationException(MyBundle.message("hass.facet.editor.token.invalid"))
        }

        val state = ProjectSettings.getInstance(project).state
        state.instanceUrl = other.instanceUrl
        state.token = other.token

        // trigger download immediately
        val service = HassRemoteRepository.getInstance(project)
        // this mode of operations cannot have multiple modules (e.g. PyCharm)
        service.refreshCache(project.modules.first(), true)
    }

    override fun reset() {
        val state = ProjectSettings.getInstance(project).state
        val other = settingsComponent!!
        other.instanceUrl = state.instanceUrl
        other.token = state.token
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }

    override fun getDisplayName(): String = MyBundle.message("hass.facet.name")
}
