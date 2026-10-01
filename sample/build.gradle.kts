plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.damson.hardwareinsets.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.damson.hardwareinsets.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true

            all {
                // Robolectric reaches jdk.internal.access to set up the
                // application state, which java.base exports to nobody. Without
                // this every Robolectric test dies before it runs, with
                // "Failed to interact with raw FileDescriptor internals;
                // perhaps JRE has changed?", which names neither the module nor
                // the flag.
                it.jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
            }
        }
    }

    buildTypes {
        release {
            // Unsigned, and never released. The sample exists to be run and
            // read, not to ship.
            isMinifyEnabled = false
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // The published coordinates would work here too. A project dependency is
    // deliberate: an API change breaks the sample in the same build rather
    // than after a release.
    implementation(project(":hardware-insets"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    // Real icons rather than glyphs borrowed from the keyboard.
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Merges a ComponentActivity into the debug manifest, which the preview
    // render test launches into. As testImplementation the AAR is on the
    // classpath but its manifest is not merged, and the test dies resolving
    // an activity that is genuinely not declared anywhere.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(composeBom)
    testImplementation(libs.junit)
    testImplementation(libs.assertj)
    // The previews are composed by a test, because nothing else opens them.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
