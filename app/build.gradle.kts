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

    implementation (Libs.Firebase.Analytics.core)
    implementation (Libs.Firebase.Crashlytics.core)

    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")

    implementation ("io.github.sceneview:arsceneview:${Libs.Sceneview.version}")

    implementation ("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation ("androidx.lifecycle:lifecycle-common-java8:2.5.1")

    // ARCore
    implementation (Libs.ARCore.core)

    // Obj - a simple Wavefront OBJ file loader
    // https://github.com/javagl/Obj
    implementation ("de.javagl:obj:0.2.1")

    //bimap for graph links storing
    implementation ("com.uchuhimo:kotlinx-bimap:1.2")
    implementation ("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")

    // Room
    implementation (Libs.Room.runtime)
    kapt (Libs.Room.compiler)
    implementation (Libs.Room.androidx)
    implementation (Libs.AndroidX.Lifecycle.runtime)

    //Dialog with editText
    implementation (Libs.Dialogs.core)
    implementation (Libs.Dialogs.input)

    //ML Kit
    implementation (Libs.MLKit.recognition)

    //bezier curve for smooth path building
    implementation (Libs.BezierSpline.core)

    //Splashscreen
    implementation (Libs.AndroidX.Splashscreen.core)

    // Navigation kotlin
    implementation(Libs.Navigation.fragments)
    implementation(Libs.Navigation.ui)

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