package it.casaricci.hass.plugin.facet

import com.intellij.facet.ui.*
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Urls
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.services.HassRemoteRepository
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class HassFacetEditorTab(
    private val state: HassFacetState,
    private val context: FacetEditorContext,
    private val manager: FacetValidatorsManager
) : FacetEditorTab() {

    private lateinit var component: HassFacetSettingsComponent

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return MyBundle.message("hass.facet.editor.title")
    }

    override fun createComponent(): JComponent {
        component = HassFacetSettingsComponent().apply {
            instanceUrl = state.instanceUrl
            token = state.token
        }

        manager.registerValidator(object : FacetEditorValidator() {
            override fun check(): ValidationResult {
                val url = try {
                    Urls.parse(component.instanceUrl, false)
                } catch (_: Exception) {
                    null
                }
                return if (url != null) {
                    ValidationResult.OK
                } else {
                    ValidationResult(MyBundle.message("hass.facet.editor.instanceUrl.invalid"))
                }
            }
        }, component.instanceUrlField)

        manager.registerValidator(object : FacetEditorValidator() {
            override fun check(): ValidationResult {
                return if (component.token.trim().isNotEmpty()) {
                    ValidationResult.OK
                } else {
                    ValidationResult(MyBundle.message("hass.facet.editor.token.invalid"))
                }
            }
        }, component.tokenField)

        component.setRefreshButtonListener {
            apply()
        }

        return component.panel
    }

    override fun isModified(): Boolean {
        return !StringUtil.equals(state.instanceUrl, component.instanceUrl.trim()) ||
                !StringUtil.equals(state.token, component.token.trim())
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        try {
            state.instanceUrl = component.instanceUrl
            state.token = component.token

            // trigger download immediately
            val service = context.project.getService(HassRemoteRepository::class.java)
            service.refreshCache(context.module, true)

        } catch (e: Exception) {
            throw ConfigurationException(e.toString())
        }
    }

    override fun reset() {
        component.instanceUrl = state.instanceUrl
        component.token = state.token
    }

}
