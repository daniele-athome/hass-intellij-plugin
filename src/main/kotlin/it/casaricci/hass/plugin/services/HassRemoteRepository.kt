package it.casaricci.hass.plugin.services

import com.intellij.json.psi.*
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModulePointerManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.*
import com.intellij.util.Url
import com.intellij.util.Urls
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.createDirectories
import it.casaricci.hass.plugin.MyBundle
import it.casaricci.hass.plugin.ProgressIndicatorInputStream
import it.casaricci.hass.plugin.coroutineToIndicatorCompat
import it.casaricci.hass.plugin.getConfiguration
import it.casaricci.hass.plugin.services.HassRemoteRepository.Companion.getServicesCacheFile
import it.casaricci.hass.plugin.splitEntityId
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.jetbrains.yaml.psi.YAMLKeyValue

private val CACHE_PATH = PathManager.getSystemDir().resolve("homeassistant")

private const val API_SERVICES_PATH = "api/services"
private const val API_STATES_PATH = "api/states"

// unused for now
fun isHassServicesCacheFile(cacheElement: PsiElement, moduleElement: PsiElement?): Boolean {
    if (moduleElement == null) {
        return false
    }

    val module = ModuleUtil.findModuleForPsiElement(moduleElement)
    if (module != null) {
        return cacheElement.containingFile.virtualFile.path ==
            getServicesCacheFile(module).pathString
    }
    return false
}

/**
 * Returns the domain name for the given action coming from the JSON returned by /api/services.
 *
 * ```json
 * [
 *    {
 *       "domain" : "homeassistant", // returns "homeassistant"
 *       "services" : {
 *          "check_config" : {  // given this JsonProperty
 *             "description" : "Checks the Home Assistant YAML-configuration files for errors. Errors will be shown in the Home Assistant logs.",
 *          }
 *       }
 *    }
 * ]
 * ```
 */
fun getDomainNameFromActionName(property: JsonProperty): String? {
    val domainName =
        property
            .findParentOfType<JsonObject>()
            ?.findParentOfType<JsonObject>()
            ?.findProperty("domain")
            ?.value
    return if (domainName is JsonStringLiteral) {
        domainName.value
    } else {
        null
    }
}

/**
 * Returns the domain name for the given second-level YAML entry (works for e.g. scripts but not
 * automations).
 *
 * ```json
 * script: // returns "script"
 *   check_config: // given this YAMLKeyValue
 *     sequence:
 *       - action: script.another
 * ```
 */
fun getDomainNameFromSecondLevelElement(property: YAMLKeyValue): String? {
    val parentKey = property.parentMapping?.parent
    return if (parentKey is YAMLKeyValue) {
        parentKey.keyText
    } else {
        null
    }
}

/** See [StateObject]. */
@Serializable
private data class StateAttributesObject(
    val icon: String? = null,
    @SerialName("friendly_name") val friendlyName: String? = null,
    @SerialName("unit_of_measurement") val unitOfMeasurement: String? = null,
    @SerialName("device_class") val deviceClass: String? = null,
)

/** Whitelisted fields that we can save in the states cache. */
@Serializable
private data class StateObject(
    @SerialName("entity_id") val entityId: String,
    val attributes: StateAttributesObject,
)

private val SERVICES_CACHE = Key<CachedValue<Collection<JsonProperty>>>("HASS_SERVICES")

private val STATES_CACHES = mutableMapOf<String, Key<CachedValue<Collection<JsonStringLiteral>>>>()

private fun getStatesCacheKey(
    vararg domainNames: String
): Key<CachedValue<Collection<JsonStringLiteral>>> {
    val key = domainNames.joinToString("_")
    return STATES_CACHES.getOrPut(key) {
        Key<CachedValue<Collection<JsonStringLiteral>>>("HASS_${key}_STATES_CACHE")
    }
}

/** Service used for caching data from Home Assistant instance (services, entities, etc.). */
open class HassRemoteRepository(private val project: Project, private val cs: CoroutineScope) {

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

