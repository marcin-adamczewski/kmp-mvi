plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

dependencies {
    implementation(libs.android.gradle)
    implementation(libs.kotlin.gradle)
    //compileOnly(libs.compose.gradlePlugin) // If using Compose Multiplatform
}
