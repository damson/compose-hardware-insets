import dev.detekt.gradle.extensions.DetektExtension
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    // Both declared here, or a subproject asking for the other one is told the
    // plugin is already on the classpath with an unknown version.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.binary.compatibility)
    alias(libs.plugins.kover)
    alias(libs.plugins.kotlinter) apply false
    alias(libs.plugins.detekt) apply false
}

// Style and static analysis are applied here rather than per module, because a
// rule that holds in one module and not the other is worse than no rule: the
// next module added would start unchecked and nothing would say so.
subprojects {
    apply(plugin = "org.jmailen.kotlinter")
    apply(plugin = "dev.detekt")

    // The pre-commit hook judges what is being committed, not the whole tree,
    // so a file someone is still editing elsewhere does not block a commit
    // that has nothing to do with it. Neither plugin offers a per-file entry
    // point, so these two take the list and narrow it to this module's own
    // files; `check` still runs the whole-project tasks.
    // NUL-separated, through a file: a comma is legal in a filename, and
    // splitting the list on one produced paths that do not exist, which this
    // then dropped without a word. An empty source set is a green run, which
    // is the worst shape a check can fail in.
    val mine = providers.gradleProperty("stagedList").map { path ->
        rootProject.file(path).readText()
            .split('\u0000')
            .filter { it.isNotBlank() }
            .map { rootProject.file(it) }
            .filter { it.exists() && it.startsWith(projectDir) }
    }.orElse(emptyList())

    tasks.register<org.jmailen.gradle.kotlinter.tasks.LintTask>("lintStaged") {
        ktlintClasspath.from(configurations.named("ktlint"))
        source(mine)
    }

    tasks.register<dev.detekt.gradle.Detekt>("detektStaged") {
        detektClasspath.from(configurations.named("detekt"))
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        buildUponDefaultConfig.set(true)
        source(mine)
    }

    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        // The shipped defaults stay in force under the file above, which says
        // only where this project differs. Without this, every rule that file
        // does not mention is switched off.
        buildUponDefaultConfig.set(true)
        parallel.set(true)
    }
}

// Hooks are not cloned, so this is opt-in per clone and stays a task rather
// than something the build does on configuration: a build that rewrites git
// config behind you is worse than a hook nobody installed.
tasks.register("installGitHooks") {
    group = "verification"
    description = "Points git at .githooks, so the pre-commit style check runs."
    notCompatibleWithConfigurationCache("Writes git config, which is not a build output.")
    // Captured here rather than read in the action: `user.dir` under the
    // daemon is the directory gradlew was launched from, so `-p` elsewhere
    // would have written the setting into whichever repo the shell was in.
    val here = layout.projectDirectory.asFile
    doLast {
        // ProcessBuilder rather than the exec providers: those capture the
        // project, which a task action may not hold.
        val run = ProcessBuilder("git", "config", "core.hooksPath", ".githooks")
            .directory(here)
            .redirectErrorStream(true)
            .start()
        val said = run.inputStream.bufferedReader().readText().trim()
        check(run.waitFor() == 0) { "git config failed: $said" }
        logger.lifecycle("git hooks installed: .githooks/pre-commit runs on commit")
        logger.lifecycle("This is per clone, shared by every worktree, and it takes")
        logger.lifecycle("precedence over a global core.hooksPath and over .git/hooks.")
    }
}

apiValidation {
    // The sample is not part of the published surface.
    ignoredProjects.add("sample")
}

// A second report, aggregating the library, filtered to the one package that has
// no excuse. Kover 0.9 has no per-rule filters, so a package-scoped gate and a
// whole-library gate cannot live in the same report: the module keeps the 90%
// line gate, and the 100% one lives here.
dependencies {
    kover(project(":hardware-insets"))
}

kover {
    reports {
        // Inside `total`, not beside it: filters set on `reports` itself are
        // defaults that the aggregated report does not pick up, and the gate then
        // measures the whole library and reads as a failure of the rule rather
        // than of the configuration.
        total {
            filters {
                includes {
                    classes("com.devddagnet.hardwareinsets.lib.domain.*")
                }
            }

            verify {
                rule("The pure geometry is covered completely") {
                    // Reached, not aspired to. Nothing in `domain` touches a
                    // framework object it cannot be handed, so these are exactly
                    // the functions the README argues you can test against hardware
                    // you do not own.
                    //
                    // The cost of the branch half, which is real: an inline stdlib
                    // call brings its own branches into this package's count, so
                    // two of the three this gate first closed were inside
                    // `maxOfOrNull` rather than in code written here. The next
                    // `sumOf` or `associate` in `domain` can add a branch
                    // reachable only by input shaped to the standard library. When
                    // that happens the honest fix is a test of the behaviour that
                    // needs the shape, or a narrower bound, not a test written to
                    // the counter.
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.LINE
                    }
                    bound {
                        minValue = 100
                        coverageUnits = CoverageUnit.BRANCH
                    }
                }
            }
        }
    }
}
