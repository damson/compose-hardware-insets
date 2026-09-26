package io.github.damson.hardwareinsets.sample

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * One work in the gallery: the wall label, and how the work is painted.
 *
 * @param paint the composition, given the palette it is painted from and a
 *   measurer for the one plate that writes words. A lambda
 *   over a [DrawScope] rather than an image, because the plate has to be the
 *   whole window: an asset would arrive at its own aspect ratio and the letter
 *   boxing would become the margin this library exists to do without.
 * @param isPaleAtTheTop whether the status bar's icons have to be dark to be
 *   seen over this plate.
 * @param isPaleAtTheBottom the same question for the navigation bar. Asked per
 *   edge and not per plate, because a plate can be sky at the top and water at
 *   the bottom, and one answer for both loses one of the two bars.
 */
@Immutable
internal class Plate(
    @StringRes val title: Int,
    @StringRes val maker: Int,
    @StringRes val medium: Int,
    @StringRes val description: Int,
    val isPaleAtTheTop: Boolean,
    val isPaleAtTheBottom: Boolean,
    val paint: DrawScope.(PlatePalette, TextMeasurer) -> Unit,
)

/** The four colours a plate may use. */
@Immutable
internal data class PlatePalette(
    val paper: Color,
    val mist: Color,
    val sea: Color,
    val ink: Color,
)

/**
 * The works, in hanging order.
 *
 * Two are pale and two are dark on purpose. A label over artwork cannot take
 * its contrast from the colour scheme, because what is behind it is a painting
 * and not a surface, and a gallery of four pale plates would never show it.
 */
/** The worked ground under the marks: ink that has been painted rather than filled. */
private val Grounded = Color(0xFF0B1B2E)

/**
 * The one colour in the gallery that is not in the palette, and the only plate
 * that uses it. Warm, and deliberately not the red the cutout markers own.
 */
private val Accent = Color(0xFFF2D06B)

internal val Plates = listOf(
    Plate(
        title = R.string.plate_tide_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_tide_medium,
        description = R.string.plate_tide_description,
        isPaleAtTheTop = true,
        isPaleAtTheBottom = false,
        paint = { palette, _ -> paintLowTide(palette) },
    ),
    Plate(
        title = R.string.plate_head_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_head_medium,
        description = R.string.plate_head_description,
        isPaleAtTheTop = false,
        isPaleAtTheBottom = false,
        paint = { palette, text -> paintHead(palette, text) },
    ),
    Plate(
        title = R.string.plate_ledger_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_ledger_medium,
        description = R.string.plate_ledger_description,
        isPaleAtTheTop = true,
        isPaleAtTheBottom = true,
        paint = { palette, text -> paintLedger(palette, text) },
    ),
    Plate(
        title = R.string.plate_chamber_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_chamber_medium,
        description = R.string.plate_chamber_description,
        isPaleAtTheTop = false,
        isPaleAtTheBottom = false,
        paint = { palette, _ -> paintCloudChamber(palette) },
    ),
    Plate(
        title = R.string.plate_crown_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_crown_medium,
        description = R.string.plate_crown_description,
        isPaleAtTheTop = false,
        isPaleAtTheBottom = false,
        paint = { palette, text -> paintCrown(palette, text) },
    ),
)

/**
 * A plate, full bleed, under everything else on the screen.
 *
 * Painted once per size into a bitmap and blitted after that, because a plate
 * is a still image and the screen over it is not: the label fades, the corner
 * control springs, and the pager draws two plates at once through a swipe.
 * Repainting hundreds of strokes on every one of those frames is what turns a
 * gallery into a five second input timeout on a slow device, with nothing in
 * the code that looks wrong.
 *
 * The description replaces the node's children rather than adding to them: a
 * painting has no parts a screen reader can usefully walk.
 */
