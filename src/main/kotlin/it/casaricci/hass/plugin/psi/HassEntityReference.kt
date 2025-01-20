package it.casaricci.hass.plugin.psi

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.KEY_NAME_DOMAINS
import it.casaricci.hass.plugin.entityId
import it.casaricci.hass.plugin.services.HassDataRepository
import it.casaricci.hass.plugin.services.HassRemoteRepository
import it.casaricci.hass.plugin.services.getDomainNameFromActionName
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

class HassEntityReference(
    element: YAMLScalar,
    range: TextRange?,
    private val domainName: String,
    private val entityName: String
) :
    PsiPolyVariantReferenceBase<YAMLScalar>(element, range) {

    private val range = TextRange.allOf(entityId(domainName, entityName))

    /**
     * Considers the whole entity ID as the reference name.
     * [Similar use case](https://intellij-support.jetbrains.com/hc/en-us/community/posts/206785615/comments/206740809)
     */
    override fun getRangeInElement(): TextRange = range

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val fileIndex = ProjectRootManager.getInstance(element.project).fileIndex
        val module = fileIndex.getModuleForFile(element.containingFile.virtualFile)
        if (module != null) {
            var result: Array<ResolveResult>? = null

            if (KEY_NAME_DOMAINS.contains(domainName)) {
                result = handleKeyNameDomain(module, domainName, entityName)
            } else {
                when (domainName) {
                    HassKnownDomains.AUTOMATION -> {
                        result = handleAutomation(module, entityName)
                    }
                }
            }

            if (isActionCall(element)) {
                if (result.isNullOrEmpty()) {
                    // a (local) action (script) was not found, try resolving it as an action call
                    result = handleActionCall(module, domainName, entityName)
                }
            } else {
                if (result.isNullOrEmpty()) {
                    result = handleGenericEntity(module, domainName, entityName)
                }
            }

            return result
        }

        return ResolveResult.EMPTY_ARRAY
    }

    private fun handleKeyNameDomain(module: Module, domainName: String, entityName: String): Array<ResolveResult> {
        val service = HassDataRepository.getInstance(module.project)
        return service.getKeyNameDomainElements(module, domainName).filter {
            it.keyText == entityName
        }
            .map { result -> PsiElementResolveResult(result) }
            .toTypedArray()
    }

    private fun handleAutomation(module: Module, entityName: String): Array<ResolveResult> {
        val service = HassDataRepository.getInstance(module.project)
        return service.getAutomations(module).filter {
            it.valueText == entityName
        }
            .map { result -> PsiElementResolveResult(result) }
            .toTypedArray()
    }

    private fun handleActionCall(module: Module, domainName: String, entityName: String): Array<ResolveResult> {
        val service = HassRemoteRepository.getInstance(module.project)

        val services = service.getServices(module)
        if (services != null) {
            // TODO convert data structures to avoid traversing the tree again
            return PsiElementResolveResult.createResults(services.filter {
                it.name == entityName && getDomainNameFromActionName(it) == domainName
            })
        }
        // for now we don't trigger a download from here (we need to handle several race conditions)

        return ResolveResult.EMPTY_ARRAY
    }

    private fun handleGenericEntity(module: Module, domainName: String, entityName: String): Array<ResolveResult> {
        val service = HassRemoteRepository.getInstance(module.project)

        val states = service.getStates(module, HassKnownDomains.SCRIPT, HassKnownDomains.AUTOMATION)
        if (states != null) {
            val entityId = "$domainName.$entityName"
            return states.filter {
                it.value == entityId
            }
                .map { result -> PsiElementResolveResult(result) }
                .toTypedArray()
        }

        return ResolveResult.EMPTY_ARRAY
    }

    private fun isActionCall(element: YAMLScalar): Boolean {
        // TODO constantize
        return element.parent is YAMLKeyValue &&
                ((element.parent as YAMLKeyValue).keyText == "action" ||
                        (element.parent as YAMLKeyValue).keyText == "service")
    }

}