    private fun isOfflineMode(module: Module): Boolean {
        return getConfiguration(module)?.instanceUrl?.isBlank() ?: true
    }

    /**
     * Refresh all Home Assistant data caches.
     *
     * @return true if at least one refresh from network was triggered.
     */
    fun refreshCache(module: Module, force: Boolean = false): Boolean {
        return refreshServices(module, force) and refreshStates(module, force)
    }

    private fun refreshServices(module: Module, force: Boolean = false): Boolean {
        if (isOfflineMode(module)) {
            // delete local cache file
            getServicesCacheFile(module).deleteIfExists()
            return true
        }

        // TODO maybe invalidate the cache after some time?
        if (!isServicesCacheAvailable(module) || force) {
            downloadServices(module)
            return true
        }
        return false
    }

    private fun refreshStates(module: Module, force: Boolean = false): Boolean {
        if (isOfflineMode(module)) {
            // delete local cache file
            getStatesCacheFile(module).deleteIfExists()
            return true
        }

        // TODO maybe invalidate the cache after some time?
        if (!isStatesCacheAvailable(module) || force) {
            downloadStates(module)
            return true
        }
        return false
    }

    /** Tries to read from cache or locally downloaded JSON file all services. */
    open fun getServices(module: Module): Collection<JsonProperty>? {
        return CachedValuesManager.getManager(project)
            .getCachedValue(
                module,
                SERVICES_CACHE,
                {
                    val result: Collection<JsonProperty>? =
                        if (isOfflineMode(module)) {
                            null
                        } else if (isServicesCacheAvailable(module)) {
                            ReadAction.compute<Collection<JsonProperty>, Throwable> {
                                try {
                                    val virtualFile =
                                        LocalFileSystem.getInstance()
                                            .findFileByNioFile(getServicesCacheFile(module))
                                            ?: throw IOException("Cache file disappeared")

                                    val psiFile = virtualFile.findPsiFile(module.project)
                                    if (
                                        psiFile !is JsonFile || psiFile.topLevelValue !is JsonArray
                                    ) {
                                        throw IOException("Corrupted or unparseable cache file")
                                    }

                                    getServices(psiFile)
                                } catch (e: ProcessCanceledException) {
                                    // canceled
                                    throw e
                                } catch (e: Exception) {
                                    thisLogger().error("Unable to load data cache", e)
                                    notifyUnexpectedError(
                                        module,
                                        MyBundle.message(
                                            "hass.notification.dataCacheError.genericError"
                                        ),
                                    )
                                    null
                                }
                            }
                        } else {
                            thisLogger().info("Service data cache not available (yet?)")
                            null
                        }

                    CachedValueProvider.Result.create(
                        result,
                        PsiModificationTracker.MODIFICATION_COUNT,
                    )
                },
                false,
            )
    }

    protected fun getServices(psiFile: JsonFile): Collection<JsonProperty> {
        return (psiFile.topLevelValue as JsonArray)
            .valueList
            .filter {
                if (it is JsonObject) {
                    val domainNameElement = it.findProperty("domain")?.value
                    domainNameElement is JsonStringLiteral
                } else false
            }
            .flatMap {
                val domainObject = it as JsonObject
                (domainObject.findProperty("services")?.value as JsonObject).propertyList
            }
    }

