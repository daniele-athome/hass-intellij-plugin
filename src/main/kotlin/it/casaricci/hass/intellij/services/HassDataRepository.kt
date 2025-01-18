package it.casaricci.hass.intellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.*
import it.casaricci.hass.intellij.HASS_KEY_AUTOMATION
import it.casaricci.hass.intellij.HASS_KEY_SCRIPT
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLSequence

private const val SECRETS_FILENAME = "secrets.yaml"

private val AUTOMATIONS_CACHE = Key<CachedValue<Collection<YAMLKeyValue>>>("HASS_AUTOMATIONS_CACHE")
private val ACTIONS_CACHE = Key<CachedValue<Collection<PsiNamedElement>>>("HASS_ACTIONS_CACHE")
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

    fun getKeyNameDomainElements(module: Module, domainName: String): Collection<YAMLKeyValue> {
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

    // TODO surely there is a more efficient way for merging local scripts with remote actions
    fun getActions(module: Module): Collection<PsiNamedElement> {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            ACTIONS_CACHE,
            {
                val remoteService = module.project.getService(HassRemoteRepository::class.java)

                val localActions = this.getKeyNameDomainElements(module, HASS_KEY_SCRIPT)
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
                val fileIndex = ProjectRootManager.getInstance(project).fileIndex
                val allFiles =
                    FilenameIndex.getAllFilesByExt(project, "yaml", GlobalSearchScope.projectScope(project))
                val psiManager = PsiManager.getInstance(project)

                CachedValueProvider.Result.create(buildList {
                    for (file in allFiles) {
                        // apparently GlobalSearchScope.moduleScope doesn't find the file
                        if (module != fileIndex.getModuleForFile(file)) {
                            continue
                        }

                        val yamlFile = psiManager.findFile(file) ?: continue
                        if (yamlFile !is YAMLFile) {
                            continue
                        }

                        // TODO is there a more efficient way to do this? This seems like an overkill...
                        YAMLUtil.getQualifiedKeyInFile(yamlFile, HASS_KEY_AUTOMATION)?.let { automationBlock ->
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
                        val secretsFile = contentRoot.findChild(SECRETS_FILENAME)
                        if (secretsFile != null) {
                            val yamlFile = PsiManager.getInstance(module.project).findFile(secretsFile) ?: continue
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

    private fun getSecondLevelElementsByKeyName(module: Module, rootKey: String): Collection<YAMLKeyValue> {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val allFiles =
            FilenameIndex.getAllFilesByExt(project, "yaml", GlobalSearchScope.projectScope(project))
        val psiManager = PsiManager.getInstance(project)

        return buildList {
            for (file in allFiles) {
                // apparently GlobalSearchScope.moduleScope doesn't find the file
                if (module != fileIndex.getModuleForFile(file)) {
                    continue
                }

                val yamlFile = psiManager.findFile(file) ?: continue
                if (yamlFile !is YAMLFile) {
                    continue
                }

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

}
