package it.casaricci.hass.plugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import it.casaricci.hass.plugin.HASS_TOKEN_SECRET
import it.casaricci.hass.plugin.psi.YamlElementPatternHelper
import org.jetbrains.yaml.psi.YAMLScalar

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

    /**
     * In theory this is useful only for token-based completions (for now only icons), because other contributors
     * use [PsiElement] objects: in those cases, the IDE will know how to replace the whole identifier.
     */
    override fun beforeCompletion(context: CompletionInitializationContext) {
        val position = context.file.findElementAt(context.startOffset) ?: return
        val scalarElement = PsiTreeUtil.getParentOfType(position, YAMLScalar::class.java)

        if (scalarElement != null) {
            val endPos = scalarElement.textOffset + scalarElement.textLength
            context.offsetMap.addOffset(CompletionInitializationContext.IDENTIFIER_END_OFFSET, endPos)
        }
    }
}
