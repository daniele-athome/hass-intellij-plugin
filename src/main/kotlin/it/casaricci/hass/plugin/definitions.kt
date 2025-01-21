/**
 * Home Assistant domain-specific definitions. Used throughout the plugin.
 */
package it.casaricci.hass.plugin

object HassKnownDomains {

    /**
     * Name of the script Home Assistant integration (i.e. root-level YAML key).
     */
    const val SCRIPT = "script"

    /**
     * Name of the automation Home Assistant integration (i.e. root-level YAML key).
     */
    const val AUTOMATION = "automation"

}

object HassKnownFilenames {

    /**
     * Name of the entry point configuration file.
     */
    const val CONFIGURATION = "configuration.yaml"

    /**
     * Name of the secrets file.
     */
    const val SECRETS = "secrets.yaml"
}

/**
 * Domains that uses this form and can be resolved to local files:
 *
 * ```yaml
 * input_number:
 *   helper_name:
 *     ...
 * ```
 */
val SECOND_LEVEL_KEY_IDENTIFIER_DOMAINS = hashSetOf(
    HassKnownDomains.SCRIPT,
    "input_number",
    "input_select",
    "input_text",
    "input_boolean",
    // TODO groups can also be defined via "platform: group" notation
    "group",
    "shell_command",
)

// TODO val SECOND_LEVEL_ATTRIBUTE_IDENTIFIER_DOMAINS = ...
//      for things like automations which are identified by an attribute (e.g. "alias" for automations)

/**
 * Token prefix to be used in secret reference.
 */
const val HASS_TOKEN_SECRET = "!secret"
