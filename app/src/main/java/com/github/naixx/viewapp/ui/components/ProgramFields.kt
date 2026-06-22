package com.github.naixx.viewapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.naixx.viewapp.ui.theme.ViewAppTheme
import kotlin.math.floor

/**
 * Vertical padding applied to every [FieldRow], controlling form density. Default is compact;
 * override per-section, e.g. `CompositionLocalProvider(LocalFieldVerticalPadding provides 6.dp) { … }`.
 */
val LocalFieldVerticalPadding = compositionLocalOf { 2.dp }

/** Label on the left, a tappable value button on the right that opens a single-choice dialog. */
@Composable
fun <T> SelectRow(
    label: String,
    options: List<Opt<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.value == selected }?.label ?: "—"
    FieldRow(label) { VSmallButton(current) { open = true } }
    if (open) {
        SelectDialog(label, options, selected, onSelect = { onSelect(it); open = false }, onDismiss = { open = false })
    }
}

/** Label on the left, a numeric value button on the right that opens a number-input dialog. */
@Composable
fun NumberRow(
    label: String,
    value: Double,
    suffix: String = "",
    decimals: Int = 1,
    onConfirm: (Double) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val shown = if (value % 1.0 == 0.0) value.toInt().toString() else "%.${decimals}f".format(value)
    FieldRow(label) { VSmallButton("$shown$suffix") { open = true } }
    if (open) {
        NumberInputDialog(label, value, onConfirm = { onConfirm(it); open = false }, onDismiss = { open = false })
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    FieldRow(label) { Switch(checked = checked, onCheckedChange = onChange) }
}

/**
 * A camera exposure-EV-limit select (Max/Min ISO, aperture, Max shutter). [options] carry the
 * camera's supported EV values plus a `null` "No limit" entry. The current value is matched
 * tolerantly (`floor(ev*10)`, like the original's `mapEv`) so stored limits resolve to a label.
 */
@Composable
fun EvLimitRow(
    label: String,
    options: List<Opt<Double?>>,
    selected: Double?,
    onSelect: (Double?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = if (selected == null) "No limit"
    else options.firstOrNull { it.value != null && evMatches(it.value, selected) }?.label ?: selected.toString()
    FieldRow(label) { VSmallButton(current) { open = true } }
    if (open) {
        val matched = options.firstOrNull { evMatches(it.value, selected) }?.value
        SelectDialog(
            label, options, matched,
            onSelect = { onSelect(it); open = false },
            onDismiss = { open = false },
        )
    }
}

private fun evMatches(a: Double?, b: Double?): Boolean =
    if (a == null || b == null) a == b else floor(a * 10) == floor(b * 10)

/** A row of selectable chips (the original's `ion-segment`). */
@Composable
fun <T> ChipSelector(
    options: List<Opt<T>>,
    selected: T?,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt.value == selected,
                onClick = { onSelect(opt.value) },
                label = { Text(opt.label) },
            )
        }
    }
}

@Composable
fun FieldRow(label: String, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = LocalFieldVerticalPadding.current),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        control()
    }
}

@Composable
fun <T> SelectDialog(
    title: String,
    options: List<Opt<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(opt.value) }.padding(vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = opt.value == selected, onClick = { onSelect(opt.value) })
                        Spacer(Modifier.width(8.dp))
                        Text(opt.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun NumberInputDialog(
    title: String,
    initial: Double,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialText = if (initial % 1.0 == 0.0) initial.toInt().toString() else initial.toString()
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { text.trim().toDoubleOrNull()?.let(onConfirm) ?: onDismiss() },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Describes an in-progress live ramp (a `dynamicChange` entry) for display in [LiveUpdateDialog]. */
data class RampInfo(val from: Double, val to: Double, val startFrame: Int, val endFrame: Int)

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

/**
 * The original's live "Update" modal: pick a new value and how many frames to ramp it over, then
 * apply it as a dynamicChange. [rampable] hides the frames selector for immediate-only params. When
 * [active] is non-null it shows the in-progress ramp (from → to) and its progress, matching the
 * original ("currently ramping to … over the next N frames").
 */
@Composable
fun LiveUpdateDialog(
    title: String,
    initial: Double,
    rampable: Boolean,
    active: RampInfo? = null,
    currentFrame: Int = 0,
    onApply: (newValue: Double, frames: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(fmt(initial)) }
    var frames by remember { mutableStateOf(if (rampable) 30 else 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (active != null && active.endFrame > active.startFrame) {
                    val remaining = (active.endFrame - currentFrame).coerceAtLeast(0)
                    val progress = ((currentFrame - active.startFrame).toFloat() /
                        (active.endFrame - active.startFrame)).coerceIn(0f, 1f)
                    Text(
                        "Currently ramping from ${fmt(active.from)} to ${fmt(active.to)} over the next " +
                            "$remaining frames. A new change replaces this schedule.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("New value") },
                )
                if (rampable) {
                    Spacer(Modifier.height(12.dp))
                    Text("Apply changes", style = MaterialTheme.typography.labelMedium)
                    // Vertical list so every option is visible at once (no horizontal scrolling).
                    ProgramLists.rampFrames.forEach { opt ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { frames = opt.value }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = frames == opt.value, onClick = { frames = opt.value })
                            Spacer(Modifier.width(8.dp))
                            Text(opt.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { text.trim().toDoubleOrNull()?.let { onApply(it, frames) } ?: onDismiss() }) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * A compact button that opens [LiveUpdateDialog] to ramp a value live (used in the running Status UI).
 * Shows a red border while [active] (an in-progress ramp for this parameter) is non-null.
 */
@Composable
fun LiveEditButton(
    text: String,
    title: String,
    current: Double,
    rampable: Boolean,
    active: RampInfo? = null,
    currentFrame: Int = 0,
    modifier: Modifier = Modifier,
    onApply: (newValue: Double, frames: Int) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val border = if (active != null) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null
    VSmallButton(text, modifier = modifier, border = border) { open = true }
    if (open) {
        LiveUpdateDialog(
            title = title,
            initial = current,
            rampable = rampable,
            active = active,
            currentFrame = currentFrame,
            onApply = { v, f -> onApply(v, f); open = false },
            onDismiss = { open = false },
        )
    }
}

/** A compact button that opens [SelectDialog] to change an option live (e.g. ramp/interval mode). */
@Composable
fun LiveSelectButton(
    text: String,
    title: String,
    options: List<Opt<String>>,
    current: String?,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    VSmallButton(text, modifier = modifier) { open = true }
    if (open) {
        SelectDialog(
            title = title,
            options = options,
            selected = current,
            onSelect = { onSelect(it); open = false },
            onDismiss = { open = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChipSelectorPreview() {
    ViewAppTheme {
        Box(Modifier.padding(16.dp)) {
            ChipSelector(
                options = ProgramLists.rampModes,
                selected = "auto",
                onSelect = {}
            )
        }
    }
}
