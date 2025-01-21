package it.casaricci.hass.plugin

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

// TODO this should start from configuration.yaml and walk all includes (in order to filter out unwanted files)
fun isHassConfigFile(virtualFile: VirtualFile, project: Project): Boolean {
    if (virtualFile.fileType == YAMLFileType.YML) {
        return ModuleUtil.findModuleForFile(virtualFile, project)?.let { isHomeAssistantModule(it) } == true
    }
    return false
}

fun entityId(domainName: String, entityName: String): String = "$domainName.$entityName"

private val ENTITY_ID_REGEX = Regex("^([A-Za-z0-9_]*)\\.([A-Za-z0-9_]*)$")

fun splitEntityId(entityId: String): Pair<String, String> {
    val match = ENTITY_ID_REGEX.matchEntire(entityId)
    return if (match != null && match.groupValues.size == 3) {
        Pair(match.groupValues[1], match.groupValues[2])
    } else {
        Pair("", "")
    }
}

fun isActionCall(element: YAMLScalar): Boolean {
    return element.parent is YAMLKeyValue &&
            ((element.parent as YAMLKeyValue).keyText == "action" ||
                    (element.parent as YAMLKeyValue).keyText == "service")
}
