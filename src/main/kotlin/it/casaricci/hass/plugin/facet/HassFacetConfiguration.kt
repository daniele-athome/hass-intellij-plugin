package it.casaricci.hass.plugin.facet

import com.intellij.facet.FacetConfiguration
import com.intellij.facet.ui.FacetEditorContext
import com.intellij.facet.ui.FacetEditorTab
import com.intellij.facet.ui.FacetValidatorsManager
import com.intellij.openapi.components.PersistentStateComponent


class HassFacetConfiguration : FacetConfiguration, PersistentStateComponent<HassFacetState> {

    private var state = HassFacetState("", "")

    override fun getState(): HassFacetState = state

    override fun loadState(state: HassFacetState) {
        this.state = state
    }

    override fun createEditorTabs(
        context: FacetEditorContext,
        manager: FacetValidatorsManager
    ): Array<FacetEditorTab> {
        return arrayOf(
            HassFacetEditorTab(state, context, manager)
        )
    }

}
