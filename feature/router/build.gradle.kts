plugins {
    id("project.android.library")
    id("project.android.library.ui")
}

android {
    namespace = "com.gerbort.router"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(Libs.Sceneview.arSceneView)

    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.NodeGraph))
    implementation(project(Modules.Core.Pathfinding))
    implementation(project(Modules.Core.HitTest))
    implementation(project(Modules.Core.CoreUi))

}