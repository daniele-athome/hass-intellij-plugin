package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.ProcessingContext
import it.casaricci.hass.plugin.ICON_NAME_PREFIX
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.services.MdiIconsRepository
import org.jetbrains.annotations.NotNull

/**
 * Provides completions for mdi:icons.
 * [Unbearably slow](https://github.com/daniele-athome/hass-intellij-plugin/issues/7).
 */
class MdiIconCompletionProvider : CompletionProvider<CompletionParameters>(), DumbAware {

    public override fun addCompletions(
        @NotNull parameters: CompletionParameters,
        @NotNull context: ProcessingContext,
        @NotNull resultSet: CompletionResultSet
    ) {
        val cache = try {
            initCache()
        } catch (e: Exception) {
            notifyLoadIconsError(parameters.position.project, e.toString())
            return
        }

        // useless advertisement, links are not clickable
        // resultSet.addLookupAdvertisement("Icons available at https://materialdesignicons.com/")
        resultSet.addAllElements(cache)
    }

    companion object {
        // keep in memory all the elements
        private var lookupList: List<LookupElement>? = null

        private fun initCache(): List<LookupElement> {
            if (lookupList == null) {
                lookupList = MdiIconsRepository.loadIcons().map {
                    LookupElementBuilder.create(ICON_NAME_PREFIX + it.key)
                        .withIcon(MdiIconsRepository.getIcon(it.value))
                        .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
                }
            }
            return lookupList!!
        }
    }

    private fun notifyLoadIconsError(project: Project, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Home Assistant load icon error")
            .createNotification(
                MyBundle.message("hass.notification.loadIconsError.title"),
                message,
                NotificationType.ERROR
            )
            .notify(project)
    }

}
