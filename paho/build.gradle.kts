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

    implementation(project(":paho-common"))

    implementation(deps.kotlin.stdlib.core)
    implementation("com.squareup.okio:okio:3.2.0")

    compileOnly("org.robolectric:android-all:13-robolectric-9030017")
    compileOnly("org.bouncycastle:bcprov-jdk15to18:1.71")
    compileOnly("org.bouncycastle:bctls-jdk15to18:1.71")
    compileOnly("org.conscrypt:conscrypt-openjdk-uber:2.5.2")
    compileOnly("org.openjsse:openjsse:1.1.10")

    testImplementation(deps.android.test.kotlinTestJunit)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
