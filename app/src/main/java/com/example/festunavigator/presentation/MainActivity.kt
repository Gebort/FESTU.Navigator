package com.example.festunavigator.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.findNavController
import com.example.festunavigator.presentation.preview.navigation.AppNavigator
import com.gerbort.app.R
import com.gerbort.text_recognition.data.ARCoreSessionLifecycleHelper
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var appNavigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)

        arCoreSessionHelper.beforeSessionResume = { session ->
            session.configure(
                session.config.apply {
                    // To get the best image of the object in question, enable autofocus.
                    focusMode = Config.FocusMode.AUTO
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        depthMode = Config.DepthMode.AUTOMATIC
                    }
                }
            )

            val filter = CameraConfigFilter(session)
                .setFacingDirection(CameraConfig.FacingDirection.BACK)
            val configs = session.getSupportedCameraConfigs(filter)
            val sort = compareByDescending<CameraConfig> { it.imageSize.width }
                .thenByDescending { it.imageSize.height }
            session.cameraConfig = configs.sortedWith(sort)[0]
        }

//        lifecycle.addObserver(arCoreSessionHelper)

    }

    override fun onResume() {
        super.onResume()
        appNavigator.bind(findNavController(R.id.fragmentContainerViewMain))
    }

    override fun onPause() {
        super.onPause()
        appNavigator.unbind()
    }

}