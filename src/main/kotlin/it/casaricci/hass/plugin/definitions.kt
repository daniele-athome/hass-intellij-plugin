/**
 * Home Assistant domain-specific definitions. Used throughout the plugin.
 */
package it.casaricci.hass.plugin

/**
 * Name of the secrets file.
 */
const val SECRETS_FILENAME = "secrets.yaml"

/**
 * Name of the script Home Assistant integration (i.e. root-level YAML key).
 */
const val HASS_DOMAIN_SCRIPT = "script"

/**
 * Name of the automation Home Assistant integration (i.e. root-level YAML key).
 */
const val HASS_DOMAIN_AUTOMATION = "automation"

/**
 * Domains that uses this form and can be resolved to local files:
 *
 * ```yaml
 * input_number:
 *   helper_name:
 *     ...
 * ```
 */
val KEY_NAME_DOMAINS = hashSetOf(
    HASS_DOMAIN_SCRIPT,
    "input_number",
    "input_select",
    "input_text",
    "input_boolean",
    // TODO groups can also be defined via "platform: group" notation
    "group",
    "shell_command",
)
