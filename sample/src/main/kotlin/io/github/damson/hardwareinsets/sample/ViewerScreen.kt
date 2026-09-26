package io.github.damson.hardwareinsets.sample

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.damson.hardwareinsets.CutoutShape
import io.github.damson.hardwareinsets.ScreenEdge
import io.github.damson.hardwareinsets.clearOfTheHardware
import io.github.damson.hardwareinsets.cornerClearance
import io.github.damson.hardwareinsets.onScreenAt

/**
 * The picture runs to every edge. The caption, the corner control and the
 * markers are what move.
 *
 * @param cutout the live shape from the activity's sibling view. Passed in
 *   rather than read here, so this stays a function of its input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    cutout: CutoutShape,
    options: ViewerOptions,
    onOptions: (ViewerOptions) -> Unit,
    onClose: () -> Unit,
) {
    var areControlsOpen by remember { mutableStateOf(false) }
    // LEFT and RIGHT are edges of the device, not of the screen, so a layout
    // cannot use one directly: it has to ask which screen edge the device has
    // turned that one into. Skipping this is what makes a side-anchored control
    // land on top of the status bar.
    val rotation = LocalView.current.display?.rotation ?: 0
    val edge = options.anchor.onScreenAt(rotation)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.picture)
            .markers(cutout, options.areMarkersShown)
    ) {
        Caption(edge = edge, options = options, modifier = Modifier.align(edge.toAlignment()))
        CloseButton(cutout, edge, options, onClose)
        OpenControls(
            onOpen = { areControlsOpen = true },
            // The trigger gets out of the way of the thing being demonstrated.
            // Nothing else on this screen can: the demo puts its own control in
            // a corner of whichever edge you pick, so a fixed trigger collides
            // in exactly one configuration and looks like a bug when it does.
            modifier = Modifier.align(
                if (edge == ScreenEdge.BOTTOM && options.isCornerAtTheEnd) Alignment.BottomStart
                else Alignment.BottomEnd
            ),
        )
    }

    if (areControlsOpen) {
        ModalBottomSheet(
            onDismissRequest = { areControlsOpen = false },
            // Half height first, so a change to a top-anchored control is visible
            // while it is being made. Drag up for the rest.
            sheetState = rememberModalBottomSheetState(),
        ) {
            ControlSheetContent(cutout, options, onOptions)
        }
    }
}

/** The only thing on the picture that is not part of the demonstration. */
@Composable
private fun OpenControls(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onOpen,
        modifier = modifier.padding(16.dp),
    ) {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.open_controls))
    }
}

/** A translucent pane: the markers underneath stay visible through it. */
@Composable
private fun Modifier.pane(shape: Shape): Modifier = this
    .background(MaterialTheme.scrim, shape)
    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)

/**
 * The inset goes outside the pane, so the pane moves clear of the hardware
 * rather than growing a transparent margin inside itself.
 */
