plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.sensor_handling"
}

dependencies {
    implementation(project(Modules.Core.CoreUi))
}