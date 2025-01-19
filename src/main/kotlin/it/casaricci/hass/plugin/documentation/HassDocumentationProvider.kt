package it.casaricci.hass.plugin.documentation

import com.intellij.codeInsight.navigation.fileStatusAttributes
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.model.Pointer
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.suggested.createSmartPointer
import it.casaricci.hass.plugin.HASS_KEY_SCRIPT
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue

class HassDocumentationProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element.language == YAMLLanguage.INSTANCE &&
            element.parent.parent is YAMLKeyValue &&
            (element.parent.parent as YAMLKeyValue).keyText == HASS_KEY_SCRIPT
        ) {
            return HassDocumentationScriptTarget(element, originalElement)
        }
        return null
    }

    @Suppress("UnstableApiUsage")
    internal class HassDocumentationScriptTarget(val element: PsiElement, private val originalElement: PsiElement?): DocumentationTarget {

        override fun createPointer(): Pointer<out DocumentationTarget> {
            val elementPtr = element.createSmartPointer()
            val originalElementPtr = originalElement?.createSmartPointer()
            return Pointer {
                val element = elementPtr.dereference() ?: return@Pointer null
                HassDocumentationScriptTarget(element, originalElementPtr?.dereference())
            }
        }

        override fun computePresentation(): TargetPresentation {
            val project = element.project
            val file = element.containingFile?.virtualFile
            val itemPresentation = (element as? NavigationItem)?.presentation
            val presentableText: String = itemPresentation?.presentableText
                ?: (element as? PsiNamedElement)?.name
                ?: element.text
                ?: run {
                    //presentationError(element)
                    element.toString()
                }
            val moduleTextWithIcon = PsiElementListCellRenderer.getModuleTextWithIcon(element)
            return TargetPresentation
                .builder(presentableText)
                .backgroundColor(file?.let { VfsPresentationUtil.getFileBackgroundColor(project, file) })
                .icon(element.getIcon(Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS))
                //.presentableTextAttributes(itemPresentation?.getColoredAttributes())
                //.containerText(itemPresentation?.getContainerText(), file?.let { fileStatusAttributes(project, file) })
                .containerText("CONTAINER TEXT", file?.let { fileStatusAttributes(project, file) })
                .locationText(moduleTextWithIcon?.text, moduleTextWithIcon?.icon)
                .presentation()
        }

        override fun computeDocumentation(): DocumentationResult? {
            return DocumentationResult.documentation("<b>TEST</b>")
        }

        override fun computeDocumentationHint(): String? {
            return "<b>HINT</b>"
        }


    }
}
