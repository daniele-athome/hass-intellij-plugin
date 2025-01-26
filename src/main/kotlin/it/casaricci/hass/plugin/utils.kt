package it.casaricci.hass.plugin

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.io.CountingGZIPInputStream
import org.apache.commons.io.input.CountingInputStream
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import java.io.InputStream

// TODO this should start from configuration.yaml and walk all includes (in order to filter out unwanted files)
fun isHassConfigFile(virtualFile: VirtualFile, project: Project): Boolean {
    if (virtualFile.fileType == YAMLFileType.YML) {
        return ModuleUtil.findModuleForFile(virtualFile, project)?.let { isHomeAssistantModule(it) } == true
    }
    return false
}

// TODO this should start from configuration.yaml and walk all includes (in order to filter out unwanted files)
fun isHassConfigFile(element: PsiElement): Boolean {
    if (element.containingFile.fileType == YAMLFileType.YML) {
        return ModuleUtil.findModuleForPsiElement(element)?.let { isHomeAssistantModule(it) } == true
    }
    return false
}

fun entityId(domainName: String, entityName: String): String = "$domainName.$entityName"

private val ENTITY_ID_REGEX = Regex("^([A-Za-z0-9_]*)\\.([A-Za-z0-9_]*)$")

fun splitEntityId(entityId: String): Pair<String, String> {
    val match = ENTITY_ID_REGEX.matchEntire(entityId)
    return if (match != null && match.groupValues.size == 3) {
        Pair(match.groupValues[1], match.groupValues[2])
    } else {
        Pair("", "")
    }
}

fun isActionCall(element: YAMLScalar): Boolean {
    return element.parent is YAMLKeyValue &&
            ((element.parent as YAMLKeyValue).keyText == "action" ||
                    (element.parent as YAMLKeyValue).keyText == "service")
}

/**
 * An [InputStream] that updates a [ProgressIndicator] while being read.
 * To be used with [com.intellij.util.io.HttpRequests], handles also [CountingGZIPInputStream] for compressed streams.
 * I was surprised that the IntelliJ SDK didn't provide such a utility (or at least I didn't find any).
 */
class ProgressIndicatorInputStream(
    private val stream: InputStream?,
    private val contentLength: Long,
    private val indicator: ProgressIndicator
) : CountingInputStream(stream) {

    private val gzipStream: Boolean = stream is CountingGZIPInputStream

    init {
        indicator.checkCanceled()
        indicator.isIndeterminate = contentLength <= 0
    }

    override fun afterRead(n: Int) {
        val bytesRead = if (gzipStream) {
            (stream as CountingGZIPInputStream).compressedBytesRead
        } else {
            count.toLong()
        }
        super.afterRead(n)

        indicator.checkCanceled()
        if (contentLength > 0) {
            updateIndicator(indicator, bytesRead, contentLength)
        }
    }

    /**
     * Copied from [com.intellij.util.net.NetUtils.updateIndicator].
     */
    private fun updateIndicator(
        indicator: ProgressIndicator,
        bytesDownloaded: Long,
        contentLength: Long,
    ) {
        val fraction = bytesDownloaded.toDouble() / contentLength
        val rankForContentLength = StringUtilRt.rankForFileSize(contentLength)
        val formattedContentLength =
            StringUtilRt.formatFileSize(contentLength, " ", rankForContentLength)
        val formattedTotalProgress =
            StringUtilRt.formatFileSize(bytesDownloaded, " ", rankForContentLength)

        @Suppress("UnstableApiUsage")
        val indicatorText: @NlsSafe String = String.format(
            "<html><code>%.0f%% · %s⧸%s</code></html>", fraction * 100,
            formattedTotalProgress,
            formattedContentLength
        )
        indicator.text2 = indicatorText
        indicator.fraction = fraction
    }


}
