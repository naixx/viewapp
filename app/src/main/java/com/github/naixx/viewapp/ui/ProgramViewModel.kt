package com.github.naixx.viewapp.ui

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import github.naixx.network.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonObject

/**
 * Holds the editable [ProgramDraft] for the Program tab and builds the outgoing program-control
 * messages. Editing happens on the raw program JsonObject (see [ProgramDraft]) so unmodeled fields
 * (motion axes, keyframes, …) survive the round-trip.
 *
 * Sends are not performed here — [com.github.naixx.viewapp.ui.MainViewModel.send] is the single
 * outgoing channel; the screen wires the built messages to it. The screens take it with
 * `activityViewModel<ProgramViewModel>()` — the Activity's store, so they share one.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ProgramViewModel : ViewModel() {

    val draft: StateFlow<ProgramDraft?>
        field = MutableStateFlow<ProgramDraft?>(null)

    private var awaitingReconfigure = false

    /**
     * Called whenever a `timelapseProgram` arrives from the device. Seeds the draft on first load
     * and after a reconfigure round-trip (which is how the device fills eclipse plans / durations);
     * otherwise leaves in-progress local edits intact.
     */
    fun onDeviceProgram(raw: JsonObject?) {
        if (raw == null) return
        if (draft.value == null || awaitingReconfigure) {
            draft.value = ProgramDraft(raw)
            awaitingReconfigure = false
        }
    }

    fun update(transform: (ProgramDraft) -> ProgramDraft) {
        draft.value = draft.value?.let(transform)
    }

    /**
     * Always carries the start delay shown on the delay button (1 s unless picked). A program without one, as the
     * VIEW has right after a reboot, never started: the core waits out the delay in 60 s steps and `undefined`
     * is never reached (`intervalometer.js` delayed()). The VIEW's own menu sets it to 1 before a start.
     */
    fun runMessage(): OutMessage? = draft.value?.let { Run(it.set("delay", it.delay).json, runDateMillis()) }

    fun saveMessage(): OutMessage? = draft.value?.let { SaveProgram(it.json) }

    /** Build a reconfigure message and mark that we expect the device to echo a recomputed program. */
    fun reconfigureMessage(): OutMessage? {
        awaitingReconfigure = true
        return draft.value?.let { ReconfigureProgram(it.json) }
    }
}
