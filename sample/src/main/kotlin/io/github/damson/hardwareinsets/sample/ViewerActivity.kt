package io.github.damson.hardwareinsets.sample

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowInsetsCompat
import io.github.damson.hardwareinsets.CutoutMode
import io.github.damson.hardwareinsets.CutoutShape
import io.github.damson.hardwareinsets.cutoutShape
import io.github.damson.hardwareinsets.drawBehindTheHardware
import io.github.damson.hardwareinsets.hideTheSystemBars
import io.github.damson.hardwareinsets.stopReportingCutoutShape

/**
 * A full-bleed media viewer, which wants the opposite of what a form wants: the
 * picture under the camera, and the controls kept off it.
 *
 * Every choice here is deliberately not the one the library defaults to, because
 * a sample that takes the defaults proves only that the defaults compile.
 */
class ViewerActivity : ComponentActivity() {

    private lateinit var insetHost: View
    private lateinit var cutout: State<CutoutShape>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ALWAYS, because a picture that stops short of the camera is
        // letterboxed for no reason. Most apps should pass SHORT_EDGES.
        drawBehindTheHardware(cutoutMode = CutoutMode.ALWAYS)
        // Only the navigation bar. The clock stays, which is what separates a
        // viewer from a game and is why this takes a mask rather than a boolean.
        hideTheSystemBars(types = WindowInsetsCompat.Type.navigationBars())

        setContentView(buildContentView())
        cutout = insetHost.cutoutShape()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Returning from another app or dismissing a dialog can leave the bar
        // behind, so this is asked for again rather than once.
        if (hasFocus) hideTheSystemBars(types = WindowInsetsCompat.Type.navigationBars())
    }

    override fun onDestroy() {
        insetHost.stopReportingCutoutShape()
        super.onDestroy()
    }

    /**
     * A sibling for the listener, because a `View` holds exactly one
     * `OnApplyWindowInsetsListener` and `ComposeView.setContent` claims it. On
     * the `ComposeView` the dispatch would still arrive, the listener would just
     * no longer be ours, and the cutout would read empty forever with nothing
     * saying so.
     */
    private fun buildContentView(): FrameLayout = FrameLayout(this).apply {
        insetHost = View(context).also { addView(it, 1, 1) }
        addView(ComposeView(context).apply { setContent { ViewerScreen(cutout.value) } })
    }
}
