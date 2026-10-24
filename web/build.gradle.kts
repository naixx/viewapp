import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

// The browser client the VIEW serves itself (docs/2026-10-24-web-client-plan.md): Compose for Web on wasmJs
// over the shared :network protocol module. Output: build/dist/wasmJs/productionExecutable (+ .gz siblings).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
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
            implementation(project(":core")) // brings :network as api

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.napier)
            implementation(libs.kmp.libs.logger)
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

/**
 * Copies the bundle into the firmware tree (`frontend/app/`, served at /app by `system/app.js`), replacing what is
 * there. Only the `.gz` of a file that has one goes in — the firmware middleware serves it to every browser, and the
 * repo grows by ≈4 MB per release instead of ≈15 — plus the small plain files; source maps stay out.
 * `-PfirmwareDir=<path>` overrides the default sibling checkout.
 */
val syncToFirmware by tasks.registering {
    group = "distribution"
    description = "Copies the bundle (.gz only) into <firmwareDir>/frontend/app."
    dependsOn(wasmJsGzipDistribution)
    val dist = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
    val firmwareDir = (project.findProperty("firmwareDir") as String?) ?: "../VIEW"
    val target = rootProject.layout.projectDirectory.dir(firmwareDir).dir("frontend/app")
    doLast {
        val root = dist.get().asFile
        val out = target.asFile
        check(out.parentFile.isDirectory) { "No firmware checkout at ${out.parentFile.parentFile}; pass -PfirmwareDir" }
        out.deleteRecursively()
        root.walkTopDown()
            .filter { it.isFile && !it.name.endsWith(".map") }
            .filter { it.name.endsWith(".gz") || !File(it.path + ".gz").exists() }
            .forEach { file ->
                val dest = File(out, file.relativeTo(root).path)
                dest.parentFile.mkdirs()
                file.copyTo(dest)
                dest.setLastModified(file.lastModified())
            }
        println("Synced to $out: " + out.walkTopDown().filter { it.isFile }.map { it.name }.sorted().joinToString())
    }
}
