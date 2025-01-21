package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.module.ModuleUtil
import com.intellij.util.ProcessingContext
import it.casaricci.hass.plugin.entityId
import it.casaricci.hass.plugin.services.HassDataRepository
import it.casaricci.hass.plugin.services.getDomainNameFromActionName
import it.casaricci.hass.plugin.services.getDomainNameFromSecondLevelElement
import org.jetbrains.annotations.NotNull
import org.jetbrains.yaml.psi.YAMLKeyValue

class HassActionCompletionProvider : CompletionProvider<CompletionParameters>() {

    public override fun addCompletions(
        @NotNull parameters: CompletionParameters,
        @NotNull context: ProcessingContext,
        @NotNull resultSet: CompletionResultSet
    ) {
        // no module, no party
        val module = ModuleUtil.findModuleForPsiElement(parameters.position) ?: return
        val service = HassDataRepository.getInstance(module.project)

        // we should maybe cache LookupElement objects, but we don't have a way to purge them when things change
        service.getActions(module).forEach { action ->
            let {
                when (action) {
                    is YAMLKeyValue -> {
                        // local action (i.e. script)
                        val domainName = getDomainNameFromSecondLevelElement(action)
                        domainName?.let {
                            entityId(domainName, action.keyText)
                        }
                    }

                    is JsonProperty -> {
                        val domainName = getDomainNameFromActionName(action)
                        domainName?.let {
                            entityId(domainName, action.name)
                        }
                    }

                    else -> {
                        null
                    }
                }
            }?.let { actionName ->
                resultSet.addElement(
                    LookupElementBuilder.create(actionName)
                        .withPsiElement(action)
                        .withCaseSensitivity(true)
                        .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
                )
            }
        }
    }

}
