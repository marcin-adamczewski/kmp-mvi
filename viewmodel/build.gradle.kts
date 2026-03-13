import com.android.build.api.dsl.androidLibrary
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
            api(project(":core"))
            implementation(libs.androidx.lifecycle.viewmodel)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.coroutines.test)
            implementation(project(":test"))
        }
    }
}
