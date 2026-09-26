package io.github.damson.hardwareinsets.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
 * @param onShare called with the line describing the plate on show. Sending it
 *   anywhere is an `Intent`, which is the activity's business and not this
 *   function's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    cutout: CutoutShape,
    options: ViewerOptions,
    onOptions: (ViewerOptions) -> Unit,
    onBarsOver: (isPaleAtTheTop: Boolean, isPaleAtTheBottom: Boolean) -> Unit,
    onShare: (String) -> Unit,
) {
    var areControlsOpen by remember { mutableStateOf(false) }
    var isChromeShown by remember { mutableStateOf(true) }
    var favourites by remember { mutableStateOf(emptySet<Int>()) }
    // LEFT and RIGHT are edges of the device, not of the screen, so a layout
    // cannot use one directly: it has to ask which screen edge the device has
    // turned that one into. Skipping this is what makes a side-anchored control
    // land on top of the status bar.
    val rotation = LocalView.current.display?.rotation ?: 0
    val edge = options.anchor.onScreenAt(rotation)
    val pager = rememberPagerState(pageCount = { Plates.size })
    val plate = Plates[pager.currentPage]
    val plateLine = shareLine(plate)
    val steps = rememberCoroutineScope()
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
            modifier = Modifier.align(Alignment.CenterStart).clearOfEverything(),
        ) {
            StepButton(
                icon = Icons.Filled.KeyboardArrowLeft,
                label = R.string.previous_plate,
                isEnabled = pager.currentPage > 0,
                onStep = { steps.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            )
        }
        AnimatedVisibility(
            visible = isChromeShown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).clearOfEverything(),
        ) {
            StepButton(
                icon = Icons.Filled.KeyboardArrowRight,
                label = R.string.next_plate,
                isEnabled = pager.currentPage < Plates.size - 1,
                onStep = { steps.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
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
            CornerActions(
                cutout = cutout,
                edge = edge,
                options = options,
                isFavourite = pager.currentPage in favourites,
                onFavourite = { isOn ->
                    favourites =
                        if (isOn) favourites + pager.currentPage
                        else favourites - pager.currentPage
                },
                onShare = { onShare(plateLine) },
            )
        }
        AnimatedVisibility(
            // Gone while the sheet is open: it is the thing that opened it, and
            // a sheet you can see through shows whatever is behind it, so a
            // button left there reads as a rendering fault rather than a button.
            visible = isChromeShown && !areControlsOpen,
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
            // No scrim and a sheet you can see through, because every control on
            // it changes the screen behind it: dimming the plate to ask about
            // the plate hides the answer. The alpha is the floor that keeps the
            // supporting text at 4.5:1 in both schemes over both the palest
            // plate and the deepest.
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = SHEET_ALPHA),
            // Spelled out, because `contentColorFor` has no answer for a colour
            // that is not a scheme role, and its non-answer is `Unspecified`:
            // the text would then inherit whatever the ambient content colour
            // happens to be.
            contentColor = MaterialTheme.colorScheme.onSurface,
            scrimColor = Color.Transparent,
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
 * Everything the window reports on the edge, for a control that has no corner.
 *
 * See [StepButton] for why this is the blunt instrument and the corner control
 * gets the precise one.
 */
@Composable
private fun Modifier.clearOfEverything(): Modifier = clearOfTheHardware(
    policy = HardwarePolicy(
        isCutoutIncluded = true,
        isWaterfallIncluded = true,
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

/**
 * The only thing on the plate that is not part of the demonstration.
 *
 * On the plaque like every other floating control, and flat. A translucent
 * surface with an elevation draws its own ambient shadow behind itself, which
 * shows through the edges as a dark frame and reads as a rendering fault.
 */
@Composable
private fun OpenControls(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onOpen,
        containerColor = MaterialTheme.plaque,
        contentColor = MaterialTheme.onPlaque,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
        // The hairline every plaque on this screen carries. Without it a dark
        // control on a dark plate has no edge at all, and two of the five
        // plates are dark.
        modifier = modifier
            .padding(16.dp)
            .lifted(CircleShape)
            .border(1.dp, MaterialTheme.onPlaque.copy(alpha = 0.22f), CircleShape),
    ) {
        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.open_controls))
    }
}

/**
 * A shadow that cannot show through the thing it is behind.
 *
 * Both shadows Compose ships paint the whole silhouette and let the element
 * draw over it, which works only while the element is opaque. Every plaque here
 * is translucent, so either of them would show through its own surface: a dark
 * centre with a hard frame at the edges, which reads as a rendering fault and
 * gets blamed on the gradient.
 *
 * So the shape is clipped out first and only what falls outside is painted.
 * Concentric strokes rather than a blur, because a mask filter is silently
 * ignored on a hardware canvas and the failure looks like a shadow that was
 * never asked for.
 */
@Composable
private fun Modifier.lifted(shape: Shape): Modifier {
    val ink = MaterialTheme.plaqueShadow
    return drawBehind {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
        clipPath(path, ClipOp.Difference) {
            translate(top = LIFT_DROP.toPx()) {
                // Many faint strokes rather than a few strong ones. Each covers
                // the band from the edge out to half its width, so a constant
                // alpha stacks into a linear falloff; widen the step and the
                // stack separates into rings you can count.
                repeat(LIFT_STEPS) { step ->
                    drawPath(
                        path = path,
                        color = ink.copy(alpha = 0.026f),
                        style = Stroke(width = (step + 1) * 2.dp.toPx()),
                    )
                }
            }
        }
    }
}

/**
 * Previous and next, because a swipe leaves no trace on a still screen.
 *
 * On the sides, where nothing else on this screen goes: the label and the
 * corner control both live on the edge the anchor names, and that is only ever
 * the top or the bottom.
 *
 * They take the whole inset rather than a rectangle-precise offset. A corner
 * control can ask [cornerClearance] which rectangles are actually in its way;
 * a button halfway down a side has no corner to be measured from, so it clears
 * everything the window reports on that edge, bars included.
 */
@Composable
private fun StepButton(
    icon: ImageVector,
    label: Int,
    isEnabled: Boolean,
    onStep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.plaque,
        contentColor = MaterialTheme.onPlaque,
        border = BorderStroke(1.dp, MaterialTheme.onPlaque.copy(alpha = 0.22f)),
        modifier = modifier
            .padding(16.dp)
            .lifted(CircleShape),
    ) {
        IconButton(onClick = onStep, enabled = isEnabled) {
            Icon(icon, contentDescription = stringResource(label))
        }
    }
}

