plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

        project.properties.forEach { (key, value) ->
            if (key.startsWith("args.") && value != null) {
                android.defaultConfig.testInstrumentationRunnerArguments[key.replace("args.", "")] = value.toString()
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "EMAIL", "\"${findProperty("email") ?: ""}\"")
            buildConfigField("String", "PASSWORD", "\"${findProperty("password") ?: ""}\"")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "EMAIL", "\"${findProperty("email") ?: ""}\"")
            buildConfigField("String", "PASSWORD", "\"${findProperty("password") ?: ""}\"")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("org.jmdns:jmdns:3.6.0")

    implementation(libs.kmp.libs.logger)
    implementation(libs.kmp.libs.prefs)
    implementation(libs.multiplatform.settings.no.arg)
    implementation(libs.multiplatform.settings.observable)
    implementation(libs.multiplatform.settings.serialization)
    debugImplementation(libs.slf4j.simple)



    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.koin.core)
    implementation(libs.koin.android)
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
