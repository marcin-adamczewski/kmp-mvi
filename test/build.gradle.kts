@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    configureMultiplatform(
        ext = this,
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
            implementation(libs.coroutines.test)
            implementation(libs.kotlin.test)
            api(libs.turbine)
            api(project(":core"))
        }
    }
}
