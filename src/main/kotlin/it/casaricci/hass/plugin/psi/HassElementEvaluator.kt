package it.casaricci.hass.plugin.psi

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
import it.casaricci.hass.plugin.HASS_AUTOMATION_NAME_PROPERTY
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.language.HassAutomation
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

// TODO refactor this a little maybe and avoid expensive operations when possible
class HassElementEvaluator : TargetElementEvaluatorEx2() {
    override fun getNamedElement(element: PsiElement): PsiElement? {
        thisLogger().trace("NAMED: $element")
        val automation: YAMLScalar? = getAutomationInfoLeaf(element)
        if (automation != null) {
            return HassAutomation(automation)
        }

        return super.getNamedElement(element)
    }

    override fun adjustReferenceOrReferencedElement(
        file: PsiFile,
        editor: Editor,
        offset: Int,
        flags: Int,
        refElement: PsiElement?
    ): PsiElement? {
        thisLogger().trace("ADJUST-REFERENCED-ELEM: $refElement (flags=$flags)")
        if (refElement is YAMLScalar) {
            val automation = getAutomationInfo(refElement)
            if (automation != null) {
                return HassAutomation(refElement)
            }
        }
        return super.adjustReferenceOrReferencedElement(file, editor, offset, flags, refElement)
    }

    private fun getAutomationInfoLeaf(element: PsiElement): YAMLScalar? {
        return getAutomationInfo(element.parentOfType<YAMLScalar>())
    }

    private fun getAutomationInfo(element: YAMLScalar?): YAMLScalar? {
        val keyValue = element?.parentOfType<YAMLKeyValue>()
        if (keyValue?.keyText == HASS_AUTOMATION_NAME_PROPERTY &&
            keyValue.parentOfType<YAMLSequenceItem>()
                ?.parentOfType<YAMLSequence>()
                ?.parentOfType<YAMLKeyValue>()?.keyText == HassKnownDomains.AUTOMATION
        ) {
            return element
        }

        return null
    }
}
