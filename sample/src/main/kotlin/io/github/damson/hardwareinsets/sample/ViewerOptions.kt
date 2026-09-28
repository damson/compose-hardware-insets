package io.github.damson.hardwareinsets.sample

import android.os.Bundle
import androidx.compose.runtime.Immutable
import io.github.damson.hardwareinsets.CutoutMode
import io.github.damson.hardwareinsets.HardwarePolicy
import io.github.damson.hardwareinsets.ScreenEdge

/**
 * Every decision the library leaves to its caller, in one place.
 *
 * The sample exists to make each of them visible, so each one is here rather
 * than fixed in a call site: a demo that hard-codes a parameter is not
 * demonstrating it.
 *
 * @param anchor the edge the caption and the corner control are anchored to.
 *   `LEFT` and `RIGHT` are edges of the device, so what the layout uses is
 *   `anchor.onScreenAt(rotation)`, not this.
 * @param isCornerAtTheEnd which corner of that edge the control sits in. It has
 *   to match what `cornerClearance` is asked, and nothing but this enforces it.
 * @param policy which of the window's insets count as hardware.
 * @param isFarEdgeIgnored whether the edge opposite [anchor] is forced to zero.
 * @param cutoutMode how far into the cutout the window may extend.
 * @param areBarsHidden whether the system bars are off screen.
 * @param areMarkersShown whether the cutout rectangles and inset depths are
 *   drawn over the picture.
 */
@Immutable
data class ViewerOptions(
    val anchor: ScreenEdge = ScreenEdge.TOP,
    val isCornerAtTheEnd: Boolean = false,
    val policy: HardwarePolicy = HardwarePolicy(),
    val isFarEdgeIgnored: Boolean = false,
    val cutoutMode: CutoutMode = CutoutMode.ALWAYS,
    val areBarsHidden: Boolean = false,
    val areMarkersShown: Boolean = true,
)

/**
 * Writes the options into a `Bundle`, for the activity to carry across its own
 * recreation.
 *
 * Every field goes in by hand rather than through a parcelable, because the two
 * enums are the library's and the sample does not own their serialised form.
 * Enum names are written rather than ordinals, so reordering either enum in the
 * library cannot silently turn a saved TOP into a BOTTOM.
 */
fun ViewerOptions.saveInto(out: Bundle): Bundle = out.apply {
    putString(ANCHOR, anchor.name)
    putBoolean(CORNER_AT_THE_END, isCornerAtTheEnd)
    putBoolean(CUTOUT_INCLUDED, policy.isCutoutIncluded)
    putBoolean(WATERFALL_INCLUDED, policy.isWaterfallIncluded)
    putBoolean(BARS_INCLUDED, policy.areSystemBarsIncluded)
    putBoolean(FAR_EDGE_IGNORED, isFarEdgeIgnored)
    putString(CUTOUT_MODE, cutoutMode.name)
    putBoolean(BARS_HIDDEN, areBarsHidden)
    putBoolean(MARKERS_SHOWN, areMarkersShown)
}

/**
 * Reads back what [saveInto] wrote, falling back to the defaults for anything
 * absent, so a first launch and a restore go down the same path.
 */
fun viewerOptionsFrom(saved: Bundle?): ViewerOptions {
    val defaults = ViewerOptions()
    if (saved == null) return defaults
    return ViewerOptions(
        anchor = enumOrNull<ScreenEdge>(saved.getString(ANCHOR)) ?: defaults.anchor,
        isCornerAtTheEnd = saved.getBoolean(CORNER_AT_THE_END, defaults.isCornerAtTheEnd),
        policy = HardwarePolicy(
            isCutoutIncluded = saved.getBoolean(CUTOUT_INCLUDED, defaults.policy.isCutoutIncluded),
            isWaterfallIncluded =
                saved.getBoolean(WATERFALL_INCLUDED, defaults.policy.isWaterfallIncluded),
            areSystemBarsIncluded =
                saved.getBoolean(BARS_INCLUDED, defaults.policy.areSystemBarsIncluded),
        ),
        isFarEdgeIgnored = saved.getBoolean(FAR_EDGE_IGNORED, defaults.isFarEdgeIgnored),
        cutoutMode = enumOrNull<CutoutMode>(saved.getString(CUTOUT_MODE)) ?: defaults.cutoutMode,
        areBarsHidden = saved.getBoolean(BARS_HIDDEN, defaults.areBarsHidden),
        areMarkersShown = saved.getBoolean(MARKERS_SHOWN, defaults.areMarkersShown),
    )
}

// A name written by an older build of the app, or by a newer one, is a name
// this build may not have. valueOf throws on it; the caller wants the default.
private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }

private const val ANCHOR = "anchor"
private const val CORNER_AT_THE_END = "isCornerAtTheEnd"
private const val CUTOUT_INCLUDED = "isCutoutIncluded"
private const val WATERFALL_INCLUDED = "isWaterfallIncluded"
private const val BARS_INCLUDED = "areSystemBarsIncluded"
private const val FAR_EDGE_IGNORED = "isFarEdgeIgnored"
private const val CUTOUT_MODE = "cutoutMode"
private const val BARS_HIDDEN = "areBarsHidden"
private const val MARKERS_SHOWN = "areMarkersShown"
