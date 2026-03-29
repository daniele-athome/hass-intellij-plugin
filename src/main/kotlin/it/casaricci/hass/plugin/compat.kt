package it.casaricci.hass.plugin

import com.intellij.concurrency.IntelliJContextElement
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.*
import com.intellij.platform.util.progress.ProgressReporter
import com.intellij.platform.util.progress.RawProgressReporter
import com.intellij.platform.util.progress.RawProgressReporterHandle
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.StepState
import com.intellij.platform.util.progress.impl.ProgressText
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job

/** Compatibility function for [coroutineToIndicator]. */
@Deprecated("Move to coroutine progress reporting")
suspend fun <T> coroutineToIndicatorCompat(action: (ProgressIndicator?) -> T): T {
    return try {
        coroutineToIndicator(action)
    } catch (e: NoSuchMethodError) {
        val ctx = coroutineContext
        contextToIndicator(ctx) { action(ProgressManager.getGlobalProgressIndicator()) }
    }
}

internal sealed interface SequentialProgressReporterHandle : AutoCloseable {

    val reporter: SequentialProgressReporter

    override fun close()
}

internal sealed interface ProgressReporterHandle : AutoCloseable {

    val reporter: ProgressReporter

    override fun close()
}

internal sealed interface ProgressStep {

    fun progressUpdates(): Flow<StepState>

    suspend fun <X> withText(text: ProgressText, action: suspend CoroutineScope.() -> X): X

    fun asConcurrent(size: Int): ProgressReporterHandle?

    fun asSequential(size: Int): SequentialProgressReporterHandle?

    fun asRaw(): RawProgressReporterHandle?
}

private class ProgressStepElement(val step: ProgressStep) :
    AbstractCoroutineContextElement(Key), IntelliJContextElement {
    override fun produceChildElement(
        parentContext: CoroutineContext,
        isStructured: Boolean,
    ): IntelliJContextElement = this

    object Key : CoroutineContext.Key<ProgressStepElement>
}

/** Modified to not use internal APIs. */
@Throws(CancellationException::class)
private fun <T> contextToIndicator(ctx: CoroutineContext, action: () -> T): T {
    val job = ctx.job
    job.ensureActive()
    val contextModality = ModalityState.nonModal()
    val handle = ctx[ProgressStepElement.Key]?.step?.asRaw()
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
