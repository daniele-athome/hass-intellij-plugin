package it.casaricci.hass.plugin.documentation

import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.ide.util.PlatformModuleRendererFactory
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.refactoring.suggested.createSmartPointer
import com.intellij.util.TextWithIcon
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.entityId
import it.casaricci.hass.plugin.splitEntityId
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

// TODO not ready yet, experimental code
class HassDocumentationProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element.language == YAMLLanguage.INSTANCE &&
            element.parent is YAMLScalar &&
            element.parent.parent is YAMLKeyValue &&
            ((element.parent.parent as YAMLKeyValue).keyText == "action" ||
                    (element.parent.parent as YAMLKeyValue).keyText == "service") &&
            splitEntityId(element.text).first == HassKnownDomains.SCRIPT
        ) {
            return HassDocumentationScriptTarget(element.parent, originalElement)
        }
        return null
    }

    @Suppress("UnstableApiUsage")
    internal class HassDocumentationScriptTarget(
        private val element: PsiElement,
        private val originalElement: PsiElement?
    ) : DocumentationTarget {

        override fun createPointer(): Pointer<out DocumentationTarget> {
            val elementPtr = element.createSmartPointer()
            val originalElementPtr = originalElement?.createSmartPointer()
            return Pointer {
                val element = elementPtr.dereference() ?: return@Pointer null
                HassDocumentationScriptTarget(element, originalElementPtr?.dereference())
            }
        }

        override fun computePresentation(): TargetPresentation {
            val moduleTextWithIcon: TextWithIcon? = let {
                val factory = ModuleRendererFactory.findInstance(element)
                if (factory !is PlatformModuleRendererFactory) {
                    factory.getModuleTextWithIcon(element)
                } else {
                    null
                }
            }

            return TargetPresentation
                .builder(element.text)
                .locationText(moduleTextWithIcon?.text, moduleTextWithIcon?.icon)
                .presentation()
        }

        override fun computeDocumentation(): DocumentationResult? {
            element.reference?.resolve()?.let {
                val script = it as YAMLKeyValue
                val description = script.childrenOfType<YAMLMapping>().firstOrNull()
                    ?.getKeyValueByKey("description")
                    ?.valueText
                if (description != null) {
                    val entityId = entityId(HassKnownDomains.SCRIPT, script.keyText)
                    return DocumentationResult
                        .documentation(
                            "<p><b>$entityId</b></p>" +
                                    "<p>$description</p>"
                        )
                }
            }
            return null
        }

        /**
         * Never called.
         */
        override fun computeDocumentationHint(): String? {
            return null
        }

    }
}
