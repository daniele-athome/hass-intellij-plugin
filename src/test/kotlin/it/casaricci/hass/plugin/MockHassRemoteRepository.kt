package it.casaricci.hass.plugin

import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.util.ResourceUtil
import it.casaricci.hass.plugin.services.HassRemoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.FileInputStream

class MockHassRemoteRepository(project: Project, jsonStatesPath: String?, jsonServicePath: String?) :
    HassRemoteRepository(project, CoroutineScope(Dispatchers.IO)) {

    private var states: Collection<JsonStringLiteral>? = null
    private var services: Collection<JsonProperty>? = null

    init {
        if (jsonStatesPath != null) {
            val jsonStatesData = ResourceUtil.loadText(FileInputStream(jsonStatesPath))
            val jsonStatesFile = PsiFileFactory.getInstance(project)
                .createFileFromText("ha_states.json", JsonFileType.INSTANCE, jsonStatesData) as JsonFile

            states = getStates(jsonStatesFile)
        }

        if (jsonServicePath != null) {
            val jsonServicesData = ResourceUtil.loadText(FileInputStream(jsonServicePath))
            val jsonServicesFile = PsiFileFactory.getInstance(project)
                .createFileFromText("ha_services.json", JsonFileType.INSTANCE, jsonServicesData) as JsonFile

            services = getServices(jsonServicesFile)
        }
    }

    override fun getStates(module: Module, vararg excludeDomains: String): Collection<JsonStringLiteral>? {
        return states
    }

    override fun getServices(module: Module): Collection<JsonProperty>? {
        return services
    }

}
