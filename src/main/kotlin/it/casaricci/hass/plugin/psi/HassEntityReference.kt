package it.casaricci.hass.plugin.psi

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.SECOND_LEVEL_KEY_IDENTIFIER_DOMAINS
import it.casaricci.hass.plugin.entityId
import it.casaricci.hass.plugin.isActionCall
import it.casaricci.hass.plugin.services.HassDataRepository
import it.casaricci.hass.plugin.services.HassRemoteRepository
import it.casaricci.hass.plugin.services.getDomainNameFromActionName
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

            if (SECOND_LEVEL_KEY_IDENTIFIER_DOMAINS.contains(domainName)) {
                result = resolveKeyNameDomain(module, domainName, entityName)
            } else {
                when (domainName) {
                    HassKnownDomains.AUTOMATION -> {
                        result = resolveAutomation(module, entityName)
                    }
                }
            }

            if (isActionCall(element)) {
                if (result.isNullOrEmpty()) {
                    // a (local) action (script) was not found, try resolving it as a remote action call
                    result = resolveRemoteAction(module, domainName, entityName)
                }
            } else {
                if (result.isNullOrEmpty()) {
                    result = resolveRemoteEntity(module, domainName, entityName)
                }
            }

            return result
        }

        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Resolves supported second-level entities (see [HassDataRepository.getKeyValueElementsForDomains] to understand
     * what that means). See [SECOND_LEVEL_KEY_IDENTIFIER_DOMAINS] for supported domains.
     */
    private fun resolveKeyNameDomain(module: Module, domainName: String, entityName: String): Array<ResolveResult> {
        val service = HassDataRepository.getInstance(module.project)
        return service.getKeyValueElementsForDomains(module, domainName).filter {
            it.keyText == entityName
        }
            .map { result -> PsiElementResolveResult(result) }
            .toTypedArray()
    }

    /**
     * Automations are identified by the value of their "alias" key. The actual PSI element is wrapped by
     * [it.casaricci.hass.plugin.language.HassAutomation]. That will actually resolve to either a local or remote
     * automation.
     */
    private fun resolveAutomation(module: Module, entityName: String): Array<ResolveResult> {
        val service = HassDataRepository.getInstance(module.project)
        return service.getAutomations(module).filter {
            it.textValue == entityName
        }
            .map { result -> PsiElementResolveResult(result) }
            .toTypedArray()
    }

    /**
     * Resolves any action call, taking the user to the remote services cache file.
     */
    private fun resolveRemoteAction(module: Module, domainName: String, entityName: String): Array<ResolveResult> {
        val service = HassRemoteRepository.getInstance(module.project)

        val services = service.getServices(module)
        if (services != null) {
            return PsiElementResolveResult.createResults(services.filter {
                it.name == entityName && getDomainNameFromActionName(it) == domainName
            })
        }
        // for now we don't trigger a download from here (we need to handle several race conditions)

        return ResolveResult.EMPTY_ARRAY
    }

    /**
     * Resolves any other entity, taking the user to the remote states (entities) cache file.
     */
    private fun resolveRemoteEntity(module: Module, domainName: String, entityName: String): Array<ResolveResult> {
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

}
