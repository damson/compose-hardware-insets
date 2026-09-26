package io.github.damson.hardwareinsets.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import io.github.damson.hardwareinsets.CutoutShape
import io.github.damson.hardwareinsets.HardwarePolicy
import io.github.damson.hardwareinsets.ScreenEdge
import io.github.damson.hardwareinsets.clearOfTheHardware
import io.github.damson.hardwareinsets.cornerClearance
import io.github.damson.hardwareinsets.onScreenAt

/**
 * A gallery: one plate at a time, running to every edge, swipe for the next.
 *
 * The use case rather than a demonstration of it. A viewer is where full bleed
 * is the point and not a style, so it is also where the hardware is a real
 * problem: the plate is meant to be under the camera, and the wall label is
 * meant not to be.
 *
 * @param cutout the live shape from the activity's sibling view. Passed in
 *   rather than read here, so this stays a function of its input.
 * @param onBarsOver called with how pale the plate now showing is at the top
 *   and at the bottom. The system bars draw their icons over it, and with a
 *   transparent bar the platform has no idea what is underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    cutout: CutoutShape,
    options: ViewerOptions,
    onOptions: (ViewerOptions) -> Unit,
    onBarsOver: (isPaleAtTheTop: Boolean, isPaleAtTheBottom: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var areControlsOpen by remember { mutableStateOf(false) }
    var isChromeShown by remember { mutableStateOf(true) }
    // LEFT and RIGHT are edges of the device, not of the screen, so a layout
    // cannot use one directly: it has to ask which screen edge the device has
    // turned that one into. Skipping this is what makes a side-anchored control
    // land on top of the status bar.
    val rotation = LocalView.current.display?.rotation ?: 0
    val edge = options.anchor.onScreenAt(rotation)
    val pager = rememberPagerState(pageCount = { Plates.size })
    val plate = Plates[pager.currentPage]
    // On the settled page, not on the swipe: flipping the icons mid-drag reads
    // as a glitch, and the bars are over the plate you are arriving at.
    LaunchedEffect(plate) { onBarsOver(plate.isPaleAtTheTop, plate.isPaleAtTheBottom) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .markers(cutout, options.areMarkersShown)
    ) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            PlateArtwork(
                plate = Plates[page],
                modifier = Modifier.togglingTheChrome(isChromeShown) { isChromeShown = !isChromeShown },
            )
        }

        AnimatedVisibility(
            visible = isChromeShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(edge.toAlignment()).fillMaxWidth(),
        ) {
            WallLabel(
                plate = plate,
                number = pager.currentPage + 1,
                total = Plates.size,
                edge = edge,
                options = options,
            )
        }
        AnimatedVisibility(
            visible = isChromeShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(edge.cornerAlignment(options.isCornerAtTheEnd))
                .clearOfTheBars(),
        ) {
            CloseButton(cutout, edge, options, onClose)
        }
        AnimatedVisibility(
            visible = isChromeShown,
            enter = fadeIn(),
            exit = fadeOut(),
            // The trigger gets out of the way of the thing being demonstrated.
            // Nothing else on this screen can: the demo puts its own control in
            // a corner of whichever edge you pick, so a fixed trigger collides
            // in exactly one configuration and looks like a bug when it does.
            modifier = Modifier
                .align(
                    if (edge == ScreenEdge.BOTTOM && options.isCornerAtTheEnd) Alignment.BottomStart
                    else Alignment.BottomEnd
                )
                .clearOfTheBars(),
        ) {
            OpenControls(onOpen = { areControlsOpen = true })
        }
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

/**
 * A control keeps off the bars by padding, and off the camera by moving.
 *
 * Two different questions, and this is the one the library answers by policy: a
 * bar is software, runs the full width and is always in the way, so it is a
 * margin. The camera is a rectangle somewhere along the edge, so it is an
 * offset, and [cornerClearance] is what works it out. Pad by the cutout as well
 * and the control drops clear of a punch-hole it is nowhere near.
 *
 * The label is not given this, because the label is the thing on trial: which
 * insets it takes is the sheet's first switch.
 */
