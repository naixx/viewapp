import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

// The browser client the VIEW serves itself (docs/2026-10-24-web-client-plan.md): Compose for Web on wasmJs
// over the shared :network protocol module. Output: build/dist/wasmJs/productionExecutable (+ .gz siblings).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "viewweb"
        browser {
            commonWebpackConfig {
                outputFileName = "viewweb.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":network"))

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.napier)
        }
    }
}

/**
 * Writes a `.gz` next to every distributable file: the VIEW's single core can't compress a multi-MB bundle per
 * request, so `system/app.js` serves these pre-compressed copies instead (firmware doc 2026-10-23-web-ui-gzip-mdns).
 */
val wasmJsGzipDistribution by tasks.registering {
    group = "distribution"
    description = "Builds the production bundle and adds .gz siblings for the firmware to serve."
    dependsOn("wasmJsBrowserDistribution")
    val dist = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    doLast {
        val root = dist.get().asFile
        root.walkTopDown().filter { it.isFile && it.name.endsWith(".gz") }.forEach { it.delete() }
        root.walkTopDown()
            .filter { it.isFile && it.length() > 1024 && !it.name.endsWith(".map") }
            .forEach { file ->
                val gz = File(file.path + ".gz")
                object : GZIPOutputStream(gz.outputStream()) {
                    init { def.setLevel(Deflater.BEST_COMPRESSION) }
                }.use { out -> file.inputStream().use { it.copyTo(out) } }
                gz.setLastModified(file.lastModified())
            }
    }
}
