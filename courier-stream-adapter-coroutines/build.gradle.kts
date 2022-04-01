import plugin.KotlinLibraryConfigurationPlugin

apply<KotlinLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task-java.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "courier-stream-adapter-coroutines")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.1")
}

plugins {
    id("java-library")
    kotlin("jvm")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

dependencies {
    api(project(":courier-core"))
    api(deps.kotlin.coroutines.core)
    api(deps.kotlin.coroutines.reactive)
    implementation(deps.kotlin.stdlib.core)

    testImplementation(deps.android.test.kotlinTestJunit)
}