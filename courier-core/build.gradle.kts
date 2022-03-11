import plugin.KotlinLibraryConfigurationPlugin

apply<KotlinLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task-java.gradle")
apply("$rootDir/gradle/script-ext.gradle")

ext {
    set("name", "courier-core")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("java-library")
    kotlin("jvm")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    nonPublicMarkers.add("androidx.annotation.RestrictTo")
}

dependencies {
    implementation(deps.kotlin.stdlib.core)
    api(deps.rx.reactiveStreams)
    implementation(deps.android.androidx.annotation)
    testImplementation(deps.android.test.kotlinTestJunit)
}

