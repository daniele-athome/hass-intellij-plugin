package it.casaricci.hass.intellij.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.module.ModuleUtil
import com.intellij.util.ProcessingContext
import it.casaricci.hass.intellij.entityId
import it.casaricci.hass.intellij.services.HassDataRepository
import it.casaricci.hass.intellij.services.getDomainNameFromActionName
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
        val service = module.project.getService(HassDataRepository::class.java)

        // we should maybe cache LookupElement objects, but we don't have a way to purge them when things change
        // FIXME potentially blocking (reads from file system);
        //       anyway we should request data from network when not available and put a modal
        service.getActions(module).forEach { action ->
            let {
                when (action) {
                    is YAMLKeyValue -> {
                        // local action (i.e. script)
                        val domainName = getDomainNameFromActionName(action)
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
                // TODO do we need to check for a prefix match? Doesn't addElement do that already?
                if (resultSet.prefixMatcher.prefixMatches(actionName)) {
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

}
