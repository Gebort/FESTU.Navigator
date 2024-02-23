plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.text_recognition"

    androidResources {
        noCompress.add("tflite")
    }
}

dependencies {

    implementation(Libs.Coroutines.playServices)
    implementation(Libs.Sceneview.arSceneView)
    implementation(Libs.MLKit.recognition)
}