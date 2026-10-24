package github.naixx.core

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Platform-free bindings every shell's `@DependencyGraph(AppScope::class)` picks up. */
@ContributesTo(AppScope::class)
@BindingContainer
object CoreBindings {

    /** The one process-lifetime coroutine scope (the connection's state, the live-frame cache). */
    @Provides
    @SingleIn(AppScope::class)
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
