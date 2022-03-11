import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "courier-auth-http")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.6")
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.courier.authhttp.BuildConfig")
}

dependencies {
    if (project.ext.get("isCI") as Boolean) {
        implementation("com.gojek.android:mqtt-client:$version")
        implementation("com.gojek.android:courier-core:$version")
    } else {
        implementation(project(":mqtt-client"))
        implementation(project(":courier-core"))
    }
    implementation(deps.square.retrofit)

    testImplementation(deps.android.test.kotlinTestJunit)
}