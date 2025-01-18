package it.casaricci.hass.intellij.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import it.casaricci.hass.intellij.HASS_KEY_SCRIPT
import it.casaricci.hass.intellij.MyBundle
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
        // TODO
        return null
    }

    override fun getType(element: PsiElement): String {
        if (isMyElement(element)) {
            return MyBundle.message("hass.findUsages.haScript")
        }
        return ""
    }

    override fun getDescriptiveName(element: PsiElement): String {
        if (isMyElement(element)) {
            return element.text
        }
        return ""
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        if (isMyElement(element)) {
            return element.text + ":"
        }
        return ""
    }

    private fun isMyElement(element: PsiElement): Boolean = element is YAMLKeyValue &&
            element.parent.parent is YAMLKeyValue &&
            (element.parent.parent as YAMLKeyValue).keyText == HASS_KEY_SCRIPT
}
