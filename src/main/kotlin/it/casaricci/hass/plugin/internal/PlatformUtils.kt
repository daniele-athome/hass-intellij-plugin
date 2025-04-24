package it.casaricci.hass.plugin.internal

/** Adapted from IntelliJ SDK because it's an internal API. */
object PlatformUtils {
    private const val PLATFORM_PREFIX_KEY: String = "idea.platform.prefix"
    private const val IDEA_PREFIX: String = "idea"

    private fun getPlatformPrefix(): String = getPlatformPrefix(IDEA_PREFIX)

    private fun getPlatformPrefix(defaultPrefix: String?): String =
        System.getProperty(PLATFORM_PREFIX_KEY, defaultPrefix)

    private fun `is`(idePrefix: String): Boolean = idePrefix == getPlatformPrefix()

    private fun isIdeaUltimate(): Boolean = `is`("idea")

    private fun isIdeaCommunity(): Boolean = `is`("Idea")

    fun isIntelliJ(): Boolean = isIdeaUltimate() || isIdeaCommunity() || `is`("IdeaEdu")
}
