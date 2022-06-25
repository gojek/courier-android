package plugin

import com.android.build.gradle.BaseExtension
import deps
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

internal fun Project.configureAndroid() {
    this.extensions.getByType<BaseExtension>().run {
        compileSdkVersion(deps.android.build.compileSdkVersion)
        buildToolsVersion(deps.android.build.buildToolsVersion)
        defaultConfig {
            minSdkVersion(deps.android.build.minSdkVersion)
            targetSdkVersion(deps.android.build.targetSdkVersion)
            consumerProguardFiles("$rootDir/proguard/proguard-rules.pro")
        }
        sourceSets {
            getByName("main").java.srcDir("src/main/kotlin")
            getByName("test").java.srcDir("src/test/kotlin")
            getByName("androidTest").java.srcDir("src/androidTest/kotlin")
        }

        buildTypes.getByName("debug") {
            isTestCoverageEnabled = true
            isDebuggable = true
        }

        buildTypes.getByName("release") {
            isTestCoverageEnabled = false
            isDebuggable = false
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        lintOptions {
            isAbortOnError = false
        }

        testOptions {
            animationsDisabled = true
        }
    }
}