@Composable
internal fun PlateArtwork(plate: Plate, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        // Remembered against the plate and the size, so the strokes are laid
        // down once and every later frame is a blit. Painting inside the draw
        // modifier instead repaints on every recomposition, and a modal sheet
        // recomposes this screen on every frame of its own animation: the sheet
        // then never finishes opening and the window stops answering input.
        val painted = remember(plate, width, height) {
            ImageBitmap(width, height).also { image ->
                CanvasDrawScope().draw(
                    density = density,
                    layoutDirection = direction,
                    canvas = Canvas(image),
                    size = Size(width.toFloat(), height.toFloat()),
                ) {
                    plate.paint(this, GalleryPalette, measurer)
                }
            }
        }
        Image(
            bitmap = painted,
            contentDescription = stringResource(plate.description),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Colour field. The whole palette as one vertical ramp, cut once by a horizon
 * so the field has a subject rather than being a gradient.
 *
 * The tide lines crowd toward the horizon by a falling geometric series.
 * Evenly spaced, they read as a ruler.
 */
private fun DrawScope.paintLowTide(palette: PlatePalette) {
    drawRect(
        Brush.verticalGradient(
            0.00f to palette.paper,
            0.32f to palette.mist,
            0.58f to palette.sea,
            1.00f to palette.ink,
        )
    )

    val horizon = size.height * 0.58f
    val sun = Offset(size.width * 0.20f, horizon - size.minDimension * 0.34f)
    val disc = size.minDimension * 0.105f
    // The halo before the disc, so the disc keeps its edge. A sun drawn at the
    // alpha of its own glow is the grey circle nobody means to paint.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.paper.copy(alpha = 0.45f), Color.Transparent),
            center = sun,
            radius = disc * 2.6f,
        ),
        radius = disc * 2.6f,
        center = sun,
    )
    drawCircle(color = palette.paper.copy(alpha = 0.95f), radius = disc, center = sun)

    drawRect(
        palette.ink.copy(alpha = 0.22f),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, size.height - horizon),
    )
    drawLine(
        palette.paper.copy(alpha = 0.60f),
        Offset(0f, horizon),
        Offset(size.width, horizon),
        strokeWidth = 1.5.dp.toPx(),
    )

    // Tide marks, not rules. Each one starts and stops short by its own amount:
    // drawn edge to edge they read as a ruled page, and the eye stops seeing
    // water at all.
    val next = seeded(0x71DE)
    var gap = (size.height - horizon) * 0.34f
    var y = size.height
    var alpha = 0.40f
    while (y - gap > horizon + 1f) {
        y -= gap
        drawLine(
            palette.paper.copy(alpha = alpha * (0.55f + next() * 0.45f)),
            Offset(size.width * next() * 0.42f, y),
            Offset(size.width * (1f - next() * 0.38f), y),
            strokeWidth = 1.dp.toPx(),
        )
        gap *= 0.68f
        alpha *= 0.94f
    }
}

/**
 * A line field: particle tracks over a single glow. The dark plate, and the
 * one that proves a label reads over ink as well as over paper.
 */
