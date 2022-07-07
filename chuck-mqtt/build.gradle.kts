import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

android {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs +=  "-Xjvm-default=all"
    }

    buildFeatures {
        // Enables Jetpack Compose for this module
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion =  "1.1.0-beta03" //versions.composeVersion
    }
}

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "chuck-mqtt-modern")
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
    ignoredPackages.addAll(listOf("com.gojek.chuckmqtt.internal.data.local.room", "com.gojek.chuckmqtt.internal"))
    ignoredClasses.add("com.gojek.chuckmqtt.BuildConfig")
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    api(project(":mqtt-client"))
    api(project(":paho"))
    implementation(project(":courier-core-android"))

    implementation(deps.kotlin.stdlib.core)

    implementation(deps.android.gson)

    implementation(deps.android.androidx.lifecycleExtensions)
    implementation(deps.android.androidx.lifecycleCommons)

    implementation(deps.android.androidx.appcompact)
    implementation(deps.android.androidx.fragmentExtensions)
    implementation(deps.android.androidx.coreKtx)
    implementation(deps.android.androidx.constraintLayout)
    implementation(deps.android.androidx.supportV4)
    implementation(deps.android.androidx.recyclerView)
    implementation(deps.android.androidx.material)

    implementation(deps.android.room.roomRuntime)
    implementation(deps.android.room.roomRxJava)
    kapt(deps.android.room.roomCompiler)

    implementation(deps.rx.java)
    implementation(deps.rx.android)
    implementation(deps.rx.rxKotlin)
    implementation(deps.rx.rx3BindingCore)

    implementation(deps.square.okio)

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
