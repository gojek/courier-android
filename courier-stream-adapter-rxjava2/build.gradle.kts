import plugin.KotlinLibraryConfigurationPlugin

apply<KotlinLibraryConfigurationPlugin>()
apply("$rootDir/gradle/publish-artifact-task-java.gradle")
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("name", "courier-stream-adapter-rxjava2")
    set("publish", true)
    set("version", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("java-library")
    kotlin("jvm")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

dependencies {
    api(project(":courier-core"))
    api(deps.rx.java)
    implementation(deps.kotlin.stdlib.core)

    testImplementation(deps.android.test.kotlinTestJunit)
}