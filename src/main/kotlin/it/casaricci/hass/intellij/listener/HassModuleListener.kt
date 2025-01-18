package it.casaricci.hass.intellij.listener

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import it.casaricci.hass.intellij.facet.getFacetState
import it.casaricci.hass.intellij.services.HassRemoteRepository

class HassModuleListener(private val project: Project) : ModuleListener {

    override fun modulesAdded(project: Project, modules: MutableList<out Module>) {
        val service = project.getService(HassRemoteRepository::class.java)
        modules.forEach { module ->
            val config = getFacetState(module)
            if (config != null) {
                // we have a Home Assistant module, refresh cache if needed
                service.refreshCache(module)
            }
        }
    }
}
