plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.node_graph"
}

dependencies {

    implementation(Libs.Sceneview.sceneView)

    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.Data))

}