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
    if (project.ext.get("isCI") as Boolean) {
        implementation("com.gojek.android:paho:$version")
        implementation("com.gojek.android:adaptive-keep-alive:$version")
        implementation("com.gojek.android:network-tracker:$version")
        implementation("com.gojek.android:app-state-manager:$version")
        implementation("com.gojek.android:timer-pingsender:$version")
        api("com.gojek.android:mqtt-pingsender:$version")
        api("com.gojek.android:courier-core:$version")
    } else {
        implementation(project(":paho"))
        implementation(project(":adaptive-keep-alive"))
        implementation(project(":network-tracker"))
        implementation(project(":app-state-manager"))
        implementation(project(":timer-pingsender"))
        api(project(":mqtt-pingsender"))
        api(project(":courier-core"))
    }

    implementation(deps.android.lifecycle.extensions)

    implementation(deps.android.room.roomRuntime)
    kapt(deps.android.room.roomCompiler)

    implementation(deps.rx.java)

    testImplementation(deps.android.test.junit)
    testImplementation(deps.android.test.mockito)
    androidTestImplementation(deps.android.test.junitExt)
}