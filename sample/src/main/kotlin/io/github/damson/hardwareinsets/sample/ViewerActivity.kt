package io.github.damson.hardwareinsets.sample

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import io.github.damson.hardwareinsets.CutoutShape
import io.github.damson.hardwareinsets.cutoutShape
import io.github.damson.hardwareinsets.drawBehindTheHardware
import io.github.damson.hardwareinsets.hideTheSystemBars
import io.github.damson.hardwareinsets.stopReportingCutoutShape

/**
 * A full-bleed media viewer, which wants the opposite of what a form wants: the
 * picture under the camera, and the controls kept off it.
 *
 * The window-level options live here because they are window-level: the cutout
 * mode is an attribute and hiding a bar is a controller call, neither of which
 * a composable owns.
 */
class ViewerActivity : ComponentActivity() {

    private lateinit var insetHost: View
    private lateinit var cutout: State<CutoutShape>
    private var options by mutableStateOf(ViewerOptions())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyWindow(options)
        setContentView(buildContentView())
        cutout = insetHost.cutoutShape()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Returning from another app or dismissing a dialog can leave a bar
        // behind, so this is asked for again rather than once.
        if (hasFocus) applyWindow(options)
    }

    override fun onDestroy() {
        insetHost.stopReportingCutoutShape()
        super.onDestroy()
    }

    private fun onOptions(next: ViewerOptions) {
        applyWindow(next)
        options = next
    }

    /**
     * The library hides bars but has no way to show them again, so the sample
     * reaches past it for that half. Worth knowing before you build an app that
     * toggles them.
     */
    private fun applyWindow(options: ViewerOptions) {
        drawBehindTheHardware(cutoutMode = options.cutoutMode)
        val bars = WindowInsetsCompat.Type.systemBars()
        if (options.areBarsHidden) {
            hideTheSystemBars(types = bars)
        } else {
            WindowCompat.getInsetsController(window, window.decorView).show(bars)
        }
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
        addView(
            ComposeView(context).apply {
                setContent {
                    SampleTheme {
                        ViewerScreen(
                            cutout = cutout.value,
                            options = options,
                            onOptions = ::onOptions,
                            onClose = { finish() },
                        )
                    }
                }
            }
        )
    }
}
