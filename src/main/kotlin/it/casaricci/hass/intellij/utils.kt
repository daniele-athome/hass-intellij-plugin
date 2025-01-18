package it.casaricci.hass.intellij

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import it.casaricci.hass.intellij.facet.moduleHasFacet

/**
 * Token prefix to be used in secret reference.
 */
const val HASS_TOKEN_SECRET = "!secret"

// TODO this should start from configuration.yaml and walk all includes (in order to filter out unwanted files)
fun isHassConfigFile(virtualFile: VirtualFile, project: Project): Boolean {
    val fileName = virtualFile.name
    if (virtualFile.isFile
        && (FileUtilRt.extensionEquals(fileName, "yml") || FileUtilRt.extensionEquals(fileName, "yaml"))
    ) {
        return ModuleUtil.findModuleForFile(virtualFile, project)?.let { moduleHasFacet(it) } == true
    }
    return false
}

fun entityId(domainName: String, entityName: String): String = "$domainName.$entityName"
