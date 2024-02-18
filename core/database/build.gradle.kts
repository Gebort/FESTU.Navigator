plugins {
    id("project.android.library")
}

android {
    namespace = "com.gerbort.database"

    defaultConfig {
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }

}

dependencies {

    implementation(Libs.Room.runtime)
    kapt(Libs.Room.compiler)
}