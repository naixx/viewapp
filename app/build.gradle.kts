import com.android.build.api.variant.BuildConfigField

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
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

    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)
    implementation(libs.voyager.tab.navigator)

    implementation(libs.kmp.libs.logger)
    implementation(libs.kmp.libs.prefs)
    implementation(libs.multiplatform.settings.no.arg)
    implementation(libs.multiplatform.settings.observable)
    implementation(libs.multiplatform.settings.serialization)
    debugImplementation(libs.slf4j.simple)

    implementation(libs.kotlinx.datetime)



    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.ktorfit)
    implementation(libs.metrox.android) // MetroAppComponentFactory: the Activity and the service are built by the graph
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    api(libs.ktor.serialization.kotlinx.json)
//    api(libs.kotlinx.datetime)
//    api(libs.kotlinx.collections.immutable)
    implementation(libs.ktorfit.converters.flow)
    implementation(libs.ktor.client.cio)

    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    implementation(libs.kotlinx.collections.immutable)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.jetbrains.kotlin.test.junit) // used to come with koin-test
    testImplementation(libs.androidx.ui.test.junit4) // ViewModelScopesTest: Compose + Voyager under Robolectric

    androidTestImplementation(libs.jetbrains.kotlin.test.junit)
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
