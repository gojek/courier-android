import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

android {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}


ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "app-state-manager")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id("kotlin-android")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.appstatemanager.BuildConfig")
}

dependencies {
    implementation(deps.kotlin.stdlib.core)
    implementation(deps.android.lifecycle.extensions)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
