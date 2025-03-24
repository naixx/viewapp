plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin
}

dependencies {
    implementation(libs.kmp.libs.core)
    api(libs.ktor.serialization.kotlinx.json)

    compileOnly("androidx.room:room-common:2.6.1")

    testImplementation(libs.jetbrains.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
