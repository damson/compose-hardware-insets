package com.devddagnet.hardwareinsets.sample.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devddagnet.hardwareinsets.lib.domain.CutoutMode
import com.devddagnet.hardwareinsets.lib.domain.CutoutShape
import com.devddagnet.hardwareinsets.lib.domain.ScreenEdge
import com.devddagnet.hardwareinsets.sample.R
import com.devddagnet.hardwareinsets.sample.model.ViewerOptions

/**
 * The sheet the controls arrive on, which is a window of its own and so carries
 * its own colours rather than the screen's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ControlSheet(
    cutout: CutoutShape,
    options: ViewerOptions,
    onOptionsChange: (ViewerOptions) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // Half height first, so a change to a top-anchored control is visible
        // while it is being made. Drag up for the rest.
        sheetState = rememberModalBottomSheetState(),
        // No scrim: every control on this sheet changes the screen behind it,
        // so dimming the plate to ask about the plate hides the answer.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SHEET_ALPHA),
        // `contentColorFor` answers `Unspecified` for a colour that is not a
        // scheme role, and the text would then take whatever is ambient.
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = Color.Transparent,
    ) {
        ControlSheetContent(cutout, options, onOptionsChange)
    }
}

/** How solid the sheet's frame is. It can be glass because no text sits on it. */
private const val SHEET_ALPHA = 0.55f

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
    onOptionsChange: (ViewerOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = CARD_ALPHA),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        // Scrollable because at a large font setting the last switches fall
        // past the bottom of a half-height sheet, out of reach with nothing to
        // say so.
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            Text(
                stringResource(R.string.controls),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
            )
            Text(
                stringResource(R.string.controls_intro),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
            )
            Legend(cutout)
            LabelSettings(options, onOptionsChange)
            HardwareSettings(options, onOptionsChange)
            WindowSettings(options, onOptionsChange)
        }
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
            "T${cutout.topInset}  B${cutout.bottomInset}  " +
                "L${cutout.leftInset}  R${cutout.rightInset}",
        )
        Text(
            stringResource(R.string.legend_detail),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
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
            modifier = Modifier.width(112.dp),
        )
        Text(
            value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Where the label goes, and whether the hardware is drawn over the plate. */
@Composable
private fun LabelSettings(options: ViewerOptions, onOptionsChange: (ViewerOptions) -> Unit) {
    Setting(
        R.string.markers, R.string.markers_detail, options.areMarkersShown,
    ) { onOptionsChange(options.copy(areMarkersShown = it)) }

    Choice(
        label = stringResource(R.string.anchor),
        detail = stringResource(R.string.anchor_detail),
        entries = ScreenEdge.entries.map { stringResource(it.label) },
        selectedIndex = ScreenEdge.entries.indexOf(options.anchor),
        onSelect = { onOptionsChange(options.copy(anchor = ScreenEdge.entries[it])) },
    )
    Choice(
        label = stringResource(R.string.corner),
        detail = stringResource(R.string.corner_detail),
        entries = listOf(stringResource(R.string.start), stringResource(R.string.end)),
        selectedIndex = if (options.isCornerAtTheEnd) 1 else 0,
        onSelect = { onOptionsChange(options.copy(isCornerAtTheEnd = it == 1)) },
    )
}

/** Which of the four things in the way the label is asked to clear. */
@Composable
private fun HardwareSettings(options: ViewerOptions, onOptionsChange: (ViewerOptions) -> Unit) {
    Label(stringResource(R.string.counts_as_hardware))
    val policy = options.policy
    Setting(
        R.string.cutout, R.string.cutout_detail, policy.isCutoutIncluded,
    ) { onOptionsChange(options.copy(policy = policy.copy(isCutoutIncluded = it))) }
    Setting(
        R.string.waterfall, R.string.waterfall_detail, policy.isWaterfallIncluded,
    ) { onOptionsChange(options.copy(policy = policy.copy(isWaterfallIncluded = it))) }
    Setting(
        R.string.system_bars, R.string.system_bars_detail, policy.areSystemBarsIncluded,
    ) { onOptionsChange(options.copy(policy = policy.copy(areSystemBarsIncluded = it))) }
    Setting(
        R.string.far_edge_ignored, R.string.far_edge_detail, options.isFarEdgeIgnored,
    ) { onOptionsChange(options.copy(isFarEdgeIgnored = it)) }
}

/** The two the window owns rather than the layout: the cutout mode and the bars. */
@Composable
private fun WindowSettings(options: ViewerOptions, onOptionsChange: (ViewerOptions) -> Unit) {
    Label(stringResource(R.string.window))
    Choice(
        label = stringResource(R.string.cutout_mode),
        detail = stringResource(R.string.cutout_mode_detail),
        entries = CutoutMode.entries.map { stringResource(it.label) },
        selectedIndex = CutoutMode.entries.indexOf(options.cutoutMode),
        onSelect = { onOptionsChange(options.copy(cutoutMode = CutoutMode.entries[it])) },
    )
    Setting(
        R.string.hide_bars, R.string.hide_bars_detail, options.areBarsHidden,
    ) { onOptionsChange(options.copy(areBarsHidden = it)) }
}

/**
 * The label for a segmented button, read from the constant rather than from its
 * position. A hand-written list beside `entries` is correct only while nobody
 * reorders the enum, and a `when` over it stops compiling instead.
 */
@get:StringRes
private val ScreenEdge.label: Int
    get() = when (this) {
        ScreenEdge.TOP -> R.string.edge_top
        ScreenEdge.BOTTOM -> R.string.edge_bottom
        ScreenEdge.LEFT -> R.string.edge_left
        ScreenEdge.RIGHT -> R.string.edge_right
    }

@get:StringRes
private val CutoutMode.label: Int
    get() = when (this) {
        CutoutMode.ALWAYS -> R.string.cutout_mode_always
        CutoutMode.SHORT_EDGES -> R.string.cutout_mode_short
        CutoutMode.DEFAULT -> R.string.cutout_mode_default
        CutoutMode.NEVER -> R.string.cutout_mode_never
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Choice(
    label: String,
    detail: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    shape = SegmentedButtonDefaults.itemShape(index, entries.size),
                    icon = {},
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    label = { Text(entry, maxLines = 1, textAlign = TextAlign.Center) },
                )
            }
        }
    }
}

@Composable
private fun Setting(
    @StringRes label: Int,
    @StringRes detail: Int,
    isOn: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        supportingContent = { Text(stringResource(detail)) },
        trailingContent = { Switch(checked = isOn, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(
            value = isOn,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    )
}

/** How solid the card is, against [SHEET_ALPHA] for the frame around it. */
private const val CARD_ALPHA = 0.93f

@Composable
private fun Label(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Preview(name = "Controls, over a camera in the corner", heightDp = 900)
@Composable
private fun ControlSheetPreview() = SamplePreview(alignment = Alignment.BottomStart) {
    ControlSheetContent(
        cutout = PreviewHardware.cameraInTheCorner,
        options = ViewerOptions(),
        onOptionsChange = {},
    )
}

@Preview(name = "Controls, on a phone with nothing in the way", heightDp = 900)
@Composable
private fun ControlSheetWithoutHardwarePreview() = SamplePreview(alignment = Alignment.BottomStart) {
    ControlSheetContent(
        cutout = PreviewHardware.none,
        options = ViewerOptions(),
        onOptionsChange = {},
    )
}
