
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
        }

        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.kotlin.test)
            implementation(libs.turbine)
            api(project(":library"))
        }
    }
}