package it.casaricci.hass.intellij.facet

import com.intellij.facet.Facet
import com.intellij.facet.FacetType
import com.intellij.openapi.module.Module

class HassFacet(
    facetType: FacetType<out Facet<*>, *>,
    module: Module,
    name: String,
    configuration: HassFacetConfiguration,
    underlyingFacet: Facet<*>?
) : Facet<HassFacetConfiguration>(facetType, module, name, configuration, underlyingFacet)
