plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.github.naixx.viewapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.naixx.viewapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        project.properties.forEach { key, value ->
            if (key.startsWith("args.") && value != null) {
                android.defaultConfig.testInstrumentationRunnerArguments[key.replace("args.", "")] = value.toString()
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
            buildConfigField("String", "EMAIL", "\"${findProperty("email") ?: ""}\"")
            buildConfigField("String", "PASSWORD", "\"${findProperty("password") ?: ""}\"")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "EMAIL", "\"${findProperty("email") ?: ""}\"")
            buildConfigField("String", "PASSWORD", "\"${findProperty("password") ?: ""}\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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

dependencies {
    implementation(project(":network"))
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
    implementation(libs.voyager.koin)
    implementation(libs.voyager.tab.navigator)

    implementation(libs.kmp.libs.logger)
    implementation(libs.kmp.libs.prefs)
    implementation(libs.kmp.libs.compose)
    implementation(libs.multiplatform.settings.no.arg)
    implementation(libs.multiplatform.settings.observable)
    implementation(libs.multiplatform.settings.serialization)
    debugImplementation(libs.slf4j.simple)
    implementation(libs.sonner)



    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.koin.core)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose)
//    implementation(libs.koin.compose.viewmodel)
    implementation(libs.ktorfit)
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
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    testImplementation(libs.robolectric)
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.koin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.koin.test)
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
