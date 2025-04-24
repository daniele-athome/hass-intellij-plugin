package it.casaricci.hass.plugin.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetManager
import com.intellij.facet.FacetType
import com.intellij.facet.FacetTypeId
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.MyIcons
import javax.swing.Icon

private const val FACET_ID = "homeassistant"
private val FACET_TYPE_ID = FacetTypeId<HassFacet>(FACET_ID)

class HassFacetType :
    FacetType<HassFacet, HassFacetConfiguration>(
        FACET_TYPE_ID,
        FACET_ID,
        MyBundle.message("hass.facet.name"),
    ) {
    override fun createDefaultConfiguration(): HassFacetConfiguration = HassFacetConfiguration()

    override fun isSuitableModuleType(moduleType: ModuleType<*>?): Boolean = true

    override fun createFacet(
        module: Module,
        name: String,
        configuration: HassFacetConfiguration,
        underlyingFacet: Facet<*>?,
    ): HassFacet = HassFacet(this, module, name, configuration, underlyingFacet)

    override fun getIcon(): Icon = MyIcons.Facet
}

fun moduleHasFacet(module: Module): Boolean =
    FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID) != null

fun getFacetState(module: Module): HassFacetState? =
    FacetManager.getInstance(module).getFacetByType(FACET_TYPE_ID)?.configuration?.state
