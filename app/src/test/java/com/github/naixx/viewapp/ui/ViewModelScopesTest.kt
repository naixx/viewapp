package com.github.naixx.viewapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.github.naixx.viewapp.utils.LocalActivityViewModelStoreOwner
import com.github.naixx.viewapp.utils.activityViewModel
import com.github.naixx.viewapp.utils.screenViewModel
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.reflect.KClass
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The two view-model scopes the screens name (`:ui`, `utils/ViewModels.kt`): `activityViewModel()` is one instance for the
 * whole Activity, `screenViewModel()` is one per Voyager screen and is cleared when the screen leaves the navigator
 * (what `rememberScreenModel` used to give). The factory is a stub, so this is about the scopes, not the app graph
 * (`AppGraphTest` covers that).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class ViewModelScopesTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    open class TrackedViewModel : ViewModel() {

        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    class SharedViewModel : TrackedViewModel()

    class PerScreenViewModel : TrackedViewModel()

    private val factory = object : MetroViewModelFactory() {
        override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel> = mapOf(
            SharedViewModel::class to { SharedViewModel() },
            PerScreenViewModel::class to { PerScreenViewModel() },
        )
    }

    /** A screen that records the instances it was given; two with different keys stand for two screens. */
    private class Probe(override val key: ScreenKey) : Screen {

        var shared: SharedViewModel? = null
        var own: PerScreenViewModel? = null

        @Composable
        override fun Content() {
            shared = activityViewModel<SharedViewModel>()
            own = screenViewModel<PerScreenViewModel>()
        }
    }

    private val first = Probe("first")
    private val second = Probe("second")
    private lateinit var navigator: Navigator

    /** Shows [first], then pushes [second] on top of it. */
    private fun showBoth() {
        compose.setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides factory,
                LocalActivityViewModelStoreOwner provides compose.activity, // what MainActivity does
            ) {
                Navigator(first) { nav ->
                    navigator = nav
                    CurrentScreen()
                }
            }
        }
        compose.waitForIdle()
        compose.runOnUiThread { navigator.push(second) }
        compose.waitForIdle()
    }

    @Test
    fun `activityViewModel is the same instance on every screen`() {
        showBoth()
        assertNotNull(first.shared)
        assertSame(first.shared, second.shared)
    }

    @Test
    fun `screenViewModel is a separate instance per screen`() {
        showBoth()
        assertNotNull(first.own)
        assertNotNull(second.own)
        assertNotSame(first.own, second.own)
    }

    @Test
    fun `popping a screen clears its own view model and keeps the shared one`() {
        showBoth()
        compose.runOnUiThread { navigator.pop() }
        compose.waitForIdle()
        assertTrue(second.own!!.cleared, "the popped screen's view model is cleared")
        assertFalse(first.own!!.cleared, "the screen underneath keeps its own")
        assertFalse(first.shared!!.cleared, "the Activity-scoped one lives on")
    }
}
