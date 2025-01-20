package it.casaricci.hass.plugin.services

import com.intellij.json.psi.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.PsiManager
import com.intellij.psi.util.*
import com.intellij.util.Url
import com.intellij.util.Urls
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.createDirectories
import it.casaricci.hass.plugin.getConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile

private val CACHE_PATH = PathManager.getSystemDir().resolve("homeassistant")

private const val API_SERVICES_PATH = "api/services"
private const val API_STATES_PATH = "api/states"

fun getDomainNameFromActionName(property: JsonProperty): String? {
    val domainName = property.findParentOfType<JsonObject>()
        ?.findParentOfType<JsonObject>()
        ?.findProperty("domain")?.value
    return if (domainName is JsonStringLiteral) {
        domainName.value
    } else {
        null
    }
}

fun getDomainNameFromActionName(property: YAMLKeyValue): String? {
    val parentKey = property.parentMapping?.parent
    return if (parentKey is YAMLKeyValue) {
        parentKey.keyText
    } else {
        null
    }
}

/**
 * See [StateObject].
 */
@Serializable
private data class StateAttributesObject(
    val icon: String? = null,
    @SerialName("friendly_name")
    val friendlyName: String? = null,
    @SerialName("unit_of_measurement")
    val unitOfMeasurement: String? = null,
    @SerialName("device_class")
    val deviceClass: String? = null,
)

/**
 * Whitelisted fields that we can save in the states cache.
 */
@Serializable
private data class StateObject(
    @SerialName("entity_id")
    val entityId: String,
    val attributes: StateAttributesObject
)

private val SERVICES_CACHE = Key<CachedValue<Collection<JsonProperty>>>("HASS_SERVICES")

private val STATES_CACHES = mutableMapOf<String, Key<CachedValue<Collection<JsonStringLiteral>>>>()

private fun getStatesCacheKey(vararg domainName: String): Key<CachedValue<Collection<JsonStringLiteral>>> {
    val key = domainName.joinToString("_")
    return STATES_CACHES.getOrPut(key) {
        Key<CachedValue<Collection<JsonStringLiteral>>>("HASS_${key}_CACHE")
    }
}

/**
 * Service used for caching data from Home Assistant instance (services, entities, etc.).
 */
@Service(Service.Level.PROJECT)
class HassRemoteRepository(private val project: Project, private val cs: CoroutineScope) {

    @OptIn(ExperimentalSerializationApi::class)
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    init {
        thisLogger().info("Creating cache directory")
        try {
            ensureCachePathExists()
        } catch (e: IOException) {
            thisLogger().warn("Failed to create cache directory", e)
        }
    }

    private fun isServicesCacheAvailable(module: Module): Boolean {
        val cacheFile = getServicesCacheFile(module)
        return cacheFile.isRegularFile() && cacheFile.isReadable()
    }

    private fun isStatesCacheAvailable(module: Module): Boolean {
        val cacheFile = getStatesCacheFile(module)
        return cacheFile.isRegularFile() && cacheFile.isReadable()
    }

    /**
     * Refresh all Home Assistant data caches.
     * @return true if at least one refresh from network was triggered.
     */
    fun refreshCache(module: Module, force: Boolean = false): Boolean {
        return refreshServices(module, force) and
                refreshStates(module, force)
    }

    private fun refreshServices(module: Module, force: Boolean = false): Boolean {
        // TODO maybe invalidate the cache after some time?
        if (!isServicesCacheAvailable(module) || force) {
            downloadServices(module, null)
            return true
        }
        return false
    }

    private fun refreshStates(module: Module, force: Boolean = false): Boolean {
        // TODO maybe invalidate the cache after some time?
        if (!isStatesCacheAvailable(module) || force) {
            downloadStates(module, null)
            return true
        }
        return false
    }

