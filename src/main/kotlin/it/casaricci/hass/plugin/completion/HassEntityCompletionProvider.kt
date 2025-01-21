package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.module.ModuleUtil
import com.intellij.util.ProcessingContext
import it.casaricci.hass.plugin.entityId
import it.casaricci.hass.plugin.services.HassDataRepository
import it.casaricci.hass.plugin.services.getDomainNameFromSecondLevelElement
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.psi.YAMLKeyValue

class HassEntityCompletionProvider : CompletionProvider<CompletionParameters>() {

    public override fun addCompletions(
        @NotNull parameters: CompletionParameters,
        @NotNull context: ProcessingContext,
        @NotNull resultSet: CompletionResultSet
    ) {
        // no module, no party
        val module = ModuleUtil.findModuleForPsiElement(parameters.position) ?: return
        val service = HassDataRepository.getInstance(module.project)

        // we should maybe cache LookupElement objects, but we don't have a way to purge them when things change
        service.getEntities(module).forEach { entity ->
            let {
                when (entity) {
                    is YAMLKeyValue -> {
                        // local action (i.e. script)
                        val domainName = getDomainNameFromSecondLevelElement(entity)
                        domainName?.let {
                            entityId(domainName, entity.keyText)
                        }
                    }

                    is JsonStringLiteral -> {
                        // entity_id content
                        entity.value
                    }

                    else -> {
                        null
                    }
                }
            }?.let { actionName ->
                // TODO do we need to check for a prefix match? Doesn't addElement do that already?
                if (resultSet.prefixMatcher.prefixMatches(actionName)) {
                    resultSet.addElement(
                        LookupElementBuilder.create(actionName)
                            .withPsiElement(entity)
                            .withCaseSensitivity(true)
                            .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
                    )
                }
            }
        }
    }

}
