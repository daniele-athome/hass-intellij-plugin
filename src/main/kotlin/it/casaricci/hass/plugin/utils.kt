package it.casaricci.hass.plugin

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.YAMLFileType

/**
 * Token prefix to be used in secret reference.
 */
const val HASS_TOKEN_SECRET = "!secret"

// TODO this should start from configuration.yaml and walk all includes (in order to filter out unwanted files)
fun isHassConfigFile(virtualFile: VirtualFile, project: Project): Boolean {
    if (virtualFile.fileType == YAMLFileType.YML) {
        return ModuleUtil.findModuleForFile(virtualFile, project)?.let { isHomeAssistantModule(it) } == true
    }
    return false
}

fun entityId(domainName: String, entityName: String): String = "$domainName.$entityName"