/** A translucent plaque: the markers underneath stay visible through it. */
@Composable
private fun Modifier.plaque(shape: Shape): Modifier = this
    .lifted(shape)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlateRail(number = number, total = total)
                Text(
                    anchorReadout(options, edge),
                    color = MaterialTheme.onPlaqueVariant,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
            }
        }
    }
}

/**
 * Where you are in the gallery, and that there is somewhere else to be.
 *
 * An indicator and not a control: a dot small enough to read as one is far
 * under the 48dp a touch target owes, and the gesture it would duplicate is the
 * swipe the whole screen already takes. The row carries the count for a screen
 * reader, because four shapes do not.
 */
@Composable
private fun PlateRail(number: Int, total: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.plate_rail, number, total)
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val isCurrent = index == number - 1
            Box(
                Modifier
                    .size(width = if (isCurrent) 20.dp else 8.dp, height = 8.dp)
                    .background(
                        color =
                            if (isCurrent) MaterialTheme.onPlaque
                            else MaterialTheme.onPlaqueVariant.copy(alpha = 0.45f),
                        shape = CircleShape,
                    )
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

/** What is shared: the plate's title and who made it, in one line. */
@Composable
private fun shareLine(plate: Plate): String =
    stringResource(R.string.share_plate, stringResource(plate.title), stringResource(plate.maker))

/**
 * The corner control: favourite this plate, or send it on.
 *
 * The two things a gallery puts next to a picture, and the reason the corner is
 * worth defending at all. A control that only closed the screen could be moved
 * anywhere, or dropped for the back gesture, which is what closes this one.
 *
 * Moved by only the cutout rectangles it actually overlaps. Padding it by the
 * full-width inset instead would drop it clear of a centred punch-hole it is
 * nowhere near, which is the whole reason this library exists.
 */
@Composable
private fun CornerActions(
    cutout: CutoutShape,
    edge: ScreenEdge,
    options: ViewerOptions,
    isFavourite: Boolean,
    onFavourite: (Boolean) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The corner asked about and the corner placed in come from one value.
    // Asking about one while placing at the other returns a confident zero,
    // which looks exactly like "no hardware here", and nothing in the API can
    // catch it.
    val clearance = cornerClearance(cutout.bounds, position = edge, isAtTheEnd = options.isCornerAtTheEnd)
    // The clearance is asked in pixels, because the rectangles the platform
    // reports are in pixels and in window coordinates.
    val widthPx = with(LocalDensity.current) { ACTIONS_WIDTH.roundToPx() }
    // The one authored movement: the control slides clear rather than
    // teleporting, so it is legible that the hardware is what moved it.
    val offset by animateIntOffsetAsState(
        targetValue = clearance(widthPx),
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "clearance",
    )

    // The plaque, not a scheme container: this sits on a painting, and the
    // gallery runs from near white to near black. A tonal container picked by
    // the scheme is invisible on half the plates, and which half changes with
    // the system theme.
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.plaque,
        contentColor = MaterialTheme.onPlaque,
        border = BorderStroke(1.dp, MaterialTheme.onPlaque.copy(alpha = 0.22f)),
        modifier = modifier
            .padding(16.dp)
            .offset { offset }
            .lifted(MaterialTheme.shapes.extraLarge)
            .sizeIn(minWidth = ACTIONS_WIDTH, minHeight = 48.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconToggleButton(checked = isFavourite, onCheckedChange = onFavourite) {
                Icon(
                    imageVector =
                        if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavourite) R.string.remove_from_favourites
                        else R.string.add_to_favourites
                    ),
                    // Filled against outlined carries the state. A tint would
                    // have to come from the palette, and every colour in it is
                    // either the plaque itself or one of the two marker colours,
                    // which mean something else on this screen.
                    tint = MaterialTheme.onPlaque.copy(alpha = if (isFavourite) 1f else 0.78f),
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
            }
        }
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

private val ACTIONS_WIDTH = 112.dp

/**
 * How solid the control sheet is. Below this the supporting text drops under
 * 4.5:1 in the dark scheme over the palest plate, which is the worst of the
 * four corners this has to hold.
 */
private const val SHEET_ALPHA = 0.82f

/** How far the lift falls below what it lifts. */
private val LIFT_DROP = 4.dp

/** How far it reaches, in 1dp bands. */
private const val LIFT_STEPS = 18

/** The height a corner control plus its padding occupies along its edge. */
private val CORNER_ROW = 80.dp
