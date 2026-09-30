package io.github.damson.hardwareinsets.sample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import io.github.damson.hardwareinsets.domain.HardwarePolicy
import io.github.damson.hardwareinsets.clearOfTheHardware
import io.github.damson.hardwareinsets.cornerClearance
import io.github.damson.hardwareinsets.hardwareInsets
import io.github.damson.hardwareinsets.domain.CutoutShape
import io.github.damson.hardwareinsets.domain.ScreenEdge
import io.github.damson.hardwareinsets.domain.ScreenRotation
import io.github.damson.hardwareinsets.domain.onScreenAt
import io.github.damson.hardwareinsets.sample.R
import io.github.damson.hardwareinsets.sample.model.Plate
import io.github.damson.hardwareinsets.sample.model.Plates
import io.github.damson.hardwareinsets.sample.model.ViewerOptions

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
 * @param onShare called with the line that introduces the library. Sending it
 *   anywhere is an `Intent`, which is the activity's business and not this
 *   function's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    cutout: CutoutShape,
    options: ViewerOptions,
    rotation: ScreenRotation,
    onOptions: (ViewerOptions) -> Unit,
    onBarsOver: (isPaleAtTheTop: Boolean, isPaleAtTheBottom: Boolean) -> Unit,
    onShare: (String) -> Unit,
) {
    var areControlsOpen by remember { mutableStateOf(false) }
    // Saved, not remembered: rotation recreates the activity, and rotation is
    // half of what this screen demonstrates. The sheet is left out on purpose.
    var isChromeShown by rememberSaveable { mutableStateOf(true) }
    var favourites by rememberSaveable(stateSaver = FavouritesSaver) {
        mutableStateOf(emptySet<Int>())
    }
    // LEFT and RIGHT are edges of the device, not of the screen, so a layout
    // cannot use one directly: it has to ask which screen edge the device has
    // turned that one into. Skipping this is what makes a side-anchored control
    // land on top of the status bar.
    val edge = options.anchor.onScreenAt(rotation)
    val pager = rememberPagerState(pageCount = { Plates.size })
    val plate = Plates[pager.currentPage]
    val repoPitch = stringResource(R.string.share_repo)
    var shakes by remember { mutableIntStateOf(0) }
    val steps = rememberCoroutineScope()
    // Driven from here because the label leaves composition when the chrome
    // hides, and an effect held there restarts on every later reveal.
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(shakes) {
        // Not on the first composition, or it shakes itself hello on open.
        if (shakes == 0) return@LaunchedEffect
        nudge.snapTo(0f)
        nudge.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = SHAKE_MILLIS
                0f at 0
                -1f at SHAKE_MILLIS * 16 / 100
                0.78f at SHAKE_MILLIS * 34 / 100
                -0.48f at SHAKE_MILLIS * 52 / 100
                0.24f at SHAKE_MILLIS * 72 / 100
                0f at SHAKE_MILLIS
            },
        )
    }
    // Computed here rather than inside the row, because two things need it:
    // the row, which moves by it, and the label, which has to reserve the room
    // the row moves into. Hardware in the anchored edge's corner drives the row
    // inward on both edges, towards the label either way, so the label pays the
    // size of the move whichever direction it has.
    val cornerWidthPx = with(LocalDensity.current) { (CORNER_PADDING + ACTIONS_WIDTH).roundToPx() }
    val cornerClearance = cornerClearance(
        cutout.bounds,
        position = edge,
        // The corner asked about and the corner placed in come from one value.
        // Asking about one while placing at the other returns a confident zero,
        // which looks exactly like "no hardware here", and nothing in the API
        // can catch it.
        isAtTheEnd = options.isCornerAtTheEnd,
    )
    val cornerMoved = cornerClearance(cornerWidthPx)
        .awayFromTheHardware(edge, isAtTheEnd = options.isCornerAtTheEnd)
    // The one authored movement: the row slides clear rather than teleporting,
    // so it is legible that the hardware is what moved it. Animated here
    // because the label reserves what the row is using now, not what it will
    // be using once the spring settles.
    val cornerSliding by animateIntOffsetAsState(
        targetValue = cornerMoved,
        animationSpec = spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow),
        label = "clearance",
    )

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
                shake = nudge.value,
                // The larger of where the row is going and where it is, so the
                // label neither takes its space back before the row has left it
                // nor lets the spring's overshoot carry the row into it.
                rowMoved = with(LocalDensity.current) {
                    maxOf(abs(cornerMoved.y), abs(cornerSliding.y)).toDp()
                },
            )
        }
        // A half-height sheet's top edge lands exactly where these sit, so
        // leaving them up gets you two buttons sawn in half.
        AnimatedVisibility(
            visible = isChromeShown && !areControlsOpen,
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
            visible = isChromeShown && !areControlsOpen,
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
                moved = cornerSliding,
                edge = edge,
                options = options,
                isFavourite = pager.currentPage in favourites,
                onFavourite = { isOn ->
                    favourites =
                        if (isOn) favourites + pager.currentPage
                        else favourites - pager.currentPage
                    // A counter rather than a flag: pressed twice quickly, it
                    // has to shake twice.
                    shakes++
                },
                onShare = { onShare(repoPitch) },
            )
        }
        AnimatedVisibility(
            // Gone while the sheet is open: it is the thing that opened it,
            // and a see-through sheet would show it sitting behind itself.
            visible = isChromeShown && !areControlsOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            // The demo puts its own control in a corner of whichever edge you
            // pick, so a fixed trigger collides in exactly one configuration.
            //
            // Clear of everything rather than of the bars alone: this is the
            // way back to the controls, and a corner camera or a curved edge
            // under it is a button the visitor cannot reliably press. The
            // corner row is the one demonstrating a rectangle-precise offset;
            // this one only has to be reachable.
            modifier = Modifier
                .align(
                    if (edge == ScreenEdge.BOTTOM && options.isCornerAtTheEnd) Alignment.BottomStart
                    else Alignment.BottomEnd
                )
                .clearOfEverything(),
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
            // No scrim: every control on this sheet changes the screen behind
            // it, so dimming the plate to ask about the plate hides the answer.
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
private fun Modifier.clearOfEverything(): Modifier = clearOfTheHardware(policy = EverythingPolicy)

/** The policy behind [clearOfEverything], named so the label can ask about it too. */
private val EverythingPolicy = HardwarePolicy(
    isCutoutIncluded = true,
    isWaterfallIncluded = true,
    areSystemBarsIncluded = true,
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
 * The one control in the accent rather than on the plaque, because it is the
 * primary action and a gallery has exactly one. Flat, like the rest: a surface
 * with an elevation draws its own ambient shadow behind itself, and [lifted] is
 * what every control here is raised by instead.
 */
@Composable
private fun OpenControls(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    FloatingActionButton(
        onClick = onOpen,
        // Material's FAB is a 16dp rounded square by default, so the circle
        // the border and the lift were drawn as disagreed with the thing they
        // were drawn around. Both read the shape from here now. The accent
        // measures 9.72:1 on the darkest plate and 1.34:1 on the palest, so the
        // border and the lift are what give this an edge, not the fill.
        shape = FAB_SHAPE,
        containerColor = MaterialTheme.accent,
        contentColor = MaterialTheme.onAccent,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
        ),
        // Without this hairline a dark control on a dark plate has no edge.
        modifier = modifier
            .padding(16.dp)
            .lifted(FAB_SHAPE)
            .border(1.dp, MaterialTheme.onAccent.copy(alpha = 0.18f), FAB_SHAPE),
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
                // Each stroke covers the band from the edge out to half its
                // width, so a constant alpha stacks into a linear falloff.
                // Widen the step and it separates into countable rings.
                repeat(LIFT_STEPS) { step ->
                    drawPath(
                        path = path,
                        color = ink.copy(alpha = LIFT_ALPHA),
                        style = Stroke(width = (step + 1) * 1.dp.toPx()),
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
            Icon(
                imageVector = icon,
                contentDescription = stringResource(label),
                // White, where every other control is in the accent: two
                // plates are painted in a yellow 1.04:1 from it, and these sit
                // in the middle of the picture where they would read as paint.
                // It measures better too, 5.53:1 on the plaque over the palest
                // plate against the accent's 4.18:1. Both states are spelled
                // out because an explicit tint replaces the dimming a disabled
                // IconButton would have done.
                tint =
                    if (isEnabled) MaterialTheme.onPlaque
                    else MaterialTheme.onPlaque.copy(alpha = 0.34f),
            )
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
 *
 * @param rowMoved how far the corner row has been driven inward by hardware in
 *   its corner, which is room this label has to leave it.
 * @param shake where in its swing the favourite nudge is, in -1..1. The label
 *   answers
 *   with a shake, because it is the only thing on screen naming the plate the
 *   heart was pressed for.
 */
@Composable
private fun WallLabel(
    plate: Plate,
    number: Int,
    total: Int,
    edge: ScreenEdge,
    options: ViewerOptions,
    shake: Float,
    rowMoved: Dp,
    modifier: Modifier = Modifier,
) {
    val swing = with(LocalDensity.current) { SHAKE_SWING.toPx() }

    val density = LocalDensity.current
    val bars = WindowInsets.systemBars.asPaddingValues()
    val applied = with(density) {
        hardwareInsets(edge, options.policy, options.isFarEdgeIgnored).let {
            if (edge.isPlacedAtTheTop) it.getTop(density).toDp() else it.getBottom(density).toDp()
        }
    }
    val fromTheEdge =
        if (edge.isPlacedAtTheTop) bars.calculateTopPadding() else bars.calculateBottomPadding()
    // The two corner controls do not sit off the same thing. The row clears the
    // bars; the settings button clears everything, so with the bars hidden and
    // a cutout on this edge it is the one standing further in.
    //
    // The button stays on the bottom edge whatever the label does, so it only
    // competes with a label anchored there. Counting it at the top reserved
    // room for a control that is not on that edge, and the label answered a
    // policy change it had no business answering.
    val settingsButton = if (edge.isPlacedAtTheTop) {
        0.dp
    } else {
        with(density) { hardwareInsets(edge, EverythingPolicy).getBottom(density).toDp() }
    }
    val reserved = reservedForTheCornerControls(fromTheEdge, settingsButton, rowMoved, applied)

    Box(
        modifier
            .offset { IntOffset((shake * swing).roundToInt(), 0) }
            .fillMaxWidth()
            .clearOfTheHardware(
                position = edge,
                policy = options.policy,
                isFarEdgeIgnored = options.isFarEdgeIgnored,
            )
            // Inboard of the corner row rather than beside it: both corners of
            // an edge can be occupied, and a gutter for each leaves the label
            // about 90dp wide.
            .padding(
                top = if (edge.isPlacedAtTheTop) reserved else 12.dp,
                bottom = if (edge.isPlacedAtTheTop) 12.dp else reserved,
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
                            if (isCurrent) MaterialTheme.accent
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
    moved: IntOffset,
    edge: ScreenEdge,
    options: ViewerOptions,
    isFavourite: Boolean,
    onFavourite: (Boolean) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            .padding(CORNER_PADDING)
            .offset { moved }
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
                    tint =
                        if (isFavourite) MaterialTheme.accent
                        else MaterialTheme.onPlaque.copy(alpha = 0.78f),
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
 * Which end of the screen this edge puts things at.
 *
 * [toAlignment] and the label's own padding both need the answer, and when they
 * disagree the label reserves its gutter at the end it is not sitting at, which
 * runs the corner control straight through it. That is what `LEFT` did.
 */
private val ScreenEdge.isPlacedAtTheTop: Boolean
    get() = this == ScreenEdge.TOP || this == ScreenEdge.LEFT

/**
 * The mapping every caller of this library writes for itself, because
 * [ScreenEdge] names an edge without being able to place anything at it.
 */
private fun ScreenEdge.toAlignment(): Alignment =
    if (isPlacedAtTheTop) Alignment.TopStart else Alignment.BottomStart

internal fun ScreenEdge.cornerAlignment(isAtTheEnd: Boolean): Alignment = when (this) {
    ScreenEdge.TOP, ScreenEdge.LEFT ->
        if (isAtTheEnd) Alignment.TopEnd else Alignment.TopStart
    ScreenEdge.BOTTOM, ScreenEdge.RIGHT ->
        if (isAtTheEnd) Alignment.BottomEnd else Alignment.BottomStart
}

private val ACTIONS_WIDTH = 112.dp

/** The trigger is a circle, at every size and in every state. */
private val FAB_SHAPE = CircleShape

/**
 * How solid the sheet's frame is, which is now the only part of it you see
 * through.
 *
 * It can be this low because no text sits on it. Three values were tried while
 * the text was on this surface, 0.82, 0.94 and 0.88, and each was a trade of
 * reading against seeing: the detail in a painting fights a paragraph whatever
 * the average contrast says. The card in [ControlSheetContent] takes the text
 * out of that argument, and the frame is then free to be glass.
 */
private const val SHEET_ALPHA = 0.55f

/** How far the lift falls below what it lifts. */
private val LIFT_DROP = 2.dp

/** How long the label takes to answer the heart. */
private const val SHAKE_MILLIS = 420

/** How far it swings at the widest point. */
private val SHAKE_SWING = 12.dp

/**
 * How far it reaches, in half-dp bands: a 7dp shadow, not a 18dp one.
 *
 * The bands are narrow because the falloff is only as smooth as they are thin,
 * and there are this many because the darkest point is their sum. Change one
 * without the other and it goes back to rings.
 */
private const val LIFT_STEPS = 14

/** Each band's share of the shadow. Fourteen of these is 0.18 at the contact. */
private const val LIFT_ALPHA = 0.013f

/**
 * The height the corner row occupies along its edge, measured from where that
 * row starts rather than from the screen.
 *
 * The tallest thing in it is the trigger: 16dp of padding, a 56dp circle, and
 * 16dp again. The bar inset underneath is added at the point of use, because
 * whether the label has already paid for it depends on the policy.
 */
private val CORNER_ROW = 88.dp

/**
 * Turns the inward distances [cornerClearance] reports into the translation
 * `Modifier.offset` wants, for a control in the [isAtTheEnd] corner of [edge].
 *
 * Kept out of the composable because no emulator can prove it: the sign is only
 * wrong where the cutout sits in the corner of the anchored edge, and no cutout
 * emulation puts one in a bottom corner. The same argument the library makes for
 * keeping its own geometry pure.
 *
 * `Modifier.offset` is direction aware, so a positive `x` already means "towards
 * the end" and the horizontal flip is the corner alone, unchanged under RTL. The
 * vertical one is not: a positive `y` is always downwards.
 */
/**
 * How much further in the label has to sit to leave both corner controls alone.
 *
 * The two are counted from different origins, which is the whole difficulty:
 * the row is placed off the system bars whatever the policy says, the settings
 * button off everything the window reports, and the label off whatever the
 * policy does say. So the further of the two corner controls is [barInset] plus
 * [rowMoved], or [everythingInset], plus its own height, and the label has
 * already been moved by [applied].
 * Only the difference is left to pay, and it is never negative: where the
 * policy has already moved the label past the row there is nothing to reserve.
 *
 * Kept pure because the interesting cases are ones no emulator can produce: a
 * [rowMoved] above zero needs a cutout in the corner of the anchored edge.
 */
internal fun reservedForTheCornerControls(
    barInset: Dp,
    everythingInset: Dp,
    rowMoved: Dp,
    applied: Dp,
): Dp = (maxOf(barInset + rowMoved, everythingInset) + CORNER_ROW - applied).coerceAtLeast(0.dp)

internal fun IntOffset.awayFromTheHardware(edge: ScreenEdge, isAtTheEnd: Boolean): IntOffset =
    IntOffset(
        x = if (isAtTheEnd) -x else x,
        y = if (edge == ScreenEdge.TOP) y else -y,
    )

/** How far the corner row sits in from the window edge on every side. */
private val CORNER_PADDING = 16.dp

/**
 * A [Set] is not one of the types a `Bundle` carries, so the favourites travel
 * as the list they came from.
 */
private val FavouritesSaver = listSaver<Set<Int>, Int>(
    save = { it.toList() },
    restore = { it.toSet() },
)
