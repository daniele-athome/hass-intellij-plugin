package it.casaricci.hass.intellij.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import it.casaricci.hass.intellij.HASS_TOKEN_SECRET
import it.casaricci.hass.intellij.psi.YamlElementPatternHelper

internal class HassCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC, YamlElementPatternHelper.getSingleLineScalarKey("icon"), MdiIconCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getScalarValueWithTagPrefix(HASS_TOKEN_SECRET), SecretCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            YamlElementPatternHelper.getSingleLineScalarKey("action", "service"), HassActionCompletionProvider()
        )
        extend(
            CompletionType.BASIC,
            PlatformPatterns.or(
                YamlElementPatternHelper.getSingleLineScalarKey("entity_id", "entities"),
                YamlElementPatternHelper.getSingleLineScalarParentKey("entity_id", "entities")
            ), HassEntityCompletionProvider()
        )
    }
}
