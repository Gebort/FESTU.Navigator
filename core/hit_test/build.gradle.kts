plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.hit_test"
}

dependencies {

    implementation(Libs.ARCore.core)
    implementation(Libs.Sceneview.arSceneView)

    implementation(project(Modules.Core.Common))

}