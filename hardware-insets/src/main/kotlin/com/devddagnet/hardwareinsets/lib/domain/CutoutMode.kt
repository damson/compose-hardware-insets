package com.devddagnet.hardwareinsets.lib.domain

/**
 * How far the window is allowed to extend into the display cutout.
 *
 * None of this applies below API 28, where the window has no such attribute and
 * the platform lays out clear of the cutout whatever is asked. From API 28 every
 * case below is honoured, except [ALWAYS], which arrives at API 30 and falls
 * back until then.
 */
enum class CutoutMode {
    /**
     * Into the cutout on every edge, falling back to [SHORT_EDGES] below API 30.
     *
     * The right answer for a surface that fills the window, and the wrong one
     * for most apps: content lands under the camera unless something insets it.
     */
    ALWAYS,

    /**
     * Into the cutout only on the window's short edges, which is the top and
     * bottom in portrait and neither in landscape.
     *
     * What most apps want: content reaches the top of the screen, and turning
     * the device puts the camera outside the window rather than over the
     * content.
     */
    SHORT_EDGES,

    /**
     * The platform's own rule: into the cutout only where the cutout is already
     * inside a system bar, or is no deeper than 16dp.
     *
     * The window is laid out clear of anything deeper, so a tall cutout leaves
     * a band the app never draws in.
     */
    DEFAULT,

    /** Never into the cutout, on any edge, whatever its depth. */
    NEVER,
}