private fun DrawScope.paintCloudChamber(palette: PlatePalette) {
    drawRect(palette.ink)

    val glow = Offset(size.width * 0.5f, size.height * 0.42f)
    val reach = size.minDimension * 0.95f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.sea.copy(alpha = 0.60f), Color.Transparent),
            center = glow,
            radius = reach,
        ),
        radius = reach,
        center = glow,
    )

    val next = seeded(0x51ED)
    repeat(140) {
        val start = Offset(next() * size.width, next() * size.height)
        val angle = -1.05f + next() * 0.30f
        val length = size.minDimension * (0.03f + next() * 0.20f)
        drawLine(
            palette.mist.copy(alpha = 0.14f + next() * 0.30f),
            start,
            Offset(start.x + cos(angle) * length, start.y + sin(angle) * length),
            strokeWidth = 1.dp.toPx(),
        )
    }
    repeat(3) { index ->
        val start = Offset(size.width * (0.12f + 0.30f * index), size.height * (0.88f - 0.06f * index))
        val length = size.minDimension * 0.85f
        val angle = -1.02f
        drawLine(
            palette.paper.copy(alpha = 0.80f),
            start,
            Offset(start.x + cos(angle) * length, start.y + sin(angle) * length),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

/**
 * Neo-expressionist: a crown, a ribcage, words struck through, raw blocks of
 * colour on a worked ground.
 *
 * An original composition in that idiom, not a copy of anyone's painting. The
 * works it takes its language from are in copyright and cannot ship in a public
 * repository; the language itself is not anyone's property.
 *
 * The one plate that leaves the palette, by a single warm accent. It stays well
 * away from the red the cutout markers use, because a plate that competes with
 * the markers argues against the thing this screen is for.
 */
private fun DrawScope.paintCrown(palette: PlatePalette, text: TextMeasurer) {
    val unit = size.width / 12f
    val next = seeded(0x8A51)

    // A worked ground rather than a flat fill: three passes of near-ink, so the
    // marks sit on something painted instead of on a background colour.
    drawRect(palette.ink)
    repeat(7) {
        val top = next() * size.height
        drawRect(
            color = if (next() > 0.5f) Grounded else palette.ink.copy(alpha = 0.55f),
            topLeft = Offset(-unit + next() * unit * 2f, top),
            size = Size(size.width * (0.5f + next() * 0.7f), size.height * (0.08f + next() * 0.16f)),
        )
    }

    drawRect(
        palette.sea,
        topLeft = Offset(unit * 6.6f, size.height * 0.52f),
        size = Size(unit * 4.6f, size.height * 0.18f),
    )
    repeat(9) { index ->
        val x = unit * 6.8f + index * unit * 0.48f
        drawLine(
            palette.paper.copy(alpha = 0.55f),
            Offset(x, size.height * 0.53f),
            Offset(x - unit * 0.6f, size.height * 0.69f),
            strokeWidth = 2.dp.toPx(),
        )
    }
    drawRect(
        Accent,
        topLeft = Offset(unit * 0.6f, size.height * 0.80f),
        size = Size(unit * 3.4f, size.height * 0.10f),
    )

    // Everything that carries the picture sits below the top third, because the
    // label is there and an 82% plaque is opaque enough to lose a crown behind
    // it. What stays up there is the ground and one word, which is what a
    // painting under a wall label looks like anyway.
    crown(
        at = Offset(unit * 0.8f, size.height * 0.345f),
        width = unit * 4.2f,
        height = size.height * 0.07f,
    )
    ribcage(
        palette = palette,
        spine = Offset(unit * 3.0f, size.height * 0.56f),
        height = size.height * 0.22f,
    )

    struckThrough(text, "SALT", Offset(unit * 0.7f, size.height * 0.445f), unit * 1.05f, palette.paper)
    struckThrough(text, "NORTH", Offset(unit * 6.2f, size.height * 0.20f), unit * 0.72f, palette.paper)
    struckThrough(text, "IRON", Offset(unit * 4.6f, size.height * 0.915f), unit * 0.86f, palette.paper)

    // Scratches last, over everything, because that is the order they were made
    // in and the only thing that keeps the blocks from looking printed.
    repeat(40) {
        val start = Offset(next() * size.width, next() * size.height)
        val length = unit * (0.3f + next() * 1.4f)
        val lean = -1.9f + next() * 0.6f
        drawLine(
            palette.mist.copy(alpha = 0.10f + next() * 0.22f),
            start,
            Offset(start.x + cos(lean) * length, start.y + sin(lean) * length),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

/**
 * A head: mask, teeth, a halo of marks, one eye crossed out.
 *
 * The figure the idiom keeps returning to. Original, like the other two in this
 * language, and for the same reason: the paintings it learns from are in
 * copyright and a public repository cannot carry them.
 */
private fun DrawScope.paintHead(palette: PlatePalette, text: TextMeasurer) {
    val unit = size.width / 12f
    val next = seeded(0x4EAD)

    drawRect(palette.ink)
    repeat(6) {
        drawRect(
            color = if (next() > 0.45f) Grounded else palette.ink.copy(alpha = 0.5f),
            topLeft = Offset(-unit + next() * unit * 2f, next() * size.height),
            size = Size(size.width * (0.45f + next() * 0.7f), size.height * (0.07f + next() * 0.15f)),
        )
    }
    drawRect(
        palette.sea,
        topLeft = Offset(unit * 5.4f, size.height * 0.33f),
        size = Size(unit * 5.2f, size.height * 0.22f),
    )

    // The skull, drawn as one closed line with none of its corners agreeing.
    val head = Path().apply {
        moveTo(unit * 1.6f, size.height * 0.45f)
        lineTo(unit * 4.2f, size.height * 0.395f)
        lineTo(unit * 7.6f, size.height * 0.425f)
        lineTo(unit * 8.4f, size.height * 0.55f)
        lineTo(unit * 7.9f, size.height * 0.685f)
        lineTo(unit * 4.6f, size.height * 0.735f)
        lineTo(unit * 1.9f, size.height * 0.665f)
        close()
    }
    drawPath(head, palette.paper, style = Stroke(width = 6.dp.toPx(), join = StrokeJoin.Round))

    // The halo: struck, not drawn as a ring, so it reads as marks and not as a
    // plate. It stops short of the top third, because that is where the wall
    // label hangs and a halo behind a plaque is a smudge.
    repeat(13) { index ->
        val lean = -2.55f + index * 0.115f
        val from = Offset(unit * 4.9f, size.height * 0.415f)
        val near = unit * 1.6f
        val far = near + unit * (0.5f + next() * 0.4f)
        drawLine(
            Accent,
            Offset(from.x + cos(lean) * near, from.y + sin(lean) * near),
            Offset(from.x + cos(lean) * far, from.y + sin(lean) * far),
            strokeWidth = 3.dp.toPx(),
        )
    }

    crossedEye(palette, Offset(unit * 3.3f, size.height * 0.505f), unit * 0.62f)
    crossedEye(palette, Offset(unit * 6.4f, size.height * 0.505f), unit * 0.62f)

    // Teeth, counted. Bars in a bar is the whole of the mouth.
    val mouth = Offset(unit * 2.9f, size.height * 0.595f)
    val mouthWidth = unit * 4.4f
    val mouthHeight = size.height * 0.055f
    drawRect(Grounded, topLeft = mouth, size = Size(mouthWidth, mouthHeight))
    drawRect(
        palette.paper,
        topLeft = mouth,
        size = Size(mouthWidth, mouthHeight),
        style = Stroke(width = 3.dp.toPx()),
    )
    repeat(7) { index ->
        val x = mouth.x + mouthWidth * (index + 1) / 8f
        drawLine(
            palette.paper,
            Offset(x, mouth.y),
            Offset(x, mouth.y + mouthHeight),
            strokeWidth = 3.dp.toPx(),
        )
    }

    struckThrough(text, "HEAD", Offset(unit * 1.4f, size.height * 0.795f), unit * 1.1f, palette.paper)
    struckThrough(text, "BONE", Offset(unit * 6.4f, size.height * 0.885f), unit * 0.8f, palette.paper)

    repeat(34) {
        val start = Offset(next() * size.width, next() * size.height)
        val length = unit * (0.25f + next() * 1.2f)
        val lean = -1.9f + next() * 0.6f
        drawLine(
            palette.mist.copy(alpha = 0.10f + next() * 0.20f),
            start,
            Offset(start.x + cos(lean) * length, start.y + sin(lean) * length),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

/** An eye that has been looked at twice: a ring, then an X over it. */
private fun DrawScope.crossedEye(palette: PlatePalette, at: Offset, radius: Float) {
    drawCircle(palette.paper, radius = radius, center = at, style = Stroke(width = 4.dp.toPx()))
    drawCircle(Accent, radius = radius * 0.34f, center = at)
    val reach = radius * 1.25f
    drawLine(
        palette.paper,
        Offset(at.x - reach, at.y - reach),
        Offset(at.x + reach, at.y + reach),
        strokeWidth = 3.dp.toPx(),
    )
    drawLine(
        palette.paper,
        Offset(at.x + reach, at.y - reach),
        Offset(at.x - reach, at.y + reach),
        strokeWidth = 3.dp.toPx(),
    )
}

/**
 * The same hand working on paper: a list of metals, every entry struck out, a
 * tally beside it and an arrow going nowhere.
 *
 * The pale plate of the three, and the gallery needs one: a label that cannot
 * take its contrast from the colour scheme has to be shown failing to, which
 * needs something bright under it.
 */
private fun DrawScope.paintLedger(palette: PlatePalette, text: TextMeasurer) {
    val unit = size.width / 12f
    val next = seeded(0x1ED6)

    drawRect(palette.paper)
    repeat(6) {
        drawRect(
            color = palette.mist.copy(alpha = 0.35f + next() * 0.4f),
            topLeft = Offset(-unit + next() * unit * 2.5f, next() * size.height),
            size = Size(size.width * (0.4f + next() * 0.7f), size.height * (0.05f + next() * 0.14f)),
        )
    }

    drawRect(
        palette.ink,
        topLeft = Offset(unit * 0.5f, size.height * 0.335f),
        size = Size(unit * 5.6f, size.height * 0.30f),
        style = Stroke(width = 4.dp.toPx()),
    )
    listOf("SALT", "IRON", "TIN", "LEAD").forEachIndexed { index, word ->
        struckThrough(
            measurer = text,
            word = word,
            at = Offset(unit * 0.9f, size.height * (0.355f + index * 0.072f)),
            size = unit * 0.78f,
            written = palette.ink,
            strike = palette.sea,
        )
    }

    // A tally, because a ledger counts. Four and a stroke across, five times.
    repeat(5) { group ->
        val left = unit * 6.5f
        val top = size.height * (0.36f + group * 0.052f)
        repeat(4) { mark ->
            val x = left + mark * unit * 0.32f
            drawLine(
                palette.ink,
                Offset(x, top),
                Offset(x + unit * 0.08f, top + size.height * 0.032f),
                strokeWidth = 3.dp.toPx(),
            )
        }
        drawLine(
            palette.sea,
            Offset(left - unit * 0.12f, top + size.height * 0.026f),
            Offset(left + unit * 1.16f, top + size.height * 0.008f),
            strokeWidth = 3.dp.toPx(),
        )
    }

    crownIn(palette.ink, at = Offset(unit * 7.6f, size.height * 0.20f), width = unit * 3.2f,
        height = size.height * 0.05f)

    arrow(palette.sea, from = Offset(unit * 1.2f, size.height * 0.71f),
        to = Offset(unit * 6.4f, size.height * 0.755f), head = unit * 0.55f)
    arrow(palette.ink, from = Offset(unit * 9.4f, size.height * 0.62f),
        to = Offset(unit * 9.4f, size.height * 0.80f), head = unit * 0.5f)

    drawRect(
        Accent,
        topLeft = Offset(unit * 2.2f, size.height * 0.82f),
        size = Size(unit * 3.0f, size.height * 0.085f),
    )
    struckThrough(text, "PAID", Offset(unit * 6.6f, size.height * 0.845f), unit * 0.9f,
        palette.ink, palette.sea)

    repeat(30) {
        val start = Offset(next() * size.width, next() * size.height)
        val length = unit * (0.2f + next() * 1.0f)
        val lean = -1.9f + next() * 0.6f
        drawLine(
            palette.ink.copy(alpha = 0.08f + next() * 0.14f),
            start,
            Offset(start.x + cos(lean) * length, start.y + sin(lean) * length),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

/** An arrow, drawn the way a note takes one: one line and two strokes. */
private fun DrawScope.arrow(color: Color, from: Offset, to: Offset, head: Float) {
    drawLine(color, from, to, strokeWidth = 4.dp.toPx())
    val lean = atan2(to.y - from.y, to.x - from.x)
    listOf(lean + 2.5f, lean - 2.5f).forEach { side ->
        drawLine(
            color,
            to,
            Offset(to.x + cos(side) * head, to.y + sin(side) * head),
            strokeWidth = 4.dp.toPx(),
        )
    }
}

/** Three points, drawn in one stroke, which is how it is always drawn. */
private fun DrawScope.crown(at: Offset, width: Float, height: Float) =
    crownIn(Accent, at, width, height)

/** The same crown in whatever colour the plate is working in. */
private fun DrawScope.crownIn(color: Color, at: Offset, width: Float, height: Float) {
    val step = width / 6f
    val path = Path().apply {
        moveTo(at.x, at.y + height)
        lineTo(at.x + step * 0.7f, at.y)
        lineTo(at.x + step * 1.9f, at.y + height * 0.62f)
        lineTo(at.x + step * 3.0f, at.y - height * 0.16f)
        lineTo(at.x + step * 4.1f, at.y + height * 0.62f)
        lineTo(at.x + step * 5.3f, at.y)
        lineTo(at.x + width, at.y + height)
    }
    drawPath(path, color, style = Stroke(width = 5.dp.toPx(), join = StrokeJoin.Round))
}

/** A spine and six pairs of ribs, drawn as an anatomy plate would have them. */
private fun DrawScope.ribcage(palette: PlatePalette, spine: Offset, height: Float) {
    drawLine(
        palette.paper,
        spine,
        Offset(spine.x, spine.y + height),
        strokeWidth = 3.dp.toPx(),
    )
    repeat(6) { index ->
        val y = spine.y + height * (0.10f + index * 0.16f)
        val reach = height * (0.42f - index * 0.035f)
        listOf(-1f, 1f).forEach { side ->
            drawLine(
                palette.paper.copy(alpha = 0.85f),
                Offset(spine.x, y),
                Offset(spine.x + reach * side, y + height * 0.07f),
                strokeWidth = 2.5.dp.toPx(),
            )
        }
    }
}

/**
 * A word written and then struck out, which is the point: the strike is what
 * makes you read it.
 */
private fun DrawScope.struckThrough(
    measurer: TextMeasurer,
    word: String,
    at: Offset,
    size: Float,
    written: Color,
    strike: Color = Accent,
) {
    val laid = measurer.measure(
        text = word,
        style = TextStyle(
            color = written,
            fontSize = size.toSp(),
            fontWeight = FontWeight.Black,
            letterSpacing = (size * 0.06f).toSp(),
        ),
    )
    drawText(laid, topLeft = at)
    val middle = at.y + laid.size.height * 0.52f
    drawLine(
        strike,
        Offset(at.x - size * 0.15f, middle + size * 0.06f),
        Offset(at.x + laid.size.width + size * 0.15f, middle - size * 0.04f),
        strokeWidth = 4.dp.toPx(),
    )
}

/**
 * A deterministic sequence in `0..1`.
 *
 * A plate is repainted on every recomposition, and the caption fading in is a
 * recomposition, so a random field would reshuffle itself as the chrome moves.
 */
private fun seeded(seed: Int): () -> Float {
    var state = seed
    return {
        state = state * 1_103_515_245 + 12_345
        ((state ushr 8) and 0xFFFF) / 65_536f
    }
}
