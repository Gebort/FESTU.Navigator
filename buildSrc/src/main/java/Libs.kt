object Libs {

    object AndroidX {
        object Lifecycle {
            const val version = "2.6.0-alpha01"
            const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
        }
        object Splashscreen {
            const val version = "1.0.0"
            const val core = "androidx.core:core-splashscreen:$version"
        }
    }
    object Gradle {
        const val version = "7.1.3"
        const val tools = "com.android.tools.build:gradle:$version"
    }
    object Kotlin {
        const val version = "1.6.21"
        const val gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
    }
    object Hilt {
        const val version = "2.40.5"
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
    }

    object Navigation {
        const val version = "2.4.2"
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

}