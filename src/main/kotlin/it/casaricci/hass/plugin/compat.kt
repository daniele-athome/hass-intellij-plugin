package it.casaricci.hass.plugin

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.contextModality
import com.intellij.openapi.progress.*
import com.intellij.platform.util.progress.RawProgressReporter
import com.intellij.platform.util.progress.internalCreateRawHandleFromContextStepIfExistsAndFresh
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * Compatibility function for [coroutineToIndicator].
 */
@Deprecated("Move to coroutine progress reporting")
suspend fun <T> coroutineToIndicatorCompat(action: (ProgressIndicator?) -> T): T {
    return try {
        coroutineToIndicator(action)
    } catch (e: NoSuchMethodError) {
        val ctx = coroutineContext
        contextToIndicator(ctx) {
            action(ProgressManager.getGlobalProgressIndicator())
        }
    }
}

@Throws(CancellationException::class)
private fun <T> contextToIndicator(ctx: CoroutineContext, action: () -> T): T {
    val job = ctx.job
    job.ensureActive()
    val contextModality = ctx.contextModality() ?: ModalityState.nonModal()
    val handle = ctx.internalCreateRawHandleFromContextStepIfExistsAndFresh()
    return if (handle != null) {
        handle.use {
            val indicator = RawProgressReporterIndicator(handle.reporter, contextModality)
            jobToIndicator(job, indicator, action)
        }
    } else {
        val indicator = EmptyProgressIndicator(contextModality)
        jobToIndicator(job, indicator, action)
    }
}

internal class RawProgressReporterIndicator(
    private val reporter: RawProgressReporter,
    contextModality: ModalityState,
) : EmptyProgressIndicator(contextModality) {

    override fun setText(text: String?) {
        reporter.text(text)
    }

    override fun setText2(text: String?) {
        reporter.details(text)
    }

    override fun setFraction(fraction: Double) {
        reporter.fraction(fraction)
    }

    override fun setIndeterminate(indeterminate: Boolean) {
        if (indeterminate) {
            reporter.fraction(null)
        } else {
            reporter.fraction(0.0)
        }
    }
}
