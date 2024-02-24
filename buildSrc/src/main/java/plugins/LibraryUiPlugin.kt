package plugins

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class LibraryUiPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        setProjectConfig(project)
        project.apply {
            plugin("androidx.navigation.safeargs")
        }
        with(project) {
            dependencies {
                add("implementation", Libs.Android.material)
                add("implementation", Libs.AndroidX.ConstraintLayout.core)
                add("implementation", Libs.AndroidX.legacy)
                add("implementation", Libs.AndroidX.CoordinatorLayout.core)
                add("implementation", Libs.AndroidX.Lifecycle.common)
                add("implementation", Libs.AndroidX.Lifecycle.runtime)
                add("implementation", Libs.AndroidX.Lifecycle.viewModel)
                add("implementation", Libs.Navigation.ui)
                add("implementation", Libs.Navigation.fragments)
            }
        }
    }

    private fun setProjectConfig(project: Project) {
        project.android().apply {
            buildFeatures {
                viewBinding = true
                //compose = true
            }
//            composeOptions {
//                kotlinCompilerExtensionVersion = "1.5.8"
//            }
        }
    }

    private fun Project.android(): LibraryExtension {
        return extensions.getByType(LibraryExtension::class.java)
    }
}