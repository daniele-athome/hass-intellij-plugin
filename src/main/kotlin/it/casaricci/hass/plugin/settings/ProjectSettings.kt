package it.casaricci.hass.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import it.casaricci.hass.plugin.facet.HassFacetState

@Service(Service.Level.PROJECT)
@State(
    name = "org.intellij.sdk.settings.AppSettings",
    storages = [Storage("homeAssistant.xml")]
)
class ProjectSettings(val project: Project) : PersistentStateComponent<HassFacetState> {

    private var myState = HassFacetState()

    override fun getState(): HassFacetState {
        return myState
    }

    override fun loadState(state: HassFacetState) {
        myState = state
    }

    companion object {

        fun getInstance(project: Project): ProjectSettings {
            return project.getService(ProjectSettings::class.java)
        }
    }

}
