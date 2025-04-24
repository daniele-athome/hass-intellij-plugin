package it.casaricci.hass.plugin.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.isScriptDefinition
import org.jetbrains.yaml.YAMLWordsScanner

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

    /** Called but return value is not used probably because YAML plugin takes precedence. */
    override fun getDescriptiveName(element: PsiElement): String {
        // since this is not used (for now), avoid losing time doing useless stuff
        /*if (isMyElement(element)) {
            return element.text
        }*/
        return ""
    }

    /** Actually never called because YAML plugin takes precedence. */
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        // since this is not used (for now), avoid losing time doing useless stuff
        /*if (isMyElement(element)) {
            return element.text + ":"
        }*/
        return ""
    }

    private fun isMyElement(element: PsiElement): Boolean = isScriptDefinition(element)
}
