package it.casaricci.hass.plugin.language

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.yaml.psi.impl.YAMLPsiElementImpl

class HassAutomation(element: PsiElement, private val automationName: String)
    : YAMLPsiElementImpl(element.node), PsiNamedElement {

    override fun getName(): String {
        return automationName
    }

    override fun setName(name: String): PsiElement {
        TODO("Not yet implemented")
    }
}
