import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "adaptive-keep-alive")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.9")
    set("fileFilter", listOf(
        "**/utils/**",
        "**/model/**",
        "**/config/**",
        "**/constants/**",
        "**/**NoOp*",
        "**/**Factory*"
    ))
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.keepalive.BuildConfig")
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    implementation(deps.kotlin.stdlib.core)
    implementation(deps.android.gson)
    implementation(deps.android.androidx.annotation)

    if (project.ext.get("isCI") as Boolean) {
        implementation("com.gojek.android:paho:$version")
        implementation("com.gojek.android:courier-core:$version")
        implementation("com.gojek.android:mqtt-pingsender:$version")
    } else {
        implementation(project(":paho"))
        implementation(project(":courier-core"))
        implementation(project(":mqtt-pingsender"))
    }

    testImplementation(deps.android.test.kotlinTestJunit)
}

