package io.github.damson.hardwareinsets.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp

/** A translucent plaque: the markers underneath stay visible through it. */
@Composable
internal fun Modifier.plaque(shape: Shape): Modifier = this
    .lifted(shape)
    .background(MaterialTheme.plaque, shape)
    .border(1.dp, MaterialTheme.onPlaque.copy(alpha = 0.22f), shape)

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
internal fun Modifier.lifted(shape: Shape): Modifier {
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

private val LIFT_DROP = 2.dp

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
