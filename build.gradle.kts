plugins {
    id(ScriptPlugins.infrastructure)
    id("org.ajoberstar.grgit").version("4.1.1")
    id("io.github.gradle-nexus.publish-plugin").version("1.1.0")
}

buildscript {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven(url = "https://plugins.gradle.org/m2/")
        maven(url = "https://ajoberstar.org/bintray-backup/")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${versions.agp}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${versions.kotlin}")
        classpath("org.jfrog.buildinfo:build-info-extractor-gradle:${versions.jfrogBuildInfoExtractor}")
        classpath("com.android.tools.build.jetifier:jetifier-processor:${versions.jetifierProcessor}")
        classpath("androidx.benchmark:benchmark-gradle-plugin:${versions.benchmarkGradlePlugin}")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${versions.dokkaGradlePlugin}")
        classpath("io.gitlab.arturbosch.detekt" +
            ":detekt-gradle-plugin:${versions.detekt}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }

    configurations.all {
        resolutionStrategy {
            force ("org.xerial:sqlite-jdbc:3.34.0")
        }
    }
}

subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_1_8.toString()
        targetCompatibility = JavaVersion.VERSION_1_8.toString()
    }
}

val clean by tasks.creating(Delete::class) {
    delete(rootProject.buildDir)
    delete("${rootDir}/buildSrc/build")
    delete("${rootDir}/courier/build")
    delete("${rootDir}/courier-core/build")
    delete("${rootDir}/courier-core-android/build")
    delete("${rootDir}/courier-message-adapter-gson/build")
    delete("${rootDir}/courier-message-adapter-moshi/build")
    delete("${rootDir}/courier-message-adapter-protobuf/build")
    delete("${rootDir}/courier-stream-adapter-coroutines/build")
    delete("${rootDir}/courier-stream-adapter-rxjava/build")
    delete("${rootDir}/courier-stream-adapter-rxjava2/build")
    delete("${rootDir}/mqtt-client/build")
    delete("${rootDir}/paho/build")
    delete("${rootDir}/adaptive-keep-alive/build")
    delete("${rootDir}/network-tracker/build")
    delete("${rootDir}/chuck-mqtt/build")
    delete("${rootDir}/chuck-mqtt-no-ops/build")
    delete("${rootDir}/app-state-manager/build")
    delete("${rootDir}/courier-auth-http/build")
    delete("${rootDir}/pingsender/mqtt-pingsender/build")
    delete("${rootDir}/pingsender/workmanager-pingsender/build")
    delete("${rootDir}/pingsender/workmanager-2.6.0-pingsender/build")
    delete("${rootDir}/pingsender/alarm-pingsender/build")
    delete("${rootDir}/pingsender/timer-pingsender/build")
}

apply(from = "${rootDir}/gradle/publish-root.gradle")
