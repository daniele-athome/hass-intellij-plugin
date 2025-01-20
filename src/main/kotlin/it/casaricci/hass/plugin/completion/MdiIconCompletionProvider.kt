package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.util.ProcessingContext
import it.casaricci.hass.plugin.services.MdiIconsRepository
import org.jetbrains.annotations.NotNull

// FIXME this is unbearably slow
class MdiIconCompletionProvider : CompletionProvider<CompletionParameters>(), DumbAware {

    public override fun addCompletions(
        @NotNull parameters: CompletionParameters,
        @NotNull context: ProcessingContext,
        @NotNull resultSet: CompletionResultSet
    ) {
        val cache = initCache()

        // FIXME replacing with tab replaces only "mdi:" and not the whole string
        // useless advertisement, links are not clickable
        // resultSet.addLookupAdvertisement("Icons available at https://materialdesignicons.com/")
        resultSet.addAllElements(cache.filter {
            resultSet.prefixMatcher.prefixMatches(it)
        })
    }

    companion object {
        private const val ICON_NAME_PREFIX = "mdi:"

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

}
