package io.github.damson.hardwareinsets.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.damson.hardwareinsets.CutoutMode
import io.github.damson.hardwareinsets.CutoutShape
import io.github.damson.hardwareinsets.ScreenEdge

/**
 * Every parameter the library takes, as a Material control, over a readout of
 * what the platform is reporting while you change it.
 *
 * The content of a modal sheet rather than a pane on the screen. A sheet that
 * stays put covers the bottom edge, and the bottom edge is one of the four this
 * screen exists to put things on: setting the anchor to `BOTTOM` would hide the
 * result behind the control that asked for it.
 */
@Composable
fun ControlSheetContent(
    cutout: CutoutShape,
    options: ViewerOptions,
    onOptions: (ViewerOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Scrollable, because the sheet opens half height and a large font setting
    // pushes the last switches past the bottom of it. Without this they cannot
    // be reached at all, and nothing about the layout says so.
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            stringResource(R.string.controls),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Legend(cutout)
        Sections(options, onOptions)
    }
}

/**
 * What the two colours over the picture mean, and what the platform is
 * currently reporting. Without the first, the markers are two mystery shapes.
 */
@Composable
private fun Legend(cutout: CutoutShape) {
    val rects = if (cutout.bounds.isEmpty()) {
        stringResource(R.string.no_cutout)
    } else {
        cutout.bounds.joinToString("   ") { "${it.width()}x${it.height()} at ${it.left},${it.top}" }
    }
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Swatch(CutoutMarker, stringResource(R.string.legend_rects), rects)
        Swatch(
            InsetMarker,
            stringResource(R.string.legend_depths),
            "T ${cutout.topInset}   B ${cutout.bottomInset}   " +
                "L ${cutout.leftInset}   R ${cutout.rightInset}",
        )
    }
    HorizontalDivider(
        Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun Swatch(color: Color, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(12.dp).background(color, MaterialTheme.shapes.extraSmall))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(76.dp),
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Sections(options: ViewerOptions, onOptions: (ViewerOptions) -> Unit) {
    Choice(
        label = stringResource(R.string.anchor),
        entries = ScreenEdge.entries.map { it.name.take(1) + it.name.drop(1).lowercase() },
        selectedIndex = ScreenEdge.entries.indexOf(options.anchor),
        onSelect = { onOptions(options.copy(anchor = ScreenEdge.entries[it])) },
    )
    Choice(
        label = stringResource(R.string.corner),
        entries = listOf(stringResource(R.string.start), stringResource(R.string.end)),
        selectedIndex = if (options.isCornerAtTheEnd) 1 else 0,
        onSelect = { onOptions(options.copy(isCornerAtTheEnd = it == 1)) },
    )
    Choice(
        label = stringResource(R.string.cutout_mode),
        entries = listOf("Always", "Short", "Default", "Never"),
        selectedIndex = CutoutMode.entries.indexOf(options.cutoutMode),
        onSelect = { onOptions(options.copy(cutoutMode = CutoutMode.entries[it])) },
    )

    Label(stringResource(R.string.counts_as_hardware))
    val policy = options.policy
    Setting(
        R.string.cutout, R.string.cutout_detail, policy.isCutoutIncluded,
    ) { onOptions(options.copy(policy = policy.copy(isCutoutIncluded = it))) }
    Setting(
        R.string.waterfall, R.string.waterfall_detail, policy.isWaterfallIncluded,
    ) { onOptions(options.copy(policy = policy.copy(isWaterfallIncluded = it))) }
    Setting(
        R.string.system_bars, R.string.system_bars_detail, policy.areSystemBarsIncluded,
    ) { onOptions(options.copy(policy = policy.copy(areSystemBarsIncluded = it))) }
    Setting(
        R.string.far_edge_ignored, R.string.far_edge_detail, options.isFarEdgeIgnored,
    ) { onOptions(options.copy(isFarEdgeIgnored = it)) }

    Label(stringResource(R.string.window))
    Setting(
        R.string.hide_bars, R.string.hide_bars_detail, options.areBarsHidden,
    ) { onOptions(options.copy(areBarsHidden = it)) }
    Setting(
        R.string.markers, R.string.markers_detail, options.areMarkersShown,
    ) { onOptions(options.copy(areMarkersShown = it)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(label: String, entries: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        // A segmented button, not a row of chips: these are one-of-many, and a
        // chip's selected state does not say so.
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, entries.size),
                    label = { Text(entry, maxLines = 1, textAlign = TextAlign.Center) },
                )
            }
        }
    }
}

/**
 * A settings row in the shape a real app uses: the option, what it does, and a
 * switch, with the whole row as the target rather than the switch alone.
 */
@Composable
private fun Setting(label: Int, detail: Int, isOn: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        supportingContent = { Text(stringResource(detail)) },
        trailingContent = { Switch(checked = isOn, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(
            value = isOn,
            role = Role.Switch,
            onValueChange = onChange,
        ),
    )
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}