    /**
     * Tries to read from cache or locally downloaded JSON file all services.
     */
    fun getServices(module: Module): Collection<JsonProperty>? {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            SERVICES_CACHE,
            {
                val result: List<JsonProperty>? = if (isServicesCacheAvailable(module)) {
                    ReadAction.compute<List<JsonProperty>, Throwable> {
                        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(getServicesCacheFile(module))
                        // TODO handle nulls
                        val psiFile = virtualFile!!.findPsiFile(module.project)

                        thisLogger().debug("Services data: $psiFile")

                        if (psiFile is JsonFile && psiFile.topLevelValue is JsonArray) {
                            (psiFile.topLevelValue as JsonArray).valueList.filter {
                                if (it is JsonObject) {
                                    val domainNameElement = it.findProperty("domain")?.value
                                    domainNameElement is JsonStringLiteral
                                } else false
                            }
                                .flatMap {
                                    val domainObject = it as JsonObject
                                    (domainObject.findProperty("services")?.value as JsonObject).propertyList
                                }
                        } else {
                            // TODO wrong file content???
                            emptyList()
                        }
                    }
                } else {
                    null
                }

                CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
            },
            false
        )
    }

    fun getStates(module: Module, vararg excludeDomains: String): Collection<JsonStringLiteral>? {
        return CachedValuesManager.getManager(project).getCachedValue(
            module,
            getStatesCacheKey(*excludeDomains),
            {
                val result: List<JsonStringLiteral>? = if (isStatesCacheAvailable(module)) {
                    ReadAction.compute<List<JsonStringLiteral>, Throwable> {
                        val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(getStatesCacheFile(module))
                        // TODO handle nulls
                        val psiFile = virtualFile!!.findPsiFile(module.project)

                        thisLogger().debug("States data: $psiFile")

                        if (psiFile is JsonFile && psiFile.topLevelValue is JsonArray) {
                            (psiFile.topLevelValue as JsonArray).valueList.filter {
                                if (it is JsonObject) {
                                    val entityIdElement = it.findProperty("entity_id")?.value
                                    if (entityIdElement is JsonStringLiteral) {
                                        val domainName = entityIdElement.value.substringBefore(".", "")
                                        !excludeDomains.contains(domainName)
                                    } else false
                                } else false
                            }
                                .map {
                                    // null checks were already done during filtering
                                    val entityObject = it as JsonObject
                                    entityObject.findProperty("entity_id")?.value as JsonStringLiteral
                                }
                        } else {
                            // TODO wrong file content???
                            emptyList()
                        }
                    }
                } else {
                    null
                }

                CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
            },
            false
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UnstableApiUsage")
    private fun downloadServices(module: Module, onComplete: (() -> Unit)?) {
        val log = thisLogger()

        cs.launch {
            lock.write {
                // TODO error handling
                val config = getConfiguration(module) ?: return@launch
                val url = buildServicesUrl(config.instanceUrl) ?: return@launch
                val cachedResponseFile = getServicesCacheFile(module)

                log.info("Downloading services to $cachedResponseFile")

                withBackgroundProgress(
                    module.project,
                    // TODO i18n
                    "Updating services data from Home Assistant",
                    cancellable = true
                ) {
                    coroutineToIndicator {
                        // TODO error handling
                        downloadAndFormatJson(
                            url,
                            config.token,
                            cachedResponseFile
                        ) { data -> jsonFormatter.decodeFromStream<kotlinx.serialization.json.JsonArray>(data) }

                        onComplete?.let { it() }
                    }
                }
            }
        }
    }

    /**
     * Download states from Home Assistant. Not all data is saved: a whitelist of cached fields are in [StateObject].
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UnstableApiUsage")
    private fun downloadStates(module: Module, onComplete: (() -> Unit)?) {
        val log = thisLogger()

        cs.launch {
            lock.write {
                // TODO error handling
                val config = getConfiguration(module) ?: return@launch
                val url = buildStatesUrl(config.instanceUrl) ?: return@launch
                val cachedResponseFile = getStatesCacheFile(module)

                log.info("Downloading states to $cachedResponseFile")

                withBackgroundProgress(
                    module.project,
                    // TODO i18n
                    "Updating states data from Home Assistant",
                    cancellable = true
                ) {
                    coroutineToIndicator {
                        // TODO error handling
                        downloadAndFormatJson(url, config.token, cachedResponseFile) { data ->
                            jsonFormatter.decodeFromStream<List<StateObject>>(data)
                        }

                        onComplete?.let { it() }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T> downloadAndFormatJson(
        url: Url,
        accessToken: String,
        cachedFile: Path,
        crossinline decoder: (data: InputStream) -> T
    ) {
        ensureCachePathExists()

        HttpRequests.request(url)
            .tuner { conn ->
                conn.addRequestProperty("Authorization", "Bearer $accessToken")
            }
            .connect { request ->
                // TODO no progress update...
                val jsonData = decoder(request.inputStream)
                FileOutputStream(cachedFile.toFile()).use { stream ->
                    jsonFormatter.encodeToStream(jsonData, stream)
                }
            }
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(cachedFile)
        // TODO null checks
        VfsUtil.markDirtyAndRefresh(false, false, true, virtualFile!!)

        ApplicationManager.getApplication().invokeLater {
            PsiManager.getInstance(project).dropPsiCaches()
        }
    }

    @Throws(IOException::class)
    private fun ensureCachePathExists() {
        CACHE_PATH.createDirectories()
    }

    private fun getServicesCacheFile(module: Module): Path {
        val projectName = FileUtil.sanitizeFileName(module.project.locationHash)
        val moduleName = FileUtil.sanitizeFileName(module.name)
        return CACHE_PATH.resolve("${projectName}_${moduleName}_services.json")
    }

    private fun getStatesCacheFile(module: Module): Path {
        val projectName = FileUtil.sanitizeFileName(module.project.locationHash)
        val moduleName = FileUtil.sanitizeFileName(module.name)
        return CACHE_PATH.resolve("${projectName}_${moduleName}_states.json")
    }

    private fun buildServicesUrl(instanceUrl: String): Url? {
        return Urls.parse(instanceUrl, false)?.resolve(API_SERVICES_PATH)
    }

    private fun buildStatesUrl(instanceUrl: String): Url? {
        return Urls.parse(instanceUrl, false)?.resolve(API_STATES_PATH)
    }

}
