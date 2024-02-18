object Libs {

    object Android {
        const val material = "com.google.android.material:material:1.6.1"
    }
    object AndroidX {
        const val core = "androidx.core:core-ktx:1.8.0"
        const val appcompat = "androidx.appcompat:appcompat:1.5.0"
        const val material = "com.google.android.material:material:1.6.1"
        const val legacy = "androidx.legacy:legacy-support-v4:1.0.0"
        object Lifecycle {
            const val version = "2.6.0-alpha01"
            const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val common = "androidx.lifecycle:lifecycle-common-java8:$version"
        }
        object Splashscreen {
            const val version = "1.0.0"
            const val core = "androidx.core:core-splashscreen:$version"
        }
        object CoordinatorLayout {
            const val version = "1.2.0"
            const val core = "androidx.coordinatorlayout:coordinatorlayout:$version"
        }
        object ConstraintLayout {
            const val core = "androidx.constraintlayout:constraintlayout:2.1.4"
        }
        object Test {
            const val junit = "junit:junit:4.13.2"
            const val androidJunit = "androidx.test.ext:junit:1.1.3"
            const val espresso = "androidx.test.espresso:espresso-core:3.4.0"
        }
    }
    object Gradle {
        const val version = "8.2.1"
        const val tools = "com.android.tools.build:gradle:$version"
    }
    object Kotlin {
        const val version = "1.9.22"
        const val gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    }

    object Coroutines {
        const val version = "1.6.1"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        const val playServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$version"
    }
    object Hilt {
        const val version = "2.48"
        const val core = "com.google.dagger:hilt-android:$version"
        const val compiler = "com.google.dagger:hilt-compiler:$version"
        const val gradle = "com.google.dagger:hilt-android-gradle-plugin:$version"
    }
    object GoogleServices {
        const val version = "4.3.13"
        const val gradle = "com.google.gms:google-services:$version"
    }

    object MLKit {
        const val version = "18.0.1"
        const val recognition = "com.google.android.gms:play-services-mlkit-text-recognition:$version"
    }

    object Sceneview {
        const val version = "0.7.0"
        const val sceneView = "io.github.sceneview:sceneview:${version}"
        const val arSceneView = "io.github.sceneview:arsceneview:${version}"
    }

    object Navigation {
        const val version = "2.5.0"
        const val fragments = "androidx.navigation:navigation-fragment-ktx:${version}"
        const val ui = "androidx.navigation:navigation-ui-ktx:${version}"

        object SafeArgs {
            const val gradle = "androidx.navigation:navigation-safe-args-gradle-plugin:$version"
        }
    }

    object Firebase {
        object Crashlytics {
            const val version = "18.2.12"
            const val core = "com.google.firebase:firebase-crashlytics-ktx:$version"
            const val gradleVersion = "2.9.1"
            const val gradle = "com.google.firebase:firebase-crashlytics-gradle:$gradleVersion"
        }
        object Analytics {
            const val version = "21.1.0"
            const val core = "com.google.firebase:firebase-analytics-ktx:$version"
        }
    }

    object ARCore {
        const val version = "1.32.0"
        const val core = "com.google.ar:core:$version"
    }

    object Room {
        const val version = "2.4.3"
        const val runtime = "androidx.room:room-runtime:$version"
        const val compiler = "androidx.room:room-compiler:$version"
        const val androidx = "androidx.room:room-ktx:$version"
    }

    object Dialogs {
        const val version = "3.3.0"
        const val core = "com.afollestad.material-dialogs:core:$version"
        const val input = "com.afollestad.material-dialogs:input:$version"
    }

    object BezierSpline {
        const val version = "1.2.0"
        const val core = "dev.benedikt.math:bezier-spline:1.2.0"
    }

    object Bimap {
        const val version = "1.2"
        const val core = "com.uchuhimo:kotlinx-bimap:$version"
        const val guavaFix = "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava"
    }

    object Obj {
        const val version = "0.2.1"
        const val core = "de.javagl:obj:$version"
    }

}