@Composable
private fun Modifier.clearOfTheBars(): Modifier = clearOfTheHardware(
    policy = HardwarePolicy(
        isCutoutIncluded = false,
        isWaterfallIncluded = false,
        areSystemBarsIncluded = true,
    ),
)

/**
 * Tap the plate to put the chrome away: the gesture every full-screen viewer
 * has, and the one that leaves the plate alone under the camera.
 *
 * No indication, because a ripple over a painting is a defect, and a click
 * label rather than a bare handler, because with the chrome gone there is
 * nothing on the screen for a screen reader to describe.
 */
@Composable
private fun Modifier.togglingTheChrome(isChromeShown: Boolean, onToggle: () -> Unit): Modifier {
    val label = stringResource(if (isChromeShown) R.string.hide_the_label else R.string.show_the_label)
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClickLabel = label,
        onClick = onToggle,
    )
}

/** The only thing on the plate that is not part of the demonstration. */
@Composable
private fun OpenControls(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onOpen,
        modifier = modifier.padding(16.dp),
    ) {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.open_controls))
    }
}

/** A translucent plaque: the markers underneath stay visible through it. */
@Composable
private fun Modifier.plaque(shape: Shape): Modifier = this
    .background(MaterialTheme.plaque, shape)
    .border(1.dp, MaterialTheme.onPlaque.copy(alpha = 0.22f), shape)

/**
 * The wall label: what the plate is, and where the library has put it.
 *
 * The inset goes outside the plaque, so the plaque moves clear of the hardware
 * rather than growing a transparent margin inside itself.
 */
@Composable
private fun WallLabel(
    plate: Plate,
    number: Int,
    total: Int,
    edge: ScreenEdge,
    options: ViewerOptions,
    modifier: Modifier = Modifier,
) {
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
            // button, and reserving a gutter for each leaves the label about
            // 90dp wide, which wraps it to one word per line.
            .padding(
                top = if (edge == ScreenEdge.TOP) CORNER_ROW else 12.dp,
                bottom = if (edge == ScreenEdge.TOP) 12.dp else CORNER_ROW,
                start = 16.dp,
                end = 16.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .plaque(MaterialTheme.shapes.large)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                stringResource(plate.title),
                color = MaterialTheme.onPlaque,
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(plate.maker),
                color = MaterialTheme.onPlaqueVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(plate.medium),
                color = MaterialTheme.onPlaqueVariant.copy(alpha = 0.82f),
                style = MaterialTheme.typography.bodySmall,
            )
            HorizontalDivider(
                Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.onPlaque.copy(alpha = 0.22f),
            )
            Text(
                stringResource(R.string.plate_of, number, total) + "   " + anchorReadout(options, edge),
                color = MaterialTheme.onPlaqueVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * The half of the label that belongs to the library rather than to the plate.
 *
 * Both names are shown whenever they differ, because "anchored to LEFT, now
 * BOTTOM" is the sentence that explains why a side anchor moves when the device
 * turns, and a reader who only sees the second one reads it as a bug.
 */
@Composable
private fun anchorReadout(options: ViewerOptions, edge: ScreenEdge): String =
    if (options.anchor == edge) stringResource(R.string.anchored_to, options.anchor.name)
    else stringResource(R.string.anchored_to_now, options.anchor.name, edge.name)

/**
 * A control in the corner of the anchored edge, moved by only the cutout
 * rectangles it actually overlaps.
 *
 * Padding it by the full-width inset instead would drop it clear of a centred
 * punch-hole it is nowhere near, which is the whole reason this library exists.
 */
@Composable
private fun CloseButton(
    cutout: CutoutShape,
    edge: ScreenEdge,
    options: ViewerOptions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
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

    FilledTonalButton(
        onClick = onClose,
        shape = MaterialTheme.shapes.extraLarge,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier
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
 * What the platform reported, drawn over the plate: the cutout rectangles, and
 * the depth each edge says content must stay off.
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
        // dirty band across the plate; the line is the number being reported.
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
