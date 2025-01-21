package it.casaricci.hass.plugin.psi

import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import it.casaricci.hass.plugin.HASS_TOKEN_SECRET
import it.casaricci.hass.plugin.splitEntityId
import org.jetbrains.yaml.psi.YAMLScalar

private const val PREFIX_SECRET = "$HASS_TOKEN_SECRET "

internal class HassReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            HassReferenceProvider()
        )
    }

    class HassReferenceProvider : PsiReferenceProvider() {

        override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
            val originalElement = CompletionUtil.getOriginalOrSelf(element)
            if (originalElement is YAMLScalar) {
                val text = originalElement.text

                if (text.startsWith(PREFIX_SECRET)) {
                    val range = TextRange.from(
                        PREFIX_SECRET.length,
                        text.length - PREFIX_SECRET.length
                    )
                    return arrayOf(
                        HassSecretReference(originalElement, range, range.substring(text))
                    )
                } else {
                    val (domainName, actionName) = splitEntityId(text)
                    if (domainName.isNotEmpty() && actionName.isNotEmpty()) {
                        val range = TextRange.from(domainName.length + 1, actionName.length)
                        return arrayOf(
                            HassEntityReference(originalElement, range, domainName, actionName)
                        )
                    }
                }
            }

            return PsiReference.EMPTY_ARRAY
        }
    }

}
