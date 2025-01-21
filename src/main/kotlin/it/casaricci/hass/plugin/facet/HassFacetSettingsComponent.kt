package it.casaricci.hass.plugin.facet

import com.intellij.icons.AllIcons
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import it.casaricci.hass.plugin.MyBundle
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField

class HassFacetSettingsComponent {

    /** We need public access for the facet editor tab. */
    val instanceUrlField = JTextField()

    /** We need public access for the facet editor tab. */
    val tokenField = JTextField()

    private val refreshButton = JButton(MyBundle.message("hass.facet.editor.refresh.text"), AllIcons.Actions.Refresh)

    val panel: JPanel
    val preferredFocusedComponent = instanceUrlField

    init {
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

        panel = JPanel(BorderLayout()).apply {
            add(contentPanel, BorderLayout.NORTH)
        }
    }

    fun setRefreshButtonListener(listener: () -> Unit) {
        refreshButton.addActionListener {
            listener()
        }
    }

    var instanceUrl: String
        get() = instanceUrlField.text
        set(value) {
            instanceUrlField.text = value
        }

    var token: String
        get() = tokenField.text
        set(value) {
            tokenField.text = value
        }

}
