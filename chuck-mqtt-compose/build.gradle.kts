import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

android {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs += "-Xjvm-default=all"
    }

    buildFeatures {
        // Enables Jetpack Compose for this module
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.1.0-beta03" //versions.composeVersion
    }
}

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "chuck-mqtt-compose")
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
    ignoredClasses.add("com.gojek.chuckmqtt.BuildConfig")
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    implementation(deps.kotlin.stdlib.core)

    implementation(deps.compose.composeActivity)
    implementation(deps.compose.composeAnimation)
    implementation(deps.compose.composeMaterial)
    implementation(deps.compose.composeViewModel)
    implementation(deps.compose.composeTooling)
    implementation(deps.compose.composeRxjava)
    implementation(deps.compose.composeCoil)
    implementation(deps.compose.composeNavigation)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
