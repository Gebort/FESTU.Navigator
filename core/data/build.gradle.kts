plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.data"
}

dependencies {

    implementation(Libs.Sceneview.sceneView)

    project(Modules.Core.Database)

}