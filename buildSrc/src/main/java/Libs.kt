object Libs {

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
    }

    object Sceneview {
        const val version = "0.7.0"
    }

    object Navigation {
        const val version = "2.4.2"

        object SafeArgs {
            const val gradle = "androidx.navigation:navigation-safe-args-gradle-plugin:$version"
        }
    }

    object Firebase {
        object Crashlytics {
            const val version = "2.9.1"
            const val gradle = "com.google.firebase:firebase-crashlytics-gradle:$version"
        }
    }

}