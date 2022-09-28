import plugin.AndroidLibraryConfigurationPlugin

apply<AndroidLibraryConfigurationPlugin>()
apply("$rootDir/gradle/script-ext.gradle")

val version = ext.get("gitVersionName")

ext {
    set("PUBLISH_GROUP_ID", "com.gojek.courier")
    set("PUBLISH_ARTIFACT_ID", "workmanager-pingsender")
    set("PUBLISH_VERSION", ext.get("gitVersionName"))
    set("minimumCoverage", "0.8")
    set(
        "fileFilter",
        listOf(
            "**/**Factory*",
            "**/**Config*"
        )
    )
}

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
    id(ScriptPlugins.apiValidator) version versions.apiValidator
}

apiValidation {
    ignoredClasses.add("com.gojek.workmanager.pingsender.BuildConfig")
}

dependencies {
    api(project(":mqtt-pingsender"))
    implementation(project(":courier-core-android"))
    implementation(deps.workManager.runtime)
    testImplementation(deps.android.test.mockitoCore)
    testImplementation(deps.android.test.kotlinTestJunit)
}

apply(from = "${rootProject.projectDir}/gradle/publish-module.gradle")
