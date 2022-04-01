import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "timer-pingsender")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.9")
    set("fileFilter", listOf(
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
    ignoredClasses.add("com.gojek.timer.pingsender.BuildConfig")
}

dependencies {
    api(project(":mqtt-pingsender"))
    implementation(project(":courier-core-android"))
    implementation(deps.android.androidx.annotation)

    testImplementation(deps.android.test.kotlinTestJunit)
}