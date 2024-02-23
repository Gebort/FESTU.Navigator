plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.path_correction"
}

dependencies {

    implementation(Libs.Sceneview.sceneView)

    implementation(project(Modules.Core.Common))

}