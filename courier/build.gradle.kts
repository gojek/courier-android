import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "courier")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.courier.BuildConfig")
}

dependencies {
    if (project.ext.get("isCI") as Boolean) {
        api("com.gojek.android:courier-core:$version")
        api("com.gojek.android:mqtt-client:$version")
    } else {
        api(project(":courier-core"))
        api(project(":mqtt-client"))
    }

    implementation(deps.rx.java)
}
