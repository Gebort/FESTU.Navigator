plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.common"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(Libs.Sceneview.sceneView)

}