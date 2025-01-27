package it.casaricci.hass.plugin.internal

/**
 * Adapted from IntelliJ SDK because it's an internal API.
 */
object PlatformUtils {
    private const val PLATFORM_PREFIX_KEY: String = "idea.platform.prefix"
    private const val IDEA_PREFIX: String = "idea"

    private fun getPlatformPrefix(): String {
        return getPlatformPrefix(IDEA_PREFIX)
    }

    private fun getPlatformPrefix(defaultPrefix: String?): String {
        return System.getProperty(PLATFORM_PREFIX_KEY, defaultPrefix)
    }

    private fun `is`(idePrefix: String): Boolean {
        return idePrefix == getPlatformPrefix()
    }

    private fun isIdeaUltimate(): Boolean {
        return `is`("idea")
    }

    private fun isIdeaCommunity(): Boolean {
        return `is`("Idea")
    }

    fun isIntelliJ(): Boolean {
        return isIdeaUltimate() || isIdeaCommunity() || `is`("IdeaEdu")
    }

}
