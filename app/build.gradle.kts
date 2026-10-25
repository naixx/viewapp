import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.github.naixx.viewapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.github.naixx.viewapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        project.properties.forEach { key, value ->
            if (key.startsWith("args.") && value != null) {
                testInstrumentationRunnerArguments[key.replace("args.", "")] = value.toString()
            }
        }
    }

    signingConfigs {
        create("release") {
            storePassword = getArg("RELEASE_STORE_PASSWORD")
            keyAlias = getArg("RELEASE_KEY_ALIAS")
            keyPassword = getArg("RELEASE_KEY_PASSWORD")
            storeFile = getArg("RELEASE_STORE_FILE")?.let { file(it) }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += listOf(
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
        )
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

androidComponents {
    onVariants { variant ->
        val email = project.findProperty("email")?.toString() ?: ""
        val password = project.findProperty("password")?.toString() ?: ""
        variant.buildConfigFields?.put(
            "EMAIL",
            BuildConfigField(
                "String",
                "\"$email\"",
                "User email"
            )
        )
        variant.buildConfigFields?.put(
            "PASSWORD",
            BuildConfigField(
                "String",
                "\"$password\"",
                "User password"
            )
        )
    }
}

dependencies {
    implementation(project(":network"))
    implementation(project(":core"))
    implementation(project(":ui")) // theme, toasts, formatting, shared components (step 9)
    implementation(project(":db"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.lifecycle)
    implementation(libs.lifecycle.viewModelKtx)


    implementation(libs.kmp.libs.logger)
    implementation(libs.kmp.libs.prefs)
    implementation(libs.multiplatform.settings.no.arg)
    implementation(libs.multiplatform.settings.observable)
    implementation(libs.multiplatform.settings.serialization)
    debugImplementation(libs.slf4j.simple)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.metrox.android) // MetroAppComponentFactory: the Activity and the service are built by the graph

    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.jetbrains.kotlin.test.junit) // used to come with koin-test
    testImplementation(libs.androidx.ui.test.junit4) // ViewModelScopesTest: Compose + Navigation 3 under Robolectric
    testImplementation(libs.ktor.client.core) // RobolectricTest builds its own client

    androidTestImplementation(libs.jetbrains.kotlin.test.junit)
    androidTestImplementation(libs.ktor.client.core) // InstrumentedTests builds its own Ktorfit
    androidTestImplementation(libs.ktorfit)
    androidTestImplementation(libs.ktorfit.converters.flow)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.slf4j.simple)
    androidTestImplementation(libs.mockk.android)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.withType<Test> {
    project.properties.forEach { (key, value) ->
        if (key.startsWith("args.") && value != null) {
            systemProperty(key.replace("args.", ""), value.toString())
        }
    }
}

fun getArg(name: String): String? {
    val value = project.findProperty("args.$name") as String?
    return value
}
