package io.github.damson.hardwareinsets.sample

import androidx.compose.ui.unit.dp
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * The label and the corner row are placed off different things, so what the
 * label owes the row is a difference rather than a constant.
 */
class CornerReservationTest {

    private val row = 88.dp

    @Test
    fun `a policy that applies nothing pays the bar inset and the whole row`() {
        assertThat(reservedForTheCornerRow(barInset = 48.dp, rowMoved = 0.dp, applied = 0.dp))
            .isEqualTo(48.dp + row)
    }

    @Test
    fun `an inset the policy already applied is not paid twice`() {
        // The case that shipped wrong: a cutout inset deep enough to clear the
        // status bar had already moved the label, and the reservation added a
        // whole bar inset on top of it.
        assertThat(reservedForTheCornerRow(barInset = 48.dp, rowMoved = 0.dp, applied = 48.dp))
            .isEqualTo(row)
    }

    @Test
    fun `hardware that drives the row inward is room the label leaves it`() {
        assertThat(reservedForTheCornerRow(barInset = 48.dp, rowMoved = 30.dp, applied = 48.dp))
            .isEqualTo(row + 30.dp)
    }

    @Test
    fun `a policy that has already cleared the row reserves nothing`() {
        assertThat(reservedForTheCornerRow(barInset = 48.dp, rowMoved = 0.dp, applied = 400.dp))
            .isEqualTo(0.dp)
    }

    @Test
    fun `the reservation never goes negative and pulls the label back up`() {
        assertThat(reservedForTheCornerRow(barInset = 0.dp, rowMoved = 0.dp, applied = 1000.dp))
            .isGreaterThanOrEqualTo(0.dp)
    }
}
