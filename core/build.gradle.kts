@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id(libs.plugins.kotlinMultiplatform.get().pluginId)
    id(libs.plugins.android.kotlin.multiplatform.library.get().pluginId)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    configureMultiplatform(
        ext = this,
        androidLibrary = true,
    )

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
            implementation(project(":test"))
        }

        jvmTest.dependencies {
            implementation(libs.junit.api)
            implementation(libs.junit.engine)
            implementation(libs.junit.params)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
