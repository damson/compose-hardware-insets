package io.github.damson.hardwareinsets.sample

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource

/**
 * One work in the gallery: the wall label, and the plate it hangs beside.
 *
 * @param artwork the plate itself, exported from the generator that paints it
 *   rather than drawn here. The sample is a caller of an insets library and
 *   nothing else; a few hundred lines of painting in it were a second subject
 *   competing with the first.
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
    @DrawableRes val artwork: Int,
    val isPaleAtTheTop: Boolean,
    val isPaleAtTheBottom: Boolean,
)

/**
 * The works, in hanging order.
 *
 * Some are pale and some are dark on purpose. A label over artwork cannot take
 * its contrast from the colour scheme, because what is behind it is a painting
 * and not a surface, and a gallery of pale plates would never show it.
 */
internal val Plates = listOf(
    Plate(
        title = R.string.plate_tide_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_tide_medium,
        description = R.string.plate_tide_description,
        artwork = R.drawable.plate_low_tide,
        isPaleAtTheTop = true,
        isPaleAtTheBottom = false,
    ),
    Plate(
        title = R.string.plate_head_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_head_medium,
        description = R.string.plate_head_description,
        artwork = R.drawable.plate_head_counted,
        isPaleAtTheTop = false,
        isPaleAtTheBottom = false,
    ),
    Plate(
        title = R.string.plate_ledger_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_ledger_medium,
        description = R.string.plate_ledger_description,
        artwork = R.drawable.plate_ledger_of_metals,
        isPaleAtTheTop = true,
        isPaleAtTheBottom = true,
    ),
    Plate(
        title = R.string.plate_arms_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_arms_medium,
        description = R.string.plate_arms_description,
        artwork = R.drawable.plate_arms_raised,
        isPaleAtTheTop = false,
        isPaleAtTheBottom = false,
    ),
    Plate(
        title = R.string.plate_crown_title,
        maker = R.string.plate_maker,
        medium = R.string.plate_crown_medium,
        description = R.string.plate_crown_description,
        artwork = R.drawable.plate_crown_crossed_out,
        isPaleAtTheTop = false,
        isPaleAtTheBottom = false,
    ),
)

/**
 * A plate, full bleed, under everything else on the screen.
 *
 * Cropped rather than fitted, which is the whole reason these are exported at
 * one tall size instead of being dropped in as assets at whatever size they
 * came at: a fitted image letterboxes, and the letterboxing would become the
 * margin this library exists to do without.
 *
 * The description replaces the node's children rather than adding to them: a
 * painting has no parts a screen reader can usefully walk.
 */
@Composable
internal fun PlateArtwork(plate: Plate, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(plate.artwork),
        contentDescription = stringResource(plate.description),
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}
