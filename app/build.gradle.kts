plugins {
    id("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-kapt")
    id ("com.google.gms.google-services")
    id ("com.google.firebase.crashlytics")
    id("androidx.navigation.safeargs")
    id ("dagger.hilt.android.plugin")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "com.example.festunavigator"
        minSdk = 24
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas".toString())
        }
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions.add("mode")
    productFlavors {
        create("user") {
            dimension = "mode"
            applicationIdSuffix = ".user"
            versionNameSuffix = "-user"
        }
        create("admin") {
            dimension = "mode"
            applicationIdSuffix = ".admin"
            versionNameSuffix = "-admin"
        }

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    androidResources {
        noCompress.add("tflite")
    }
}

dependencies {

    implementation ("androidx.core:core-ktx:1.8.0")
    implementation ("androidx.appcompat:appcompat:1.5.0")
    implementation ("com.google.android.material:material:1.6.1")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.legacy:legacy-support-v4:1.0.0")
    implementation ("com.google.firebase:firebase-analytics-ktx:21.1.0")
    implementation ("com.google.firebase:firebase-crashlytics-ktx:18.2.12")
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")

    implementation ("io.github.sceneview:arsceneview:${Libs.Sceneview.version}")

    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("androidx.lifecycle:lifecycle-common-java8:2.5.1")

    // ARCore
    implementation ("com.google.ar:core:1.32.0")

    // Obj - a simple Wavefront OBJ file loader
    // https://github.com/javagl/Obj
    implementation ("de.javagl:obj:0.2.1")

    //bimap for graph links storing
    implementation ("com.uchuhimo:kotlinx-bimap:1.2")
    implementation ("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

    // Room
    implementation ("androidx.room:room-runtime:2.4.3")
    kapt ("androidx.room:room-compiler:2.4.3")

    // Kotlin Extensions and Coroutines support for Room
    implementation ("androidx.room:room-ktx:2.4.3")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0-alpha01")

    //Dialog with editText
    implementation ("com.afollestad.material-dialogs:core:3.3.0")
    implementation ("com.afollestad.material-dialogs:input:3.3.0")

    //ML Kit
    implementation ("com.google.android.gms:play-services-mlkit-text-recognition:18.0.1")

    //bezier curve for smooth path building
    implementation ("dev.benedikt.math:bezier-spline:1.2.0")

    //Splashscreen
    implementation ("androidx.core:core-splashscreen:1.0.0")

    // Navigation kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:${Libs.Navigation.version}")
    implementation("androidx.navigation:navigation-ui-ktx:${Libs.Navigation.version}")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.0-alpha01")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.1")

    //Hilt
    implementation (Libs.Hilt.core)
    kapt (Libs.Hilt.compiler)



}