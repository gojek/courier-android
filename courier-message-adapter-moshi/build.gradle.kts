import plugin.KotlinLibraryConfigurationPlugin

apply<KotlinLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "courier-message-adapter-moshi")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
    set("minimumCoverage", "0.8")
}

plugins {
    id("java-library")
    kotlin("jvm")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

dependencies {
    api(project(":courier-core"))
    api(deps.square.moshi)
    implementation(deps.kotlin.stdlib.core)

    testImplementation(deps.android.test.kotlinTestJunit)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
