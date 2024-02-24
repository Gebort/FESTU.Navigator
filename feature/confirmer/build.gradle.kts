plugins {
    id("project.android.library")
    id("project.android.library.ui")
}

android {
    namespace = "com.gerbort.confirmer"
}

dependencies {

    implementation(Libs.Sceneview.arSceneView)

    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.CoreUi))
}