import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
}

val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    val secrets: Properties = Properties()
    // Load the secrets file and add all properties to project.ext
    secrets.load(java.io.FileInputStream(secretsFile))
    secrets.forEach { (key, value) ->
        project.extra.set("args.$key", value)
    }
} else {
    logger.warn("No secrets.properties found. Create this file in project root to store credentials.")
    logger.warn("Example content: email=your_email@example.com\\npassword=your_password")
}

// The Node.js / Yarn downloads for :web come from the repositories in settings.gradle.kts (FAIL_ON_PROJECT_REPOS);
// a null base URL stops the Kotlin plugin from registering its own.
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}
plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
    the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
}
allprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}

allprojects {
    // Language flags for every compilation (JVM, Android, Wasm): the shared modules use explicit backing fields too.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xcollection-literals")
            freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
