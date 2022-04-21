import plugin.KotlinLibraryConfigurationPlugin

apply<KotlinLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "paho")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
    set("minimumCoverage", "0.0")
}

plugins {
    id("java-library")
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(deps.kotlin.stdlib.core)
    testImplementation(deps.android.test.kotlinTestJunit)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
