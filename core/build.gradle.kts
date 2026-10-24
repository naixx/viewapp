import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Platform-free application logic shared by the Android app and the browser client (docs/2026-10-24-shared-ui-architecture.md):
// the device session/state, stores and downloaders live here, UI does not. Like :network it has no Android code, so a
// jvm target serves the Android modules and the JVM tests, and wasmJs serves :web.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    jvm()
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":network"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.collections.immutable)
            api(libs.kotlinx.io.core) // files on every platform that has them (the browser hasn't)
            implementation(libs.kmp.libs.logger)
        }
        jvmTest.dependencies {
            implementation(libs.jetbrains.kotlin.test.junit)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
