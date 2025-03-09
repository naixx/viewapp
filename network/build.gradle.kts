
plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
                    "${project.rootProject.projectDir.absolutePath}/compose_compiler_config.conf"
        )
    }
}

tasks.withType<Test> {
    project.properties.forEach { (key, value) ->
        if (key.startsWith("args.") && value != null) {
            systemProperty(key.replace("args.", ""), value.toString())
        }
    }
}

dependencies {
    implementation(libs.kmp.libs.core)

    implementation(libs.koin.core)
    implementation(libs.ktorfit)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    api(libs.ktor.serialization.kotlinx.json)
//    api(libs.kotlinx.datetime)
    api(libs.kotlinx.collections.immutable)
    implementation(libs.ktorfit.converters.flow)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.jetbrains.kotlin.test.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.kotlinx.coroutines.test)
}
