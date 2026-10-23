
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // jvm is what :app and :db consume (Android resolves the JVM variant) and where the fast protocol tests run.
    jvm()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
                    "${project.rootProject.projectDir.absolutePath}/compose_compiler_config.conf"
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kmp.libs.core)
            implementation(libs.kmp.libs.logger)

            implementation(libs.koin.core)
            implementation(libs.ktorfit)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            api(libs.ktor.serialization.kotlinx.json)
//            api(libs.kotlinx.datetime)
            api(libs.kotlinx.collections.immutable)
            implementation(libs.ktorfit.converters.flow)

            // Room annotations on the shared Clip / TimelapseClipInfo entities (see CLAUDE.md, module structure).
            compileOnly(libs.room.common)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
        }
        jvmTest.dependencies {
            implementation(libs.jetbrains.kotlin.test.junit)
            implementation(libs.mockk)
            implementation(libs.koin.test)
            implementation(libs.slf4j.simple)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<Test> {
    project.properties.forEach { (key, value) ->
        if (key.startsWith("args.") && value != null) {
            systemProperty(key.replace("args.", ""), value.toString())
        }
    }
}