    open fun getStates(
        module: Module,
        vararg excludeDomains: String,
    ): Collection<JsonStringLiteral>? {
        return CachedValuesManager.getManager(project)
            .getCachedValue(
                module,
                getStatesCacheKey(*excludeDomains),
                {
                    val result: Collection<JsonStringLiteral>? =
                        if (isOfflineMode(module)) {
                            null
                        } else if (isStatesCacheAvailable(module)) {
                            ReadAction.compute<Collection<JsonStringLiteral>, Throwable> {
                                try {
                                    val virtualFile =
                                        LocalFileSystem.getInstance()
                                            .findFileByNioFile(getStatesCacheFile(module))
                                            ?: throw IOException("Cache file disappeared")

                                    val psiFile = virtualFile.findPsiFile(module.project)
                                    if (
                                        psiFile !is JsonFile || psiFile.topLevelValue !is JsonArray
                                    ) {
                                        throw IOException("Corrupted or unparseable cache file")
                                    }

                                    getStates(psiFile, *excludeDomains)
                                } catch (e: ProcessCanceledException) {
                                    // canceled
                                    throw e
                                } catch (e: Exception) {
                                    thisLogger().error("Unable to load data cache", e)
                                    notifyUnexpectedError(
                                        module,
                                        MyBundle.message(
                                            "hass.notification.dataCacheError.genericError"
                                        ),
                                    )
                                    null
                                }
                            }
                        } else {
                            thisLogger().info("States data cache not available (yet?)")
                            null
                        }

                    CachedValueProvider.Result.create(
                        result,
                        PsiModificationTracker.MODIFICATION_COUNT,
                    )
                },
                false,
            )
    }

    protected fun getStates(
        psiFile: JsonFile,
        vararg excludeDomains: String,
    ): Collection<JsonStringLiteral> {
        return (psiFile.topLevelValue as JsonArray)
            .valueList
            .filter {
                if (it is JsonObject) {
                    val entityIdElement = it.findProperty("entity_id")?.value
                    if (entityIdElement is JsonStringLiteral) {
                        val (domainName, _) = splitEntityId(entityIdElement.value)
                        !excludeDomains.contains(domainName)
                    } else false
                } else false
            }
            .map {
                // null checks were already done during filtering
                val entityObject = it as JsonObject
                entityObject.findProperty("entity_id")?.value as JsonStringLiteral
            }
    }

    /** Download services (actions) from Home Assistant. The returned information is saved as is. */
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UnstableApiUsage")
    private fun downloadServices(module: Module) {
        val log = thisLogger()

        cs.launch {
            lock.write {
                val config = getConfiguration(module) ?: return@launch
                val url = buildServicesUrl(config.instanceUrl) ?: return@launch
                val cachedResponseFile = getServicesCacheFile(module)

                log.info("Downloading services to $cachedResponseFile")

                withBackgroundProgress(
                    module.project,
                    MyBundle.message("hass.notification.refreshCache.progress"),
                    cancellable = true,
                ) {
                    coroutineToIndicatorCompat {
                        try {
                            downloadAndFormatJson(url, config.token, cachedResponseFile) {
                                data,
                                length ->
                                val stream =
                                    ProgressIndicatorInputStream(
                                        data,
                                        length,
                                        ProgressIndicatorProvider.getGlobalProgressIndicator(),
                                    )
                                jsonFormatter.decodeFromStream<
                                    kotlinx.serialization.json.JsonArray
                                >(
                                    stream
                                )
                            }
                        } catch (e: ProcessCanceledException) {
                            // canceled
                            throw e
                        } catch (e: IOException) {
                            notifyDownloadError(module, e.toString())
                        }
                    }
                }
            }
        }
    }

