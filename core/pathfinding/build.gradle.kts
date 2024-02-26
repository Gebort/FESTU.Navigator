plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.pathfinding"
}

dependencies {

    //излишняя библиотека для простого доступа к Float3
    implementation(Libs.Sceneview.sceneView)
    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.Smoothing))
    implementation(project(Modules.Core.Data))
    implementation(project(Modules.Core.NodeGraph))

}