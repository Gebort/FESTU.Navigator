buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(Libs.Kotlin.gradle)
        classpath(Libs.Gradle.tools)
        classpath (Libs.Firebase.Crashlytics.gradle)
        classpath(Libs.Navigation.SafeArgs.gradle)
        classpath ("com.google.dagger:hilt-android-gradle-plugin:2.40.5")
        classpath ("com.google.gms:google-services:4.3.13")
    }

}