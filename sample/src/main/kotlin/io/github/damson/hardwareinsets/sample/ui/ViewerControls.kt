package io.github.damson.hardwareinsets.sample.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.damson.hardwareinsets.cornerClearance
import io.github.damson.hardwareinsets.sample.R

/**
 * The only thing on the plate that is not part of the demonstration.
 *
 * The one control in the accent rather than on the plaque, because it is the
 * primary action and a gallery has exactly one. Flat, like the rest: a surface
 * with an elevation draws its own ambient shadow behind itself, and [lifted] is
 * what every control here is raised by instead.
 */
@Composable
internal fun OpenControls(onOpen: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun StepButton(
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

/** The trigger is a circle, at every size and in every state. */
private val FAB_SHAPE = CircleShape
