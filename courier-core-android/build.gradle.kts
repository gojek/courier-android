import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "courier-core-android")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
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
    ignoredClasses.add("com.gojek.courier.core.BuildConfig")
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    api(project(":courier-core"))
    implementation(deps.android.androidx.lifecycleExtensions)
    implementation(deps.android.androidx.lifecycleCommons)
    implementation(deps.android.androidx.lifecycleProcess)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
