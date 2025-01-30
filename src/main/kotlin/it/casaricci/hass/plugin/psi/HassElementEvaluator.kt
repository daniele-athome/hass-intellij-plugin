package it.casaricci.hass.plugin.psi

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import it.casaricci.hass.plugin.HASS_AUTOMATION_NAME_PROPERTY
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.language.HassAutomation
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

class HassElementEvaluator : TargetElementEvaluatorEx2() {
    override fun getNamedElement(element: PsiElement): PsiElement? {
        thisLogger().trace("NAMED: $element")
        val maybeAutomation = getAutomationAlias(element)
        if (maybeAutomation?.parentOfType<YAMLSequenceItem>()
            ?.parentOfType<YAMLSequence>()
                ?.parentOfType<YAMLKeyValue>()?.keyText == HassKnownDomains.AUTOMATION) {

            // return the key-value element
            //return element.parent.parent
            return HassAutomation(element, maybeAutomation.valueText)
        }

        return super.getNamedElement(element)
    }

    private fun getAutomationAlias(element: PsiElement): YAMLKeyValue? {
        val keyValue = element.parentOfType<YAMLScalar>()
            ?.parentOfType<YAMLKeyValue>()
        return if (keyValue?.keyText == HASS_AUTOMATION_NAME_PROPERTY) {
            keyValue
        }
        else {
            null
        }
    }
}
