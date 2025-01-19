package it.casaricci.hass.plugin

/**
 * Name of the script Home Assistant integration (i.e. root-level key).
 */
const val HASS_KEY_SCRIPT = "script"

/**
 * Name of the automation Home Assistant integration (i.e. root-level key).
 */
const val HASS_KEY_AUTOMATION = "automation"

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
    HASS_KEY_SCRIPT,
    "input_number",
    "input_select",
    "input_text",
    "input_boolean",
    // TODO groups can also be defined via "platform: group" notation
    "group",
    "shell_command",
)
