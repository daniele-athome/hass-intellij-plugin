package it.casaricci.hass.plugin.findUsages

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.parentOfType
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.MyBundle
import org.jetbrains.yaml.YAMLElementTypes
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.lexer.YAMLFlexLexer
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLSequence
import org.jetbrains.yaml.psi.YAMLSequenceItem

class HassAutomationFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            YAMLFlexLexer(),
            TokenSet.create(
                YAMLTokenTypes.SCALAR_KEY,
                YAMLTokenTypes.SCALAR_TEXT,
                YAMLTokenTypes.TEXT,
            ),
            TokenSet.create(
                YAMLTokenTypes.COMMENT
            ),
            YAMLElementTypes.SCALAR_VALUES)
    }

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        thisLogger().trace("canFindUsageFor: $element")
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

    private fun isMyElement(element: PsiElement): Boolean {
        thisLogger().trace("element: $element")
        val parent = if (element is YAMLKeyValue) {
            thisLogger().trace("element.keyText: " + element.keyText)
            element.parentMapping
        } else {
            thisLogger().trace("element.text: " + element.text)
            element.parentOfType<YAMLKeyValue>()?.parentMapping
        }
        thisLogger().trace(
            "lookup parent.keyText: " + parent?.parentOfType<YAMLSequenceItem>()
                ?.parentOfType<YAMLSequence>()
                ?.parentOfType<YAMLKeyValue>()
                ?.keyText
        )
        return parent?.parentOfType<YAMLSequenceItem>()
            ?.parentOfType<YAMLSequence>()
            ?.parentOfType<YAMLKeyValue>()
            ?.keyText == HassKnownDomains.AUTOMATION
    }
}
