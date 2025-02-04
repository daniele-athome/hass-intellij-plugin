package it.casaricci.hass.plugin.schema

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory
import com.jetbrains.jsonSchema.extension.SchemaType
import com.jetbrains.jsonSchema.impl.JsonSchemaVersion
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.isHassConfigFile
import org.jetbrains.annotations.Nls

private const val HASS_SCHEMA_PATH = "/schemas/configuration.json"

class HassJsonSchemaProviderFactory : JsonSchemaProviderFactory, DumbAware {

    override fun getProviders(project: Project): List<JsonSchemaFileProvider> {
        return listOf(HassJsonSchemaProvider(project))
    }

    private class HassJsonSchemaProvider(private val project: Project) : JsonSchemaFileProvider {
        override fun isAvailable(file: VirtualFile): Boolean = isHassConfigFile(file, project)

        override fun getName(): @Nls String = MyBundle.message("hass.json.schema.name")

        override fun getSchemaFile(): VirtualFile? =
            javaClass.getResource(HASS_SCHEMA_PATH)?.let(VfsUtil::findFileByURL)

        override fun getSchemaType(): SchemaType = SchemaType.embeddedSchema

        override fun getSchemaVersion(): JsonSchemaVersion = JsonSchemaVersion.SCHEMA_7
    }
}
