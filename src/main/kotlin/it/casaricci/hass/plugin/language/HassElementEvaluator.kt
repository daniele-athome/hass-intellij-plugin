package it.casaricci.hass.plugin.language

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.parentOfType
import it.casaricci.hass.plugin.isAutomation
import org.jetbrains.yaml.psi.YAMLScalar

class HassElementEvaluator : TargetElementEvaluatorEx2() {
    /**
     * This method will be called for elements that don't inherit from
     * [com.intellij.psi.PsiNamedElement].
     *
     * @return an actual [com.intellij.psi.PsiNamedElement] that can be named and used as a
     *   reference
     */
    override fun getNamedElement(element: PsiElement): PsiElement? {
        // handle text values (e.g. property values)
        val textElement = element.parentOfType<YAMLScalar>()
        if (textElement != null) {
            return wrapElement(textElement)
        }

        return null
    }

    /**
     * This method will be called for practically every element selected by the user, so it needs to
     * be very fast.
     *
     * @return an element wrapped with a custom class that we can recognize (if available, otherwise
     *   the same element will be returned)
     */
    override fun adjustReferenceOrReferencedElement(
        file: PsiFile,
        editor: Editor,
        offset: Int,
        flags: Int,
        refElement: PsiElement?,
    ): PsiElement? {
        val wrappedElement =
            when (refElement) {
                is YAMLScalar -> {
                    wrapElement(refElement)
                }

                else -> {
                    null
                }
            }

        return wrappedElement
            ?: super.adjustReferenceOrReferencedElement(file, editor, offset, flags, refElement)
    }

    private fun wrapElement(element: YAMLScalar): PsiNamedElement? {
        if (isAutomation(element)) {
            return HassAutomation(element)
        }
        return null
    }
}
