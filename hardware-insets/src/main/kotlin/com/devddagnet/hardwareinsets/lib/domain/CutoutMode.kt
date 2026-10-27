package com.devddagnet.hardwareinsets.lib.domain

/**
 * How far the window is allowed to extend into the display cutout.
 *
 * None of this applies below API 28, where the window has no such attribute and
 * the platform lays out clear of the cutout whatever is asked. From API 28 every
 * case below is honoured, except [ALWAYS], which arrives at API 30 and falls
 * back until then.
 *
 * **An app targeting SDK 35 or later gets [ALWAYS] on Android 15 whatever it
 * asks for.** On a non-floating window the platform reads [DEFAULT],
 * [SHORT_EDGES] and [NEVER] as [ALWAYS], so that nothing shows a black bar
 * beside the cutout. On such a target the choice below reaches only older
 * platforms and floating windows, and what keeps content clear of the camera is
 * the inset, not the mode.
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
     * Into the cutout on the screen's short edges and never on its long ones:
     * the top and bottom held upright, the left and right turned sideways.
     *
     * Subject to the Android 15 rule above.
     */
    SHORT_EDGES,

    /**
     * The platform's own rule: into the cutout only where the cutout is already
     * inside a system bar, or is no deeper than 16dp.
     *
     * The window is laid out clear of anything deeper, so a tall cutout leaves
     * a band the app never draws in. Subject to the Android 15 rule above.
     */
    DEFAULT,

    /**
     * Never into the cutout, on any edge, whatever its depth. Subject to the
     * Android 15 rule above.
     */
    NEVER,
}
