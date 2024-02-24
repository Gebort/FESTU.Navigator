package plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class LibraryGradlePlugin: Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            applyPlugins()

            dependencies {
                add("implementation", Libs.Hilt.core)
                add("kapt", Libs.Hilt.compiler)

            }
        }

        setProjectConfig(project)
    }

    private fun Project.applyPlugins() {
        project.apply {
            plugin("android-library")
            plugin("kotlin-android")
            plugin("kotlin-kapt")
            plugin("com.google.firebase.crashlytics")
            plugin("dagger.hilt.android.plugin")
        }
    }

    private fun setProjectConfig(project: Project) {
        project.android().apply {
            compileSdk = ProjectConfig.compileSdk

            defaultConfig {
                minSdk = ProjectConfig.minSdk
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    private fun Project.android(): LibraryExtension {
        return extensions.getByType(LibraryExtension::class.java)
    }
}