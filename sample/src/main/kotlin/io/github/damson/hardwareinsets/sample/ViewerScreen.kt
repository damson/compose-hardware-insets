package io.github.damson.hardwareinsets.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.damson.hardwareinsets.CutoutShape
import io.github.damson.hardwareinsets.HardwarePolicy
import io.github.damson.hardwareinsets.ScreenEdge
import io.github.damson.hardwareinsets.clearOfTheHardware
import io.github.damson.hardwareinsets.cornerClearance

/**
 * The picture runs to every edge. The caption and the close button are what move.
 *
 * @param cutout the live shape from the activity's sibling view. Passed in rather
 *   than read here, so this stays a function of its input and previews.
 */
@Composable
fun ViewerScreen(cutout: CutoutShape) {
    var edge by remember { mutableStateOf(ScreenEdge.TOP) }
    var arePaddingBars by remember { mutableStateOf(false) }
    var isShowingCutout by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(PICTURE).cutoutOverlay(cutout, isShowingCutout)) {
        Caption(
            edge = edge,
            arePaddingBars = arePaddingBars,
            modifier = Modifier.align(edge.toAlignment()),
        )
        CloseButton(cutout, edge)
        Controls(
            edge = edge,
            arePaddingBars = arePaddingBars,
            isShowingCutout = isShowingCutout,
            onEdge = { edge = it },
            onBars = { arePaddingBars = it },
            onCutout = { isShowingCutout = it },
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/**
 * The background keeps the full width and only the words move, which is why the
 * padding goes on the contents rather than on the surface.
 *
 * @param arePaddingBars whether the system bars count as something to avoid. The
 *   status bar is still on screen here, so a caption under it is unreadable; the
 *   library leaves them out by default because most callers hide them.
 */
@Composable
private fun Caption(edge: ScreenEdge, arePaddingBars: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().background(SCRIM)) {
        Text(
            text = "Anchored to ${edge.name}",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clearOfTheHardware(
                    position = edge,
                    policy = HardwarePolicy(areSystemBarsIncluded = arePaddingBars),
                )
                .padding(16.dp),
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
private fun BoxScope.CloseButton(cutout: CutoutShape, edge: ScreenEdge) {
    // One value drives both the question and the placement. Asking about one
    // corner while placing at the other returns a confident zero, which looks
    // exactly like "no hardware here", and nothing in the API can catch it.
    val clearance = cornerClearance(cutout.bounds, position = edge, isAtTheEnd = CLOSE_AT_THE_END)
    // The clearance is asked in pixels, because the rectangles the platform
    // reports are in pixels and in window coordinates.
    val widthPx = with(LocalDensity.current) { CLOSE_WIDTH.roundToPx() }

    Button(
        onClick = {},
        modifier = Modifier
            .align(edge.cornerAlignment(CLOSE_AT_THE_END))
            .offset { clearance(widthPx) }
            .size(CLOSE_WIDTH, 48.dp),
    ) {
        Text(stringResource(R.string.close))
    }
}

@Composable
private fun Controls(
    edge: ScreenEdge,
    arePaddingBars: Boolean,
    isShowingCutout: Boolean,
    onEdge: (ScreenEdge) -> Unit,
    onBars: (Boolean) -> Unit,
    onCutout: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(SCRIM).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.caption_edge), color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenEdge.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == edge,
                    onClick = { onEdge(candidate) },
                    label = { Text(candidate.name) },
                )
            }
        }
        Toggle(stringResource(R.string.pad_for_bars), arePaddingBars, onBars)
        Toggle(stringResource(R.string.show_cutout), isShowingCutout, onCutout)
    }
}

@Composable
private fun Toggle(label: String, isOn: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(checked = isOn, onCheckedChange = onChange)
        Text(label, color = Color.White)
    }
}

/** Draws the rectangles the platform reported, which is the thing Compose will not tell you. */
private fun Modifier.cutoutOverlay(cutout: CutoutShape, isShowing: Boolean): Modifier =
    drawWithContent {
        drawContent()
        if (!isShowing) return@drawWithContent
        cutout.bounds.forEach { rect ->
            drawRect(
                color = Color.Red.copy(alpha = 0.4f),
                topLeft = Offset(rect.left.toFloat(), rect.top.toFloat()),
                size = Size(rect.width().toFloat(), rect.height().toFloat()),
            )
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

private fun ScreenEdge.cornerAlignment(isAtTheEnd: Boolean): Alignment = when (this) {
    ScreenEdge.TOP, ScreenEdge.LEFT ->
        if (isAtTheEnd) Alignment.TopEnd else Alignment.TopStart
    ScreenEdge.BOTTOM, ScreenEdge.RIGHT ->
        if (isAtTheEnd) Alignment.BottomEnd else Alignment.BottomStart
}

/** The corner the close button lives in, asked and answered in one place. */
private const val CLOSE_AT_THE_END = false
private val CLOSE_WIDTH = 96.dp
private val PICTURE = Brush.linearGradient(listOf(Color(0xFF12283C), Color(0xFF7A3B2E)))
private val SCRIM = Color.Black.copy(alpha = 0.45f)
