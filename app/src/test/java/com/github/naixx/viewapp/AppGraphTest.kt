package com.github.naixx.viewapp

import android.app.Application
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.naixx.viewapp.ui.*
import dev.zacsweers.metro.createGraphFactory
import github.naixx.network.Clip
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

/**
 * The compiler proves that every binding resolves, not which classes made it into the multibinding maps: a view model
 * that lost its `@ViewModelKey @ContributesIntoMap` still compiles and fails at first use ("Unknown model class"). So
 * the real graph is built here and the maps are read.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class AppGraphTest {

    private val graph: AppGraph =
        createGraphFactory<AppGraph.Factory>().create(ApplicationProvider.getApplicationContext<Application>())

    @Test
    fun `every view model is in the factory's maps`() {
        assertEquals(
            setOf(MainViewModel::class, ProgramViewModel::class, ClipsViewModel::class),
            graph.viewModelProviders.keys,
        )
        assertEquals(setOf(TimelapseViewModel.Factory::class), graph.manualAssistedFactoryProviders.keys)
        assertEquals(emptySet(), graph.assistedFactoryProviders.keys)
    }

    @Test
    fun `the factory builds a fresh, unscoped view model per call`() {
        val factory = graph.metroViewModelFactory
        val first = factory.create(MainViewModel::class, CreationExtras.Empty)
        val second = factory.create(MainViewModel::class, CreationExtras.Empty)
        assertIs<MainViewModel>(first)
        assertNotSame(first, second) // the ViewModelStore, not the graph, owns the lifetime
    }

    @Test
    fun `a per-clip view model comes from its manual assisted factory`() {
        val timelapse = factory<TimelapseViewModel.Factory>().create(Clip(index = 0, id = 1, name = "tl-1", frames = 3, imageBase64 = ""))
        assertIs<TimelapseViewModel>(timelapse)
    }

    @Test
    fun `the Activity and the service are built by the graph`() {
        assertEquals(setOf(MainActivity::class), graph.activityProviders.keys)
        assertEquals(setOf(WebSocketService::class), graph.serviceProviders.keys)
    }

    private inline fun <reified F : dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory> factory(): F =
        graph.metroViewModelFactory.createManuallyAssistedFactory(F::class)()
}