    /**
     * Download states from Home Assistant. Not all data is saved: a whitelist of cached fields are
     * in [StateObject].
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Suppress("UnstableApiUsage")
    private fun downloadStates(module: Module) {
        val log = thisLogger()

        cs.launch {
            lock.write {
                val config = getConfiguration(module) ?: return@launch
                val url = buildStatesUrl(config.instanceUrl) ?: return@launch
                val cachedResponseFile = getStatesCacheFile(module)

                log.info("Downloading states to $cachedResponseFile")

                withBackgroundProgress(
                    module.project,
                    MyBundle.message("hass.notification.refreshCache.progress"),
                    cancellable = true,
                ) {
                    coroutineToIndicatorCompat {
                        try {
                            downloadAndFormatJson(url, config.token, cachedResponseFile) {
                                data,
                                length ->
                                val stream =
                                    ProgressIndicatorInputStream(
                                        data,
                                        length,
                                        ProgressIndicatorProvider.getGlobalProgressIndicator(),
                                    )
                                jsonFormatter.decodeFromStream<List<StateObject>>(stream)
                            }
                        } catch (e: ProcessCanceledException) {
                            // canceled
                            throw e
                        } catch (e: IOException) {
                            notifyDownloadError(module, e.toString())
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IOException::class)
    private inline fun <reified T> downloadAndFormatJson(
        url: Url,
        accessToken: String,
        cachedFile: Path,
        crossinline decoder: (data: InputStream, length: Long) -> T,
    ) {
        ensureCachePathExists()

        HttpRequests.request(url)
            .tuner { conn -> conn.addRequestProperty("Authorization", "Bearer $accessToken") }
            .connect { request ->
                val jsonData = decoder(request.inputStream, request.connection.contentLengthLong)
                FileOutputStream(cachedFile.toFile()).use { stream ->
                    jsonFormatter.encodeToStream(jsonData, stream)
                }
            }
        val virtualFile =
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(cachedFile)
                ?: throw IOException(
                    MyBundle.message("hass.notification.refreshCache.failedLoadCachedFile")
                )
        VfsUtil.markDirtyAndRefresh(false, false, true, virtualFile)

        ApplicationManager.getApplication().invokeLater {
            PsiManager.getInstance(project).dropPsiCaches()
        }
    }

    /** Download error notification (usually network issues). */
    private fun notifyDownloadError(module: Module, message: String) {
        val moduleRef = ModulePointerManager.getInstance(module.project).create(module)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Home Assistant data refresh")
            .createNotification(
                MyBundle.message("hass.notification.refreshCache.title"),
                message,
                NotificationType.ERROR,
            )
            .addAction(
                NotificationAction.create(
                    MyBundle.message("hass.notification.error.action.tryAgain"),
                    "refresh-cache",
                ) { _, notification ->
                    moduleRef.module?.let { module -> refreshCache(module, true) }
                    notification.hideBalloon()
                }
            )
            // TODO .addAction(<action for opening module settings>)
            .notify(project)
    }

    /** Unexpected error notification (usually bugs or weird things happening). */
    private fun notifyUnexpectedError(module: Module, message: String) {
        val moduleRef = ModulePointerManager.getInstance(module.project).create(module)

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Home Assistant data cache error")
            .createNotification(
                MyBundle.message("hass.notification.dataCacheError.title"),
                message,
                NotificationType.ERROR,
            )
            .addAction(
                NotificationAction.create(
                    MyBundle.message("hass.notification.error.action.tryAgain"),
                    "refresh-cache",
                ) { _, notification ->
                    moduleRef.module?.let { module -> refreshCache(module, true) }
                    notification.hideBalloon()
                }
            )
            .notify(project)
    }

    @Throws(IOException::class)
    private fun ensureCachePathExists() {
        CACHE_PATH.createDirectories()
    }

    private fun buildServicesUrl(instanceUrl: String): Url? {
        return Urls.parse(instanceUrl, false)?.resolve(API_SERVICES_PATH)
    }

    private fun buildStatesUrl(instanceUrl: String): Url? {
        return Urls.parse(instanceUrl, false)?.resolve(API_STATES_PATH)
    }

    companion object {

        internal fun getServicesCacheFile(module: Module): Path {
            val projectName = FileUtil.sanitizeFileName(module.project.locationHash)
            val moduleName = FileUtil.sanitizeFileName(module.name)
            return CACHE_PATH.resolve("${projectName}_${moduleName}_services.json")
        }

        internal fun getStatesCacheFile(module: Module): Path {
            val projectName = FileUtil.sanitizeFileName(module.project.locationHash)
            val moduleName = FileUtil.sanitizeFileName(module.name)
            return CACHE_PATH.resolve("${projectName}_${moduleName}_states.json")
        }

        @JvmStatic
        fun getInstance(project: Project): HassRemoteRepository {
            return project.getService(HassRemoteRepository::class.java)
        }
    }
}
