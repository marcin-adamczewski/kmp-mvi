plugins {
    alias(sampleLibs.plugins.androidApplication) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(sampleLibs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply  false
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.junit5) apply false
}

val localProperties = java.util.Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val localVersion = localProperties.getProperty("version") ?: localProperties.getProperty("VERSION_NAME")

if (localVersion != null) {
    allprojects {
        version = localVersion
    }
}
