package it.casaricci.hass.plugin.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import it.casaricci.hass.plugin.HassKnownFilenames
import it.casaricci.hass.plugin.MyBundle
import org.jetbrains.yaml.YAMLWordsScanner
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue

class HassSecretFindUsagesProvider : FindUsagesProvider {
    override fun getWordsScanner(): WordsScanner = YAMLWordsScanner()

    override fun canFindUsagesFor(element: PsiElement): Boolean = isMyElement(element)

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        if (isMyElement(element)) {
            return MyBundle.message("hass.findUsages.haSecret")
        }
        return ""
    }

    // called but result is not used.
    override fun getDescriptiveName(element: PsiElement): String {
        if (isMyElement(element)) {
            return element.text
        }
        return ""
    }

    // not called ever.
    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        if (isMyElement(element)) {
            return element.text + ":"
        }
        return ""
    }

    private fun isMyElement(element: PsiElement): Boolean =
        element is YAMLKeyValue &&
            element.parentMapping?.parent is YAMLDocument &&
            element.containingFile.name == HassKnownFilenames.SECRETS
}
