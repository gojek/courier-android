import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "alarm-pingsender")
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
    ignoredClasses.add("com.gojek.alarm.pingsender.BuildConfig")
}

dependencies {
    if (project.ext.get("isCI") as Boolean) {
        api("com.gojek.android:mqtt-pingsender:$version")
        implementation("com.gojek.android:courier-core-android:$version")
    } else {
        api(project(":mqtt-pingsender"))
        implementation(project(":courier-core-android"))
    }
    implementation(deps.android.lifecycle.extensions)

    testImplementation(deps.android.test.kotlinTestJunit)
}