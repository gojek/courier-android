import plugin.KotlinLibraryConfigurationPlugin

apply<KotlinLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "courier-message-adapter-kotlin-serialization")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("java-library")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

dependencies {
    api(project(":courier-core"))
    api(deps.kotlin.serializationJson)
    implementation(deps.kotlin.stdlib.core)

    testImplementation(deps.android.test.kotlinTestJunit)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
