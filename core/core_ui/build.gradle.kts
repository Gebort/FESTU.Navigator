plugins {
    id("project.android.library")
    id("project.android.library.ui")
}

android {
    namespace = "com.gerbort.core_ui"
}

dependencies {

    implementation(Libs.Sceneview.arSceneView)

    implementation(project(Modules.Core.Common))

}