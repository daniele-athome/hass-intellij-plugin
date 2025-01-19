package it.casaricci.hass.plugin.facet

import com.intellij.facet.ui.*
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.Urls
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.services.HassRemoteRepository
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

// TODO use HassFacetSettingsComponent
class HassFacetEditorTab(
    private val state: HassFacetState,
    private val context: FacetEditorContext,
    private val manager: FacetValidatorsManager
) : FacetEditorTab() {

    private val instanceUrlField = JTextField(state.instanceUrl)
    private val tokenField = JTextField(state.token)
    private val refreshButton = JButton(MyBundle.message("hass.facet.editor.refresh.text"), AllIcons.Actions.Refresh)

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return MyBundle.message("hass.facet.editor.title")
    }

    override fun createComponent(): JComponent {
        // TODO add a button for refreshing the cached JSONs (services, entities, etc.)
        val contentPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(
                MyBundle.message("hass.facet.editor.instanceUrl.title"),
                instanceUrlField
            )
            .addLabeledComponent(
                MyBundle.message("hass.facet.editor.token.title"),
                tokenField
            )
            .addComponent(refreshButton)
            .panel.apply {
                border = JBUI.Borders.empty(10)
            }

        manager.registerValidator(object : FacetEditorValidator() {
            override fun check(): ValidationResult {
                val url = try {
                    Urls.parse(instanceUrlField.text, false)
                } catch (_: Exception) {
                    null
                }
                return if (url != null) {
                    ValidationResult.OK
                } else {
                    ValidationResult(MyBundle.message("hass.facet.editor.instanceUrl.invalid"))
                }
            }
        }, instanceUrlField)

        manager.registerValidator(object : FacetEditorValidator() {
            override fun check(): ValidationResult {
                return if (tokenField.text.trim().isNotEmpty()) {
                    ValidationResult.OK
                } else {
                    ValidationResult(MyBundle.message("hass.facet.editor.token.invalid"))
                }
            }
        }, tokenField)

        refreshButton.addActionListener {
            apply()
        }

        return JPanel(BorderLayout()).apply {
            add(contentPanel, BorderLayout.NORTH)
        }
    }

    override fun isModified(): Boolean {
        return !StringUtil.equals(state.instanceUrl, instanceUrlField.text.trim()) ||
                !StringUtil.equals(state.token, tokenField.text.trim())
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        try {
            state.instanceUrl = instanceUrlField.text
            state.token = tokenField.text

            // trigger download immediately
            val service = context.project.getService(HassRemoteRepository::class.java)
            service.refreshCache(context.module, true)

        } catch (e: Exception) {
            throw ConfigurationException(e.toString())
        }
    }

    override fun reset() {
        instanceUrlField.text = state.instanceUrl
        tokenField.text = state.token
    }


}
