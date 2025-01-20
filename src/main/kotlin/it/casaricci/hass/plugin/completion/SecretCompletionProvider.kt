package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtil
import com.intellij.util.ProcessingContext
import it.casaricci.hass.plugin.services.HassDataRepository
import org.jetbrains.annotations.NotNull

class SecretCompletionProvider : CompletionProvider<CompletionParameters>() {

    public override fun addCompletions(
        @NotNull parameters: CompletionParameters,
        @NotNull context: ProcessingContext,
        @NotNull resultSet: CompletionResultSet
    ) {
        // no module, no party
        val module = ModuleUtil.findModuleForPsiElement(parameters.position) ?: return
        val service = HassDataRepository.getInstance(module.project)

        resultSet.addAllElements(service.getSecrets(module).map {
            LookupElementBuilder.create(it)
                .withCaseSensitivity(true)
                .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE)
        })
    }

}
