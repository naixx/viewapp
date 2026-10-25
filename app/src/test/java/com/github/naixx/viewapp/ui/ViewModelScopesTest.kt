package com.github.naixx.viewapp.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * whole Activity, `screenViewModel()` is one per Navigation 3 entry and is cleared when the entry leaves the back stack —
 * the decorators MainScreen uses, over a two-entry back stack. The factory is a stub, so this is about the scopes, not
 * the app graph.
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

    private data object First : NavKey
    private data object Second : NavKey

    /** A screen that records the instances it was given; one per entry. */
    private class Probe {

        var shared: SharedViewModel? = null
        var own: PerScreenViewModel? = null

        @Composable
        fun Content() {
            shared = activityViewModel<SharedViewModel>()
            own = screenViewModel<PerScreenViewModel>()
        }
    }

    private val first = Probe()
    private val second = Probe()
    private lateinit var backStack: SnapshotStateList<NavKey>

    /** Shows [first], then pushes [second] on top of it. */
    private fun showBoth() {
        compose.setContent {
            backStack = remember { mutableStateListOf<NavKey>(First) }
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides factory,
                LocalActivityViewModelStoreOwner provides compose.activity, // what MainActivity does
            ) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator<NavKey>(LocalActivityViewModelStoreOwner.current),
                    ),
                    entryProvider = entryProvider {
                        entry<First> { first.Content() }
                        entry<Second> { second.Content() }
                    },
                )
            }
        }
        compose.waitForIdle()
        compose.runOnUiThread { backStack.add(Second) }
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
        compose.runOnUiThread { backStack.removeLastOrNull() }
        compose.waitForIdle()
        assertTrue(second.own!!.cleared, "the popped screen's view model is cleared")
        assertFalse(first.own!!.cleared, "the screen underneath keeps its own")
        assertFalse(first.shared!!.cleared, "the Activity-scoped one lives on")
    }
}
