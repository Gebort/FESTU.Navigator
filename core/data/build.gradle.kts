plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.data"
}

dependencies {

    implementation(Libs.Sceneview.sceneView)

    implementation(project(Modules.Core.Common))
    implementation(project(Modules.Core.Database))

}