package io.github.damson.hardwareinsets

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Holds the library's layers apart by reading the imports off `src/main`.
 *
 * Three layers, and the dependencies only ever point inward:
 *
 * - `domain` is the shapes, the edges and the maths. It may use value types
 *   and constants, and it may not touch a framework object or Compose. This is
 *   what makes `cornerClearanceFor` answerable about hardware nobody owns, and
 *   that claim is the library's whole argument.
 * - `platform` reads the real window: a `View`, an `Activity`, the constants
 *   the framework wants. It knows `domain`.
 * - The root package is the Compose front door. It knows both.
 *
 * A new file joins these rules by existing. Nothing has to be registered.
 */
class ArchitectureTest {

    private val sourceRoot: File =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "hardware-insets/src/main/kotlin") }
            .firstOrNull { it.isDirectory }
            ?: error("hardware-insets/src/main/kotlin not found above ${System.getProperty("user.dir")}")

    private val sources: List<File> =
        sourceRoot.walkTopDown().filter { it.extension == "kt" }.toList()

    /**
     * An empty scan passes every rule below, so the scan is asserted first.
     */
    @Test
    fun `Should be reading the library's sources`() {
        assertThat(sources).isNotEmpty
        assertThat(sources.map { it.name })
            .contains("ScreenEdge.kt", "CutoutShape.kt", "HardwareInsets.kt", "EdgeToEdge.kt")
        assertThat(layer(DOMAIN)).isNotEmpty
        assertThat(layer(PLATFORM)).isNotEmpty
    }

    @Test
    fun `Should keep Compose out of the domain`() {
        // The one exception is the unit types a geometry answer is expressed
        // in. Those are values, not a runtime: nothing about IntOffset needs a
        // composition, a recomposer or a window.
        assertNoImports(
            importedFrom = "androidx.compose",
            inLayer = DOMAIN,
            except = { it.startsWith("androidx.compose.ui.unit.") || it == "androidx.compose.runtime.Immutable" },
        )
    }

    @Test
    fun `Should keep framework objects out of the domain`() {
        // Value types and constants may cross: Rect is a box of four ints and
        // Surface.ROTATION_90 is the number the platform means by it. A View,
        // a Window or an Activity may not, because holding one is what makes a
        // thing need a device to test.
        assertNoImports(
            importedFrom = "android.",
            inLayer = DOMAIN,
            except = { it == "android.graphics.Rect" || it == "android.view.Surface" },
        )
        assertNoImports(importedFrom = "androidx.core.view", inLayer = DOMAIN)
        assertNoImports(importedFrom = "androidx.activity", inLayer = DOMAIN)
    }

    @Test
    fun `Should point every dependency inward`() {
        // The domain is the innermost layer, so nothing it imports may come
        // from the library at all, and the platform may reach the domain but
        // never the Compose front door.
        assertNoImports(importedFrom = LIBRARY, inLayer = DOMAIN)
        assertNoImports(
            importedFrom = LIBRARY,
            inLayer = PLATFORM,
            except = { it.startsWith("$LIBRARY.domain.") },
        )
    }

    @Test
    fun `Should keep the window handling in the platform layer`() {
        // The front door is Compose, and a modifier that reached for an
        // Activity would be a second way to do what drawBehindTheHardware
        // already does, discoverable by nobody.
        assertOnlyInLayer(importedFrom = "androidx.activity", layer = PLATFORM)
        assertOnlyInLayer(importedFrom = "android.view.View", layer = PLATFORM)
    }

    private fun layer(prefix: String): List<File> =
        sources.filter { it.relativeTo(sourceRoot).path.startsWith(prefix) }

    private fun imports(file: File): List<String> =
        file.readLines()
            .filter { it.startsWith("import ") }
            .map { it.removePrefix("import ").substringBefore(" as ").trim() }

    private fun assertNoImports(
        importedFrom: String,
        inLayer: String,
        except: (String) -> Boolean = { false },
    ) {
        val offences = layer(inLayer).flatMap { file ->
            imports(file)
                .filter { it.startsWith(importedFrom) && !except(it) }
                .map { "${file.relativeTo(sourceRoot).path} imports $it" }
        }
        assertThat(offences)
            .describedAs("$inLayer may not import $importedFrom")
            .isEmpty()
    }

    private fun assertOnlyInLayer(importedFrom: String, layer: String) {
        val offences = sources
            .filterNot { it.relativeTo(sourceRoot).path.startsWith(layer) }
            .flatMap { file ->
                imports(file)
                    .filter { it.startsWith(importedFrom) }
                    .map { "${file.relativeTo(sourceRoot).path} imports $it" }
            }
        assertThat(offences)
            .describedAs("only $layer may import $importedFrom")
            .isEmpty()
    }

    private companion object {
        const val LIBRARY = "io.github.damson.hardwareinsets"
        const val DOMAIN = "io/github/damson/hardwareinsets/domain"
        const val PLATFORM = "io/github/damson/hardwareinsets/platform"
    }
}
