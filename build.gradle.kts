import java.util.Properties

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
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
