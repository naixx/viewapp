package com.github.naixx.viewapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.naixx.viewapp.ui.theme.ViewAppTheme

/**
 * A single-choice segmented control — the faithful equivalent of the original web app's `ion-segment`.
 * Reusable for any small, mutually-exclusive option set (mode tabs, ramp parameters, …).
 *
 * @param label maps an option to its displayed text. Pass `{ it.value.toString() }` (or a custom
 *   short form) when the full [Opt.label] is too long to fit a segment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedControl(
    options: List<Opt<T>>,
    selected: T?,
    modifier: Modifier = Modifier,
    label: (Opt<T>) -> String = { it.label },
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, opt ->
            SegmentedButton(
                selected = opt.value == selected,
                onClick = { onSelect(opt.value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = { Text(label(opt), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun SegmentedControlPreview() {
    ViewAppTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SegmentedControl(ProgramLists.rampModes, selected = "auto") {}
            SegmentedControl(ProgramLists.rampParameters, selected = "S+I", label = { it.value }) {}
            SegmentedControl(ProgramLists.intervalModes, selected = "fixed") {}
        }
    }
}
