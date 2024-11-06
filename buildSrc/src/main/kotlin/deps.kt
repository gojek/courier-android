@file:Suppress("unused", "ClassName")

object versions {
    const val jacoco = "0.8.6"
    const val detekt = "1.18.0"

    const val kotlin = "1.6.21"
    const val agp = "7.4.2"
    const val jetifierProcessor = "1.0.0-beta10"
    const val jfrogBuildInfoExtractor = "4.23.4"
    const val navigation = "2.1.0-rc01"
    const val coroutines = "1.3.2"
    const val broadcast = "1.0.0"
    const val lifecycle = "2.4.1"
    const val room = "2.2.5"
    const val retrofit = "2.6.2"
    const val groovy = "2.5.7"
    const val benchmarkGradlePlugin = "1.0.0"
    const val dokkaGradlePlugin = "1.5.0"
    const val nodeGradlePlugin = "2.2.0"
    const val bintrayGradlePlugin = "1.8.4"
    const val timber = "4.7.1"
    const val okio = "1.13.0"
    const val moshi = "1.11.0"
    const val appCompact = "1.2.0"
    const val activityVersion = "1.1.0"
    const val constraintLayoutVersion = "2.0.4"
    const val supportV4Version = "1.0.0"
    const val recyclerViewVersion = "1.1.0"
    const val materialVersion = "1.3.0"
    const val annotationVersion = "1.2.0"
    const val coreKtxVersion = "1.3.0"
    const val apiValidator = "0.14.0"
    const val workManager = "2.7.0"
}

object ScriptPlugins {
    const val infrastructure = "scripts.infrastructure"
    const val apiValidator = "org.jetbrains.kotlinx.binary-compatibility-validator"
}

object deps {
    object android {
        const val gson = "com.google.code.gson:gson:2.8.6"
        const val protobuf = "com.google.protobuf:protobuf-lite:3.0.0"

        object build {
            const val buildToolsVersion = "33.0.1"
            const val compileSdkVersion = 34
            const val minSdkVersion = 21
            const val sampleMinSdkVersion = 21
            const val targetSdkVersion = 34
        }

        object test {
            const val core = "androidx.test:core:1.2.0"
            const val coreTesting = "androidx.arch.core:core-testing:2.1.0"
            const val junit = "junit:junit:4.12"
            const val runner = "androidx.test:runner:1.2.0"
            const val roboelectric = "org.robolectric:robolectric:4.2"
            const val mockito = "com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0"
            const val mockitoCore = "org.mockito:mockito-core:4.4.0"
            const val junitExt = "androidx.test.ext:junit:1.1.1"
            const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit:${versions.kotlin}"
        }

        object broadcast {
            const val localbroadcast =
                "androidx.localbroadcastmanager:localbroadcastmanager:${versions.broadcast}"
        }

        object room {
            const val roomRuntime = "androidx.room:room-runtime:${versions.room}"
            const val roomRxJava = "androidx.room:room-rxjava2:${versions.room}"
            const val roomCompiler = "androidx.room:room-compiler:${versions.room}"
        }

        object androidx {
            const val appcompact = "androidx.appcompat:appcompat:${versions.appCompact}"
            const val fragmentExtensions = "androidx.fragment:fragment-ktx:${versions.activityVersion}"
            const val constraintLayout = "androidx.constraintlayout:constraintlayout:${versions.constraintLayoutVersion}"
            const val coreKtx = "androidx.core:core-ktx:${versions.coreKtxVersion}"
            const val supportV4 = "androidx.legacy:legacy-support-v4:${versions.supportV4Version}"
            const val recyclerView = "androidx.recyclerview:recyclerview:${versions.recyclerViewVersion}"
            const val material = "com.google.android.material:material:${versions.materialVersion}"
            const val annotation = "androidx.annotation:annotation:${versions.annotationVersion}"
            const val lifecycleExtensions = "androidx.lifecycle:lifecycle-runtime-ktx:${versions.lifecycle}"
            const val lifecycleCommons = "androidx.lifecycle:lifecycle-common-java8:${versions.lifecycle}"
            const val lifecycleProcess = "androidx.lifecycle:lifecycle-process:${versions.lifecycle}"
        }
    }

    object kotlin {
        object stdlib {
            const val core = "org.jetbrains.kotlin:kotlin-stdlib:${versions.kotlin}"
            const val jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin}"
        }

        object coroutines {
            const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.coroutines}"
            const val reactive =
                "org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${versions.coroutines}"
            const val android =
                "org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.coroutines}"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${versions.coroutines}"
        }
    }

    object logger {
        const val timber = "com.jakewharton.timber:timber:${versions.timber}"
    }

    object rx {
        const val reactiveStreams = "org.reactivestreams:reactive-streams:1.0.2"
        const val java1 = "io.reactivex:rxjava:1.3.4"
        const val java = "io.reactivex.rxjava2:rxjava:2.2.15"
        const val android = "io.reactivex.rxjava2:rxandroid:2.1.1"
        const val rxKotlin = "io.reactivex.rxjava2:rxkotlin:2.4.0"
        const val rx3BindingCore = "com.jakewharton.rxbinding3:rxbinding-core:3.0.0"
    }

    object detekt {
        const val lint = "io.gitlab.arturbosch.detekt:detekt-formatting:${versions.detekt}"
        const val cli = "io.gitlab.arturbosch.detekt:detekt-cli:${versions.detekt}"
    }

    object square {
        const val okio = "com.squareup.okio:okio:${versions.okio}"
        const val moshi = "com.squareup.moshi:moshi-kotlin:${versions.moshi}"
        const val retrofit = "com.squareup.retrofit2:retrofit:${versions.retrofit}"
    }

    object workManager {
        const val runtime = "androidx.work:work-runtime:${versions.workManager}"
        const val runtime_2_6_0 = "androidx.work:work-runtime:2.6.0"
    }
}
