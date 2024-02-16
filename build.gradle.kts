buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath (Libs.Kotlin.gradle)
        classpath (Libs.Gradle.tools)
        classpath (Libs.Firebase.Crashlytics.gradle)
        classpath (Libs.Navigation.SafeArgs.gradle)
        classpath (Libs.Hilt.gradle)
        classpath (Libs.GoogleServices.gradle)
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }

}