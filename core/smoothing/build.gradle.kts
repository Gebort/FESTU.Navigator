plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.smoothing"
}

dependencies {

    implementation(Libs.Sceneview.sceneView)
    implementation(Libs.BezierSpline.core)

    implementation(project(Modules.Core.Common))
}