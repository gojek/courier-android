import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "mqtt-client")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.1")
    set("fileFilter", listOf(
        "**/logging/**",
        "**/utils/**",
        "**/factory/**",
        "**/config/**",
        "**/model/**",
        "**/constants/**",
        "**/**Config*"
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
    ignoredPackages.addAll(listOf("com.gojek.mqtt.persistence"))
    ignoredClasses.add("com.gojek.mqtt.client.BuildConfig")
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    api(project(":mqtt-pingsender"))
    api(project(":courier-core"))
    implementation(project(":paho"))
    implementation(project(":adaptive-keep-alive"))
    implementation(project(":network-tracker"))
    implementation(project(":app-state-manager"))
    implementation(project(":timer-pingsender"))

    implementation(deps.android.lifecycle.extensions)

    implementation(deps.android.room.roomRuntime)
    kapt(deps.android.room.roomCompiler)

    implementation(deps.rx.java)

    testImplementation(deps.android.test.junit)
    testImplementation(deps.android.test.mockito)
    androidTestImplementation(deps.android.test.junitExt)
}