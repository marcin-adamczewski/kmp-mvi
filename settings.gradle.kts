pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("sampleLibs") {
            from(files("sample/sampleLibs.versions.toml"))
        }
    }
}

rootProject.name = "kmp-mvi"
include(":library")
include(":test")
include(":sample:composeApp")
