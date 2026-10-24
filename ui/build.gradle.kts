import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/*
 * The shared UI (docs/2026-10-24-shared-ui-architecture.md, steps 9-11): Compose Multiplatform for the Android app and
 * the browser client served by the VIEW. Android comes through AGP's KMP library plugin (one variant, no manifest
 * merging of its own), the browser through wasmJs. Package names stay those of the app, so screens move here without
 * import churn.
 */
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "com.github.naixx.viewapp.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core")) // the components render :network message types; :core brings :network as api
            api(libs.voyager.navigator) // SuperNav exposes Navigator
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
            api(libs.sonner) // toasts: EventBus events carry its ToastType
            implementation(libs.kmp.libs.logger)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.ui.tooling.preview) // the multiplatform androidx.compose.ui.tooling.preview.Preview
        }
    }
}

dependencies {
    // Android Studio renders the common @Preview functions with ui-tooling on the Android runtime classpath.
    androidRuntimeClasspath(compose.uiTooling)
}
