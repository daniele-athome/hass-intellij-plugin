package it.casaricci.hass.plugin.documentation

import com.intellij.ide.util.ModuleRendererFactory
import com.intellij.ide.util.PlatformModuleRendererFactory
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import com.intellij.psi.util.childrenOfType
import com.intellij.util.TextWithIcon
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.entityId
import it.casaricci.hass.plugin.isActionCall
import it.casaricci.hass.plugin.isHassConfigFile
import it.casaricci.hass.plugin.services.getDomainNameFromActionName
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

// TODO not ready yet, experimental code
class HassDocumentationProvider : PsiDocumentationTargetProvider {

    /**
     * This part of the API is not really well documented yet - I guess because it's relatively new.
     * This method is called in various situations and I didn't really knew how to detect each situation but by
     * reverse-engineering the method arguments. The current code seems to handle all (2+1) situations well - for now.
     */
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        // we still don't know when originalElement could be null, better safe than sorry
        if (originalElement == null) {
            return null
        }

        // documentation request coming from a Home Assistant YAML file
        if (isHassConfigFile(originalElement)) {
            // documentation request coming from an action call
            if (originalElement.parent is YAMLScalar && isActionCall(originalElement.parent as YAMLScalar)) {
                val resolveReference: Boolean

                // documentation request from a code completion popup
                val documentedElement = if (element is JsonProperty || element is YAMLKeyValue) {
                    resolveReference = false
                    element
                }
                // documentation request directly to the action call
                else if (element.parent is YAMLScalar) {
                    resolveReference = true
                    element.parent
                }
                // unhandled case?
                else {
                    resolveReference = false
                    null
                }

                if (documentedElement != null) {
                    return HassDocumentationActionTarget(documentedElement, originalElement, resolveReference)
                }
            }
        }

        return null
    }

    @Suppress("UnstableApiUsage")
    internal class HassDocumentationActionTarget(
        private val element: PsiElement,
        private val originalElement: PsiElement?,
        private val resolveReference: Boolean
    ) : DocumentationTarget {

        override fun createPointer(): Pointer<out DocumentationTarget> {
            val elementPtr = element.createSmartPointer()
            val originalElementPtr = originalElement?.createSmartPointer()
            return Pointer {
                val element = elementPtr.dereference() ?: return@Pointer null
                HassDocumentationActionTarget(element, originalElementPtr?.dereference(), resolveReference)
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
            val resolvedElement = if (resolveReference) {
                element.reference?.resolve()
            } else {
                element
            }

            return resolvedElement?.let { action ->
                when (action) {
                    // local script
                    is YAMLKeyValue -> {
                        val description = action.childrenOfType<YAMLMapping>().firstOrNull()
                            ?.getKeyValueByKey("description")
                            ?.valueText
                        if (description != null) {
                            val entityId = entityId(HassKnownDomains.SCRIPT, action.keyText)
                            return documentationForEntity(entityId, description)
                        }
                        return null
                    }

                    // remote action
                    is JsonProperty -> {
                        return (action.value as? JsonObject)?.let { properties ->
                            return (properties.findProperty("description")?.value as? JsonStringLiteral)
                                ?.value?.let { description ->
                                    return getDomainNameFromActionName(action)?.let { domainName ->
                                        val entityId = entityId(domainName, action.name)
                                        return documentationForEntity(entityId, description)
                                    }
                                }
                        }
                    }

                    else -> {
                        return null
                    }
                }
            }
        }

        private fun documentationForEntity(entityId: String, description: String): DocumentationResult =
            DocumentationResult
                .documentation(
                    "<p><b>$entityId</b></p>" +
                            "<p>$description</p>"
                )

        /**
         * Never called.
         */
        override fun computeDocumentationHint(): String? {
            return null
        }

    }
}
