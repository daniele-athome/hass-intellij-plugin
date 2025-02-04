package it.casaricci.hass.plugin.language

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.yaml.YAMLBundle
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.impl.YAMLKeyValueImpl

/**
 * Wraps the "alias" key value element of an automation, giving the element [PsiNamedElement] title.
 */
class HassAutomation(private val element: YAMLScalar) : PsiNamedElement, YAMLScalar by element {

    /**
     * Automation name is actually the value of the "alias" key.
     */
    override fun getName(): String {
        return element.textValue
    }

    /**
     * Copied from [YAMLKeyValueImpl] + [org.jetbrains.yaml.YAMLUtil], although it seems like overkill.
     */
    override fun setName(name: String): PsiElement {
        if (name == element.textValue) {
            throw IncorrectOperationException(YAMLBundle.message("rename.same.name"))
        }

        val elementGenerator = YAMLElementGenerator.getInstance(element.project)

        val tempFile: PsiFile = elementGenerator.createDummyYamlWithText(name)
        val textElement = PsiTreeUtil.collectElementsOfType(tempFile, YAMLScalar::class.java).iterator().next()

        element.replace(textElement)
        return this
    }
}
