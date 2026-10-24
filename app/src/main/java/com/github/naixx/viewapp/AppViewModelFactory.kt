package com.github.naixx.viewapp

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

/**
 * The app's one `ViewModelProvider.Factory`: every view model is a `@ViewModelKey @ContributesIntoMap(AppScope)` entry
 * in these maps (the per-clip `TimelapseViewModel.Factory` in the manual-assisted one), and `metroViewModel()` /
 * `assistedMetroViewModel()` reach it through `LocalMetroViewModelFactory`, which the Activity provides. Metro ships only
 * the abstract class because the maps are keyed by the app's scope. Moves to `:ui` with the screens (step 9).
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AppViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders:
        Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>,
) : MetroViewModelFactory()
