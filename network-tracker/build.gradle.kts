import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "network-tracker")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.4")
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.networktracker.BuildConfig")
}

dependencies {
    if (project.ext.get("isCI") as Boolean) {
        api("com.gojek.android:courier-core:$version")
    } else {
        api(project(":courier-core"))
    }
    implementation(deps.android.lifecycle.extensions)

    testImplementation(deps.android.test.kotlinTestJunit)
}