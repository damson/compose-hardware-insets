package io.github.damson.hardwareinsets.sample.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Every preview in the module, composed once.
 *
 * A preview is only checked by someone opening the file, so one that throws is
 * found months later by whoever opens it next, with no idea which change did
 * it. These compose against a window with no cutout of its own and no system
 * bars, which is the arrangement the previews exist for and the one nothing
 * else in the suite covers.
 *
 * It is why the preview functions are `internal` rather than private: reaching
 * them is what makes this a test of the previews rather than a second test of
 * the components inside them.
 *
 * One test each, because a compose rule takes one `setContent` and the second
 * call fails with "has already set content", which reads as a defect in the
 * preview rather than in the test holding it.
 *
 * In `testDebug` rather than `test`, because the `ComponentActivity` these
 * launch into is merged in by `ui-test-manifest`, which is a debug dependency.
 * Under `test` the whole class passes on debug and fails fifteen times on
 * release, resolving an activity that is genuinely declared nowhere.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreviewRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun render(preview: @Composable () -> Unit) {
        compose.setContent { preview() }
        // Not `onRoot().assertExists()`. A root exists the moment `setContent`
        // returns, so asserting it proves only what `setContent` already proved
        // by not throwing. A preview that composes and emits nothing is the
        // failure worth catching, and it is the one a reader of the file cannot
        // see: the IDE shows them an empty rectangle either way.
        assertThat(compose.onAllNodes(readable, useUnmergedTree = true).fetchSemanticsNodes())
            .describedAs("the preview emitted nothing a reader could see")
            .isNotEmpty()
    }

    private companion object {
        /**
         * Something on screen with words on it: every preview here has text, an
         * icon with a description, or a described painting.
         */
        val readable = SemanticsMatcher("carries text or a content description") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.isNotEmpty() == true ||
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.isNotEmpty() == true
        }
    }

    @Test
    fun `Should compose the viewer over a camera in the corner`() =
        render { ViewerScreenPreview() }

    @Test
    fun `Should compose the viewer over a centred punch-hole`() =
        render { ViewerScreenCentredHolePreview() }

    @Test
    fun `Should compose the viewer over curved edges`() =
        render { ViewerScreenCurvedPreview() }

    @Test
    fun `Should compose the viewer with nothing in the way`() =
        render { ViewerScreenWithoutHardwarePreview() }

    @Test
    fun `Should compose the label anchored to the top`() = render { WallLabelPreview() }

    @Test
    fun `Should compose the label anchored to the bottom`() =
        render { WallLabelAtTheBottomPreview() }

    @Test
    fun `Should compose the label leaving room for a corner row`() =
        render { WallLabelBesideTheCornerPreview() }

    @Test
    fun `Should compose the label mid shake`() = render { WallLabelShakingPreview() }

    @Test
    fun `Should compose the corner row`() = render { CornerActionsPreview() }

    @Test
    fun `Should compose the corner row stepped clear of a camera`() =
        render { CornerActionsClearPreview() }

    @Test
    fun `Should compose the settings trigger`() = render { OpenControlsPreview() }

    @Test
    fun `Should compose a step button`() = render { StepButtonPreview() }

    @Test
    fun `Should compose a step button at the end of the gallery`() =
        render { StepButtonDisabledPreview() }

    @Test
    fun `Should compose the controls over a camera in the corner`() =
        render { ControlSheetPreview() }

    @Test
    fun `Should compose the controls with nothing in the way`() =
        render { ControlSheetWithoutHardwarePreview() }
}
