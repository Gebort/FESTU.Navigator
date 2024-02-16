plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation ("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation ("com.android.tools.build:gradle:8.2.1")
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.48")
}

gradlePlugin {
    plugins {

        register("androidLibrary") {
            id = "project.android.library"
            implementationClass = "plugins.LibraryGradlePlugin"
        }

        register("composeLibrary") {
            id = "project.android.library.ui"
            implementationClass = "plugins.LibraryUiPlugin"
        }

    }
}