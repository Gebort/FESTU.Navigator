package plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class LibraryGMSPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            applyPlugins()
        }

        setProjectConfig(project)
    }

    private fun Project.applyPlugins() {
        project.apply {
            plugin("com.google.gms.google-services")
        }
    }

    private fun setProjectConfig(project: Project) {
        project.android().apply {
//            compileSdk = ProjectConfig.compileSdk
//
//            defaultConfig {
//                minSdk = ProjectConfig.minSdk
//                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//            }
//
//            compileOptions {
//                sourceCompatibility = JavaVersion.VERSION_1_8
//                targetCompatibility = JavaVersion.VERSION_1_8
//            }
        }
    }

    private fun Project.android(): LibraryExtension {
        return extensions.getByType(LibraryExtension::class.java)
    }
}