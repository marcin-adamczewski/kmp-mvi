import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Project
import org.gradle.api.problems.internal.GradleCoreProblemGroup.versionCatalog
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.getting
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.invoke
import kotlin.jvm.kotlin

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
fun Project.configureMultiplatform(
    ext: KotlinMultiplatformExtension,
    androidLibrary: Boolean = false,
    configure: KotlinHierarchyBuilder.Root.() -> Unit = {},
) = ext.apply {
    explicitApi()
    jvmToolchain(17)

    val libs by versionCatalog

    jvm() {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    if (androidLibrary) {
        val moduleName = path.split(":").drop(2).joinToString(".")
        val androidNamespace = if (moduleName.isEmpty() || moduleName == "core") {
            "com.adamczewski.kmpmvi"
        } else {
            "com.adamczewski.kmpmvi.$moduleName"
        }
        androidLibrary {
            namespace = androidNamespace
            compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
            minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
            withJava()
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_11)
            }
            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    mingwX64()

    linuxX64()
    linuxArm64()

    wasmJs {
        nodejs()
        browser()
    }

    js(IR) {
        nodejs()
        browser()
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlin.concurrent.atomics.ExperimentalAtomicApi")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
        }
    }
}

val Project.versionCatalog: Lazy<VersionCatalog>
    get() = lazy { extensions.getByType<VersionCatalogsExtension>().named("libs") }