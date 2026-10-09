package dev.spaceapps.weather.ios

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Handle for a Flow collected on behalf of Swift. Call [cancel] when the Swift owner goes away.
 * (Named to avoid clashing with Combine's `Cancellable` in Swift.)
 */
class FlowSubscription internal constructor(private val scope: CoroutineScope) {
    fun cancel() {
        scope.cancel()
    }
}

/** Collects on the main thread, so Swift callbacks can update UI state directly. */
internal fun <T> Flow<T>.subscribe(onEach: (T) -> Unit): FlowSubscription {
    val scope = MainScope()
    scope.launch { collect { onEach(it) } }
    return FlowSubscription(scope)
}
