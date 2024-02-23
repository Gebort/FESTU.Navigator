import com.android.build.api.dsl.Packaging

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
    namespace = "com.gerbort.app"
    compileSdk = ProjectConfig.compileSdk

    defaultConfig {
        applicationId = ProjectConfig.appId
        minSdk = ProjectConfig.minSdk
        targetSdk = ProjectConfig.targetSdk
        versionCode = ProjectConfig.versionCode
        versionName = ProjectConfig.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    kapt {
        arguments {
            arg("room.schemaLocation", "$projectDir/schemas".toString())
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
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
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
    androidResources {
        noCompress.add("tflite")
    }
}

dependencies {

    implementation (Libs.AndroidX.core)
    implementation (Libs.AndroidX.appcompat)
    implementation (Libs.Android.material)
    implementation (Libs.AndroidX.ConstraintLayout.core)
    implementation (Libs.AndroidX.legacy)

    implementation (Libs.Firebase.Analytics.core)
    implementation (Libs.Firebase.Crashlytics.core)

    testImplementation (Libs.AndroidX.Test.junit)
    androidTestImplementation (Libs.AndroidX.Test.androidJunit)
    androidTestImplementation (Libs.AndroidX.Test.espresso)

    implementation (Libs.Sceneview.arSceneView)

    implementation (Libs.AndroidX.CoordinatorLayout.core)
    implementation (Libs.AndroidX.Lifecycle.common)

    // ARCore
    implementation (Libs.ARCore.core)

    // Obj - a simple Wavefront OBJ file loader
    // https://github.com/javagl/Obj
    implementation (Libs.Obj.core)

    //bimap for graph links storing
    implementation (Libs.Bimap.core)
    implementation (Libs.Bimap.guavaFix)

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
    implementation (Libs.Coroutines.core)
    implementation (Libs.Coroutines.android)
    implementation (Libs.AndroidX.Lifecycle.viewModel)
    implementation (Libs.Coroutines.playServices)

    //Hilt
    implementation (Libs.Hilt.core)
    kapt (Libs.Hilt.compiler)

    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.HitTest))
    implementation(project(Modules.Core.Pathfinding))
    implementation(project(Modules.Core.TextRecognition))
    implementation(project(Modules.Core.NodeGraph))
    implementation(project(Modules.Core.Data))
    implementation(project(Modules.Core.PathCorrection))

}