plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.pathfinding"
}

dependencies {

    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.Smoothing))
    implementation(project(Modules.Core.NodeGraph))

}