@Composable
private fun Caption(edge: ScreenEdge, options: ViewerOptions, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clearOfTheHardware(
                position = edge,
                policy = options.policy,
                isFarEdgeIgnored = options.isFarEdgeIgnored,
            )
            // Inboard of the corner row rather than beside it. Both corners of
            // an edge can be occupied, by the control and by the settings
            // button, and reserving a gutter for each leaves the caption about
            // 90dp wide, which wraps it to one word per line.
            .padding(
                top = if (edge == ScreenEdge.TOP) CORNER_ROW else 12.dp,
                bottom = if (edge == ScreenEdge.TOP) 12.dp else CORNER_ROW,
                start = 16.dp,
                end = 16.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (options.anchor == edge) "Anchored to ${options.anchor.name}"
            else "Anchored to ${options.anchor.name}, now ${edge.name}",
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .pane(MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

/**
 * A control in the corner of the anchored edge, moved by only the cutout
 * rectangles it actually overlaps.
 *
 * Padding it by the full-width inset instead would drop it clear of a centred
 * punch-hole it is nowhere near, which is the whole reason this library exists.
 */
@Composable
private fun BoxScope.CloseButton(
    cutout: CutoutShape,
    edge: ScreenEdge,
    options: ViewerOptions,
    onClose: () -> Unit,
) {
    // The corner asked about and the corner placed in come from one value.
    // Asking about one while placing at the other returns a confident zero,
    // which looks exactly like "no hardware here", and nothing in the API can
    // catch it.
    val clearance = cornerClearance(cutout.bounds, position = edge, isAtTheEnd = options.isCornerAtTheEnd)
    // The clearance is asked in pixels, because the rectangles the platform
    // reports are in pixels and in window coordinates.
    val widthPx = with(LocalDensity.current) { CLOSE_WIDTH.roundToPx() }
    // The one authored movement: the control slides clear rather than
    // teleporting, so it is legible that the hardware is what moved it.
    val offset by animateIntOffsetAsState(
        targetValue = clearance(widthPx),
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "clearance",
    )
    val shape = MaterialTheme.shapes.extraLarge

    FilledTonalButton(
        onClick = onClose,
        shape = shape,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier
            .align(edge.cornerAlignment(options.isCornerAtTheEnd))
            .padding(16.dp)
            .offset { offset }
            .sizeIn(minWidth = CLOSE_WIDTH, minHeight = 48.dp),
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(R.string.close),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/**
 * What the platform reported, drawn over the picture: the cutout rectangles,
 * and the depth each edge says content must stay off.
 *
 * The rectangles are the thing Compose will not give you. The depths it will,
 * and they are drawn beside them because the difference is the whole argument:
 * a centred punch-hole reports a depth across the entire width.
 */
private fun Modifier.markers(cutout: CutoutShape, isShowing: Boolean): Modifier =
    drawWithContent {
        drawContent()
        if (!isShowing) return@drawWithContent

        val depths = listOf(
            Offset(0f, 0f) to Size(size.width, cutout.topInset.toFloat()),
            Offset(0f, size.height - cutout.bottomInset) to Size(size.width, cutout.bottomInset.toFloat()),
            Offset(0f, 0f) to Size(cutout.leftInset.toFloat(), size.height),
            Offset(size.width - cutout.rightInset, 0f) to Size(cutout.rightInset.toFloat(), size.height),
        )
        // A wash plus the line where the depth ends. The wash alone reads as a
        // dirty band across the picture; the line is the number being reported.
        depths.filter { it.second.width > 0f && it.second.height > 0f }.forEach { (at, of) ->
            drawRect(InsetMarker.copy(alpha = 0.10f), topLeft = at, size = of)
            drawRect(
                InsetMarker.copy(alpha = 0.85f),
                topLeft = at,
                size = of,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        cutout.bounds.forEach { rect ->
            val topLeft = Offset(rect.left.toFloat(), rect.top.toFloat())
            val of = Size(rect.width().toFloat(), rect.height().toFloat())
            drawRect(CutoutMarker.copy(alpha = 0.45f), topLeft = topLeft, size = of)
            drawRect(CutoutMarker, topLeft = topLeft, size = of, style = Stroke(width = 3.dp.toPx()))
        }
    }

/**
 * The mapping every caller of this library writes for itself, because
 * [ScreenEdge] names an edge without being able to place anything at it.
 */
private fun ScreenEdge.toAlignment(): Alignment = when (this) {
    ScreenEdge.TOP, ScreenEdge.LEFT -> Alignment.TopStart
    ScreenEdge.BOTTOM, ScreenEdge.RIGHT -> Alignment.BottomStart
}

internal fun ScreenEdge.cornerAlignment(isAtTheEnd: Boolean): Alignment = when (this) {
    ScreenEdge.TOP, ScreenEdge.LEFT ->
        if (isAtTheEnd) Alignment.TopEnd else Alignment.TopStart
    ScreenEdge.BOTTOM, ScreenEdge.RIGHT ->
        if (isAtTheEnd) Alignment.BottomEnd else Alignment.BottomStart
}

private val CLOSE_WIDTH = 112.dp

/** The height a corner control plus its padding occupies along its edge. */
private val CORNER_ROW = 80.dp
