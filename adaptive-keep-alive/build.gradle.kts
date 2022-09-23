import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "adaptive-keep-alive")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
    set("minimumCoverage", "0.9")
    set(
        "fileFilter",
        listOf(
            "**/utils/**",
            "**/model/**",
            "**/config/**",
            "**/constants/**",
            "**/**NoOp*",
            "**/**Factory*"
        )
    )
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

    implementation(project(":paho"))
    implementation(project(":courier-core"))
    implementation(project(":mqtt-pingsender"))
    implementation(project(":network-tracker"))

    testImplementation(deps.android.test.mockitoCore)
    testImplementation(deps.android.test.kotlinTestJunit)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
