package io.github.damson.hardwareinsets.domain

/**
 * Which corner of the window a control is tucked into.
 *
 * One value instead of the pair of booleans this replaces. Those were
 * `isRtl` and `isAtTop`, and the first was not the layout direction: it meant
 * "against the right edge", which is the start corner under RTL. A caller
 * reading the name and passing the layout direction got a confident answer
 * about the wrong corner.
 */
enum class WindowCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    ;

    internal val isAtTheTop: Boolean get() = this == TOP_LEFT || this == TOP_RIGHT

    internal val isAtTheRight: Boolean get() = this == TOP_RIGHT || this == BOTTOM_RIGHT
}
