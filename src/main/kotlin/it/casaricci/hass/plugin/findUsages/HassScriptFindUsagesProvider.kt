package it.casaricci.hass.plugin.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.isScriptDefinition
import org.jetbrains.yaml.YAMLWordsScanner
import org.jetbrains.yaml.psi.YAMLKeyValue

class HassScriptFindUsagesProvider : FindUsagesProvider {

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
            return MyBundle.message("hass.findUsages.haScript")
        }
        return ""
    }

    // FIXME this is not used I think because element is never wrapped
    override fun getDescriptiveName(element: PsiElement): String {
        if (isMyElement(element)) {
            return element.text
        }
        return ""
    }

    // FIXME this is not used I think because element is never wrapped
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        if (isMyElement(element)) {
            return element.text + ":"
        }
        return ""
    }

    private fun isMyElement(element: PsiElement): Boolean = isScriptDefinition(element)
}
