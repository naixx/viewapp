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
    alias(libs.plugins.metro) // AppViewModelFactory contributes itself to AppScope
    alias(libs.plugins.kotlinSerialization) // the navigation routes are @Serializable (the back stack survives a config change)
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
            api(libs.navigation3.ui) // MainScreen: NavDisplay over the back stack; the shells compose MainScreen()
            api(libs.lifecycle.viewmodel.navigation3) // a ViewModelStore per NavEntry: screenViewModel()
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
            api(libs.coil.compose) // images everywhere in the shared UI; the frames reach it through FrameFetcher
            api(libs.sonner) // toasts: EventBus events carry its ToastType
            api(libs.metrox.viewmodel.compose) // view models: @ViewModelKey map, AppViewModelFactory, metroViewModel()
            implementation(compose.components.resources) // ByteArray.decodeToImageBitmap for the frame histogram
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
