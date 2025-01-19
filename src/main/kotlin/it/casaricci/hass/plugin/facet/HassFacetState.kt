package it.casaricci.hass.plugin.facet

/**
 * Home Assistant facet state.
 */
data class HassFacetState(
    /**
     * URL to the Home Assistant instance.
     */
    var instanceUrl: String = "",

    /**
     * Access token for authentication.
     */
    var token: String = ""
)
