import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "mqtt-pingsender")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.mqtt.pingsender.BuildConfig")
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    if (project.ext.get("isCI") as Boolean) {
        api("com.gojek.android:paho:$version")
        implementation("com.gojek.android:courier-core:$version")
    } else {
        api(project(":paho"))
        implementation(project(":courier-core"))
    }
    implementation(deps.android.androidx.annotation)

    testImplementation(deps.android.test.kotlinTestJunit)
}