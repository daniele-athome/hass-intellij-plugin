package it.casaricci.hass.plugin.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.language.HassAutomation
import org.jetbrains.yaml.YAMLWordsScanner

class HassAutomationFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return YAMLWordsScanner()
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return isMyElement(element)
    }

    override fun getHelpId(element: PsiElement): String? {
        return null
    }

    override fun getType(element: PsiElement): String {
        if (isMyElement(element)) {
            return MyBundle.message("hass.findUsages.haAutomation")
        }
        return ""
    }

    override fun getDescriptiveName(element: PsiElement): String {
        if (isMyElement(element)) {
            return (element as PsiNamedElement).name ?: ""
        }
        return ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return getDescriptiveName(element)
    }

    private fun isMyElement(element: PsiElement): Boolean {
        return element is HassAutomation
    }
}
