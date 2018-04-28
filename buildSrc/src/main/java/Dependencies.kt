object Versions : Configuration("gradle/versions.properties") {
    val kotlin by string()
    val androidSupport by string()
    val kotlinCoroutines by string()
    val room by string()
    val lifecycle by string()
    val dagger by string()
    val mockitoCore by string()
    val mockitoKotlin by string()
    val junit by string()
    val assertj by string()
}

object Android : Configuration("gradle/android.properties") {
    val defaultConfig = object : Configuration("gradle/android/default_config.properties") {
        val applicationId by string()
        val minSdkVersion by int()
        val targetSdkVersion by int()
        val versionCode by int()
        val versionName by string()
        val testInstrumentationRunner by string()
    }

    @JvmStatic
    val compileSdkVersion by int()
}

object Plugins {
    val androidTools = "com.android.tools.build:gradle:3.2.0-alpha09"
    val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
}

object Deps {
    val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
    val kotlinCoroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
    val kotlinCoroutinesRx2 = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:${Versions.kotlinCoroutines}"

    val rxJava2 = "io.reactivex.rxjava2:rxjava:2.1.10"
    val rxJava2Android = "io.reactivex.rxjava2:rxandroid:2.0.2"

    val appcompat = "com.android.support:appcompat-v7:${Versions.androidSupport}"
    val recyclerView = "com.android.support:recyclerview-v7:${Versions.androidSupport}"
    val supportDesign = "com.android.support:design:${Versions.androidSupport}"

    val constraintLayout = "com.android.support.constraint:constraint-layout:1.0.2"

    val roomRuntime = "android.arch.persistence.room:runtime:1.0.0"
    val roomCompiler = "android.arch.persistence.room:compiler:${Versions.room}"
    val roomRxJava2 = "android.arch.persistence.room:rxjava2:${Versions.room}"

    val lifecycleViewModel = "android.arch.lifecycle:viewmodel:${Versions.lifecycle}"
    val lifecycleExtensions = "android.arch.lifecycle:extensions:${Versions.lifecycle}"

    val dagger = "com.google.dagger:dagger:${Versions.dagger}"
    val daggerCompiler = "com.google.dagger:dagger-compiler:${Versions.dagger}"
}

object TestDeps {
    val junit = "junit:junit:${Versions.junit}"
    val supportTestRunner = "com.android.support.test:runner:1.0.1"
    val espressoCore = "com.android.support.test.espresso:espresso-core:3.0.1"
    val mockitoCore = "org.mockito:mockito-core:${Versions.mockitoCore}"
    val mockitoKotlin = "com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}"
    val assertj = "org.assertj:assertj-core:${Versions.assertj}"
}
