package it.casaricci.hass.plugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.*
import it.casaricci.hass.plugin.HassKnownDomains
import it.casaricci.hass.plugin.HassKnownFilenames
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence

private val AUTOMATIONS_CACHE = Key<CachedValue<Collection<YAMLKeyValue>>>("HASS_AUTOMATIONS_CACHE")
private val ACTIONS_CACHE = Key<CachedValue<Collection<PsiNamedElement>>>("HASS_ACTIONS_CACHE")
private val ENTITIES_CACHE = Key<CachedValue<Collection<PsiElement>>>("HASS_ENTITIES_CACHE")
private val SECRETS_CACHE = Key<CachedValue<Collection<YAMLKeyValue>>>("HASS_SECRETS_CACHE")

private val DOMAIN_CACHES = mutableMapOf<String, Key<CachedValue<Collection<YAMLKeyValue>>>>()

private fun getCacheKey(domainName: String): Key<CachedValue<Collection<YAMLKeyValue>>> {
    return DOMAIN_CACHES.getOrPut(domainName) {
        Key<CachedValue<Collection<YAMLKeyValue>>>("HASS_${domainName}_CACHE")
    }
}

/**
 * Unified access point for Home Assistant data (actions, sensors, automations, scripts, ...). Combines local (i.e.
 * parsed from YAML files) with remote (retrieved via Home Assistant REST API) data when possible. Local data always
 * has priority.
 */
@Service(Service.Level.PROJECT)
class HassDataRepository(private val project: Project) {

    // TODO lots of duplicated and unefficient code here

    /**
     * Returns a list of all second level key-value elements (under the given first level key)
     * in all YAML files in the module.
     *
     * ```yaml
     * script:
     *   example_script1:  # selected
     *      [...]
     *
     * automation:
     *   - alias: first automation # NOT selected: not a key-value direct descendant
     *     [...]
     *
     * input_text:
     *   field_test: # selected
     *     [...]
     * ```
     */
    fun getKeyValueElementsForDomain(module: Module, domainName: String): Collection<YAMLKeyValue> {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            getCacheKey(domainName),
            {
                CachedValueProvider.Result.create(
                    getSecondLevelElementsByKeyName(module, domainName),
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }

    // TODO surely there is a more efficient way for merging local entities with remote entities
    fun getEntities(module: Module): Collection<PsiElement> {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            ENTITIES_CACHE,
            {
                val remoteService = HassRemoteRepository.getInstance(module.project)

                // TODO we should use local definitions for domains we can handle (groups, input_*, etc.)
                val localEntities = this.getKeyValueElementsForDomain(module, HassKnownDomains.SCRIPT)
                // list of all entity id (to filter out duplicates from remote entities)
                val allEntityIds = localEntities.map { it.keyText }.toHashSet()

                val remoteEntities =
                    remoteService.getStates(module, HassKnownDomains.SCRIPT)?.filter { action ->
                        !allEntityIds.contains(action.name)
                    }

                val allEntities = if (remoteEntities != null) (localEntities + remoteEntities) else localEntities

                CachedValueProvider.Result.create(
                    allEntities,
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }

    // TODO surely there is a more efficient way for merging local scripts with remote actions
    fun getActions(module: Module): Collection<PsiNamedElement> {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            ACTIONS_CACHE,
            {
                val remoteService = HassRemoteRepository.getInstance(module.project)

                val localActions = this.getKeyValueElementsForDomain(module, HassKnownDomains.SCRIPT)
                // list of all script names (to filter out duplicates from remote services)
                val allScriptNames = localActions.map { it.keyText }.toHashSet()

                val remoteActions = remoteService.getServices(module)?.filter { action ->
                    !allScriptNames.contains(action.name)
                }

                val allActions = if (remoteActions != null) (localActions + remoteActions) else localActions

                CachedValueProvider.Result.create(
                    allActions,
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }

    /**
     * List of all automations. Uses local data only.
     */
    fun getAutomations(module: Module): Collection<YAMLKeyValue> {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            AUTOMATIONS_CACHE,
            {
                CachedValueProvider.Result.create(buildList {
                    for (yamlFile in findAllYamlPsiFiles(module)) {
                        // TODO is there a more efficient way to do this? This seems like an overkill...
                        YAMLUtil.getQualifiedKeyInFile(yamlFile, HassKnownDomains.AUTOMATION)?.let { automationBlock ->
                            automationBlock.childrenOfType<YAMLSequence>().firstOrNull()?.let { automations ->
                                addAll(
                                    automations.items
                                        .flatMap { automation ->
                                            automation.childrenOfType<YAMLMapping>().first().keyValues
                                        })
                                // TODO some filter here maybe?
                            }
                        }
                    }
                }, PsiModificationTracker.MODIFICATION_COUNT)
            },
            false
        )
    }

    fun getSecrets(module: Module): Collection<YAMLKeyValue> {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            SECRETS_CACHE,
            {
                val result = buildList {
                    for (contentRoot in ModuleRootManager.getInstance(module).contentRoots) {
                        val secretsFile = contentRoot.findChild(HassKnownFilenames.SECRETS)
                        if (secretsFile != null) {
                            val yamlFile = secretsFile.findPsiFile(module.project)
                            if (yamlFile !is YAMLFile) {
                                continue
                            }

                            addAll(YAMLUtil.getTopLevelKeys(yamlFile))
                        }
                    }
                }

                CachedValueProvider.Result.create(
                    result,
                    PsiModificationTracker.MODIFICATION_COUNT
                )
            },
            false
        )
    }

    /**
     * See [getKeyValueElementsForDomain].
     */
    private fun getSecondLevelElementsByKeyName(module: Module, rootKey: String): Collection<YAMLKeyValue> {
        return buildList {
            for (yamlFile in findAllYamlPsiFiles(module)) {
                // TODO is there a more efficient way to do this? This seems like an overkill...
                YAMLUtil.getQualifiedKeyInFile(yamlFile, rootKey)?.let { scriptBlock ->
                    scriptBlock.childrenOfType<YAMLMapping>().firstOrNull()?.let { scripts ->
                        addAll(
                            scripts.keyValues
                                .filter { script -> script.key != null })
                    }
                }
            }
        }
    }

    private fun findAllYamlPsiFiles(module: Module): Collection<YAMLFile> {
        return findAllYamlFiles(module).mapNotNull {
            it.findPsiFile(module.project)
        }.filterIsInstance<YAMLFile>()
    }

    private fun findAllYamlFiles(module: Module): Collection<VirtualFile> {
        val scope = GlobalSearchScope.union(
            module.rootManager.contentRoots.map {
                GlobalSearchScopesCore.directoryScope(module.project, it, true)
            }
        )
        return FileTypeIndex.getFiles(YAMLFileType.YML, scope)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): HassDataRepository {
            return project.getService(HassDataRepository::class.java)
        }
    }

}
