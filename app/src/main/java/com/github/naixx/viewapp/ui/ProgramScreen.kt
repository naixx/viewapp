package com.github.naixx.viewapp.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import com.github.naixx.viewapp.ui.components.ChipSelector
import com.github.naixx.viewapp.ui.components.EvLimitRow
import com.github.naixx.viewapp.ui.components.FieldRow
import com.github.naixx.viewapp.ui.components.LiveUpdateDialog
import com.github.naixx.viewapp.ui.components.NumberRow
import com.github.naixx.viewapp.ui.components.OnConnectedEffect
import com.github.naixx.viewapp.ui.components.Opt
import com.github.naixx.viewapp.ui.components.ProgramLists
import com.github.naixx.viewapp.ui.components.SegmentedControl
import com.github.naixx.viewapp.ui.components.SelectDialog
import com.github.naixx.viewapp.ui.components.SelectRow
import com.github.naixx.viewapp.ui.components.ToggleRow
import com.github.naixx.viewapp.ui.components.VButton
import com.github.naixx.viewapp.ui.components.VSmallButton
import com.github.naixx.viewapp.ui.components.cameraSettings
import com.github.naixx.viewapp.ui.components.sampleEclipseRaw
import com.github.naixx.viewapp.ui.components.sampleFixedRaw
import com.github.naixx.viewapp.ui.components.sampleProgramRaw
import com.github.naixx.viewapp.ui.components.evMatches
import com.github.naixx.viewapp.ui.components.status
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import com.github.naixx.viewapp.utils.activityViewModel
import com.github.naixx.viewapp.utils.formatted
import github.naixx.network.CameraDetails
import github.naixx.network.CameraSettings
import github.naixx.network.DynamicChange
import github.naixx.network.Get
import github.naixx.network.OutMessage
import github.naixx.network.ProgramDraft
import github.naixx.network.Status
import github.naixx.network.Stop
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// The program editor (idle state) + its mode sections. Rendered by StatusScreen, which is the single
// combined Status/Program screen (mirrors the original IntervalometerPage). The running state and live
// dynamic-change controls live in StatusScreen.
@Composable
internal fun ProgramEditor(
    d: ProgramDraft,
    vm: ProgramViewModel,
    settings: CameraSettings?,
    send: (OutMessage) -> Unit,
) {
    Text("New time-lapse", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    // Mode tabs — switching mode reconfigures on the device (fills eclipse plans / durations).
    ChipSelector(ProgramLists.rampModes, d.rampMode) { mode ->
        vm.update { it.set("rampMode", mode) }
        vm.reconfigureMessage()?.let(send)
    }
    Spacer(Modifier.height(12.dp))

    when (d.rampMode) {
        "auto" -> AutoRampingSection(d, vm, settings)
        "eclipse" -> EclipseSection(d, vm, send)
        else -> BasicFixedSection(d, vm)
    }

    if (d.rampMode != "eclipse") {
        FramesAndSchedule(d, vm)
    }

    Spacer(Modifier.height(16.dp))
    StartStopButtons(d, vm, send)
}

@Composable
private fun BasicFixedSection(d: ProgramDraft, vm: ProgramViewModel) {
    SectionHeader("Interval")
    IntervalRows(d, vm, showModeChips = false)
}

@Composable
private fun AutoRampingSection(d: ProgramDraft, vm: ProgramViewModel, settings: CameraSettings?) {
    // Like the original: a single row summarising ramping and opening a dialog for the details.
    RampingOptionsRow(d, vm, settings)

    SectionHeader("Interval")
    IntervalRows(d, vm, showModeChips = true)

    SelectRow("Bracketing", ProgramLists.bracketing, if (d.hdrCount < 2) 1 else 3) {
        vm.update { p -> p.set("hdrCount", it) }
    }
    if (d.hdrCount > 1) {
        SelectRow("Bracketing Frames", ProgramLists.bracketingFrames, d.hdrCount) {
            vm.update { p -> p.set("hdrCount", it) }
        }
        SelectRow("Stops / Frame", ProgramLists.bracketingStops, d.hdrStops) {
            vm.update { p -> p.set("hdrStops", it) }
        }
    }
}

/** Summary row ("Ramping Options") that opens the ramping dialog — mirrors `RampingModalContentPage`. */
@Composable
private fun RampingOptionsRow(d: ProgramDraft, vm: ProgramViewModel, settings: CameraSettings?) {
    var open by remember { mutableStateOf(false) }
    FieldRow("Ramping Options") { VSmallButton(rampingSummary(d)) { open = true } }
    if (open) {
        RampingOptionsDialog(
            d, settings?.details,
            onChange = { key, value -> vm.update { p -> p.set(key, value) } },
            onDismiss = { open = false },
        )
    }
}

/** The ramping options button text: the ramped parameters, plus "HL" with highlight protection on. */
internal fun rampingSummary(d: ProgramDraft): String = d.rampParameters + if (d.highlightProtection) ", HL" else ""

/**
 * The program's exposure limits by the camera's names, e.g. "Limits: ISO 400–6400, aperture f/1.4–f/8, shutter up to 20"".
 * The EVs follow the VIEW (`camera.js` setEv lists): ISO from isoMin up to isoMax, aperture from apertureMin
 * (widest) up to apertureMax (narrowest), shutter up to shutterMax. A value missing from the lists shows as its EV.
 */
internal fun rampLimitsLine(d: ProgramDraft, details: CameraDetails?): String {
    fun name(ev: Double?, entries: List<Pair<String, Double?>>?): String? =
        ev?.let { e -> entries?.firstOrNull { evMatches(it.second, e) }?.first ?: "EV ${e.formatted(1)}" }

    fun range(label: String, from: String?, upTo: String?): String? = when {
        from != null && upTo != null -> "$label $from–$upTo"
        upTo != null -> "$label up to $upTo"
        from != null -> "$label from $from"
        else -> null
    }

    val iso = details?.iso?.list?.map { it.name to it.ev }
    val aperture = details?.aperture?.list?.map { "f/${it.name}" to it.ev }
    val shutter = details?.shutter?.list?.map { it.name to it.ev }
    val limits = listOfNotNull(
        range("ISO", name(d.isoMin, iso), name(d.isoMax, iso)),
        range("aperture", name(d.apertureMin, aperture), name(d.apertureMax, aperture)),
        range("shutter", null, name(d.shutterMax, shutter)),
    )
    return "Limits: " + limits.ifEmpty { listOf("none") }.joinToString()
}

/** Build EV-limit options from a camera option list: "No limit" + each supported (name, ev). */
private fun evLimitOptions(entries: List<Pair<String, Double?>>): List<Opt<Double?>> =
    listOf(Opt<Double?>("No limit", null)) + entries.mapNotNull { (name, ev) -> ev?.let { Opt<Double?>(name, it) } }

/**
 * The ramping options of [d]; every edit goes to [onChange] as a program key and value. [live] is for a running
 * time-lapse: the VIEW reads luminance and highlight protection only when it starts, so they are shown, not edited.
 */
@Composable
internal fun RampingOptionsDialog(
    d: ProgramDraft,
    details: CameraDetails?,
    live: Boolean = false,
    onChange: (key: String, value: JsonElement) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ramping Options") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                RampingOptionsContent(d, details, live, onChange)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun RampingOptionsContent(
    d: ProgramDraft,
    details: CameraDetails?,
    live: Boolean,
    onChange: (key: String, value: JsonElement) -> Unit,
) {
    val isoOpts = evLimitOptions(details?.iso?.list?.map { it.name to it.ev } ?: emptyList())
    val apertureOpts = evLimitOptions(details?.aperture?.list?.map { it.name to it.ev } ?: emptyList())
    val shutterOpts = evLimitOptions(details?.shutter?.list?.map { it.name to it.ev } ?: emptyList())
    if (live) {
        Text("Changes apply from the next frame.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
    }
    Text("Ramp Parameters", style = MaterialTheme.typography.bodyMedium)
    Text(
        ProgramLists.rampParameters.firstOrNull { it.value == d.rampParameters }?.label ?: "",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(Modifier.height(4.dp))
    SegmentedControl(ProgramLists.rampParameters, d.rampParameters, label = { it.value }) {
        onChange("rampParameters", JsonPrimitive(it))
    }
    Spacer(Modifier.height(4.dp))
    if (live) {
        FieldRow("Day Luminance") { Text(ProgramLists.dayLuminance.labelOf(d.dayLuminance)) }
        FieldRow("Night Luminance") { Text(ProgramLists.nightLuminance.labelOf(d.nightLuminance)) }
        FieldRow("Highlight Protection") { Text(if (d.highlightProtection) "On" else "Off") }
        Text("Set when the time-lapse starts.", style = MaterialTheme.typography.bodySmall)
    } else {
        SelectRow("Day Luminance", ProgramLists.dayLuminance, d.dayLuminance) {
            onChange("dayLuminance", JsonPrimitive(it))
        }
        SelectRow("Night Luminance", ProgramLists.nightLuminance, d.nightLuminance) {
            onChange("nightLuminance", JsonPrimitive(it))
        }
        ToggleRow("Highlight Protection", d.highlightProtection) {
            onChange("highlightProtection", JsonPrimitive(it))
        }
    }
    if (isoOpts.size > 1) {
        EvLimitRow("Maximum ISO", isoOpts, d.isoMax) { onChange("isoMax", JsonPrimitive(it)) }
        EvLimitRow("Minimum ISO", isoOpts, d.isoMin) { onChange("isoMin", JsonPrimitive(it)) }
    }
    if (apertureOpts.size > 1) {
        EvLimitRow("Maximum Aperture", apertureOpts, d.apertureMax) { onChange("apertureMax", JsonPrimitive(it)) }
        EvLimitRow("Minimum Aperture", apertureOpts, d.apertureMin) { onChange("apertureMin", JsonPrimitive(it)) }
    }
    if (shutterOpts.size > 1) {
        EvLimitRow("Maximum Shutter", shutterOpts, d.shutterMax) { onChange("shutterMax", JsonPrimitive(it)) }
    }
}

private fun <T> List<Opt<T>>.labelOf(value: T): String = firstOrNull { it.value == value }?.label ?: value.toString()

/** Interval settings shared by auto & fixed. Auto adds a fixed/auto/aux mode selector. */
@Composable
private fun IntervalRows(d: ProgramDraft, vm: ProgramViewModel, showModeChips: Boolean) {
    if (showModeChips) {
        ChipSelector(ProgramLists.intervalModes, d.intervalMode) { mode ->
            vm.update { it.set("intervalMode", mode) }
        }
        Spacer(Modifier.height(4.dp))
    }
    if (d.rampMode == "auto" && d.intervalMode == "auto") {
        NumberRow("Day Interval", d.dayInterval, suffix = " s") { vm.update { p -> p.set("dayInterval", it) } }
        NumberRow("Night Interval", d.nightInterval, suffix = " s") { vm.update { p -> p.set("nightInterval", it) } }
    } else {
        NumberRow("Interval", d.interval.toDoubleOrNull() ?: 5.0, suffix = " s") {
            vm.update { p -> p.set("interval", intString(it)) }
        }
    }
}

@Composable
private fun FramesAndSchedule(d: ProgramDraft, vm: ProgramViewModel) {
    ToggleRow("Enable Scheduling", d.scheduled) { vm.update { p -> p.set("scheduled", it) } }

    val fixedInterval = d.rampMode == "fixed" || d.intervalMode == "fixed"
    if (!d.scheduled) {
        if (fixedInterval) {
            NumberRow("Frames", d.frames.toDouble(), decimals = 0) { vm.update { p -> p.set("frames", it.toInt()) } }
        } else {
//            FieldRow("Frames") { Text("until stopped", style = MaterialTheme.typography.bodyMedium) }
        }
    }
    AnimatedContent(targetState = d.scheduled, label = "schedule") { scheduled ->
        if (scheduled) {
            Column {
                SectionHeader("Schedule")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProgramLists.weekDays.forEach { day ->
                        FilterChip(
                            selected = d.schedDay(day),
                            onClick = { vm.update { p -> p.set("sched$day", !d.schedDay(day)) } },
                            label = { Text(day.take(2)) }
                        )
                    }
                }
                SelectRow("Daily Start Time", ProgramLists.timeOfDay, d.schedStart) { vm.update { p -> p.set("schedStart", it) } }
                SelectRow("Daily Stop Time", ProgramLists.timeOfDay, d.schedStop) { vm.update { p -> p.set("schedStop", it) } }
            }
        } else {
            Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
private fun EclipseSection(d: ProgramDraft, vm: ProgramViewModel, send: (OutMessage) -> Unit) {
    d.eclipseInfo?.let {
        Text(it, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
    }
    VButton("Recalculate plans") { vm.reconfigureMessage()?.let(send) }
    Spacer(Modifier.height(8.dp))

    val plans = d.plans
    if (plans.isEmpty()) {
        Text(
            "No eclipse plans yet. Set the location/date on the device, then Recalculate.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    plans.forEachIndexed { i, plan ->
        val name = (plan["name"] as? JsonPrimitive)?.content ?: "Plan ${i + 1}"
        SectionHeader(name)
        val mode = (plan["mode"] as? JsonPrimitive)?.content
        SelectRow("Exposure Mode", ProgramLists.exposureMode, mode) {
            vm.update { p -> p.editPlan(i, "mode", JsonPrimitive(it)) }
        }
        val interval = (plan["interval"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 12.0
        NumberRow("Interval", interval, suffix = " s") {
            vm.update { p -> p.editPlan(i, "interval", JsonPrimitive(it.toInt())) }
        }
    }
}

@Composable
private fun StartStopButtons(d: ProgramDraft, vm: ProgramViewModel, send: (OutMessage) -> Unit) {
    var delayOpen by remember { mutableStateOf(false) }
    // START centered as the prominent CTA (like the original); delay sits left, Save right.
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        VSmallButton("delay ${delayLabel(d.delay)}", modifier = Modifier.align(Alignment.CenterStart)) {
            delayOpen = true
        }
        VButton(
            "START",
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            vm.runMessage()?.let(send)
        }
        VSmallButton("Save", modifier = Modifier.align(Alignment.CenterEnd)) {
            vm.saveMessage()?.let(send)
        }
    }
    if (delayOpen) {
        SelectDialog(
            title = "Delay Start",
            options = ProgramLists.delays,
            selected = d.delay,
            onSelect = { vm.update { p -> p.set("delay", it) }; delayOpen = false },
            onDismiss = { delayOpen = false },
        )
    }
}

private fun delayLabel(seconds: Int): String = when {
    seconds <= 0      -> "none"
    seconds % 3600 == 0 -> "${seconds / 3600}h"
    seconds % 60 == 0 -> "${seconds / 60}m"
    else              -> "${seconds}s"
}

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.titleSmall)
    HorizontalDivider()
    Spacer(Modifier.height(2.dp))
}

private fun intString(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

// --- Previews ---

@Composable
private fun EditorPreview(raw: JsonObject) {
    val vm = remember { ProgramViewModel().apply { onDeviceProgram(raw) } }
    val d = vm.draft.collectAsState().value ?: return
    ViewAppTheme {
        Surface {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                ProgramEditor(d, vm, cameraSettings) {}
            }
        }
    }
}

@Preview(name = "Editor — Auto Ramping", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun ProgramEditorAutoPreview() = EditorPreview(sampleProgramRaw)

@Preview(name = "Ramping Options dialog", showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun RampingOptionsDialogPreview() {
    val vm = remember { ProgramViewModel().apply { onDeviceProgram(sampleProgramRaw) } }
    val d = vm.draft.collectAsState().value ?: return
    ViewAppTheme { RampingOptionsDialog(d, cameraSettings.details, onChange = { _, _ -> }, onDismiss = {}) }
}

@Preview(name = "Ramping Options — running", showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun RampingOptionsLivePreview() {
    ViewAppTheme {
        Surface {
            Column(Modifier.padding(16.dp)) {
                RampingOptionsContent(ProgramDraft(sampleProgramRaw), cameraSettings.details, live = true) { _, _ -> }
            }
        }
    }
}

@Preview(name = "Editor — Basic Fixed", showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun ProgramEditorFixedPreview() = EditorPreview(sampleFixedRaw)

@Preview(name = "Editor — Eclipse", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun ProgramEditorEclipsePreview() = EditorPreview(sampleEclipseRaw)

@Preview(name = "Editor — Scheduled", showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun ProgramEditorScheduledPreview() =
    EditorPreview(ProgramDraft(sampleFixedRaw).set("scheduled", true).json)
