package com.example.festunavigator.presentation

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.festunavigator.R
import com.example.festunavigator.data.ml.classification.ARCoreSessionLifecycleHelper
import com.example.festunavigator.data.ml.classification.TextAnalyzer
import com.example.festunavigator.data.pathfinding.AStarImpl
import com.example.festunavigator.data.repository.GraphImpl
import com.example.festunavigator.domain.use_cases.*
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.exceptions.*
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

private const val TAG: String = "MainActivity"

class MainActivity : AppCompatActivity() {

    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val analytics = Firebase.analytics

        setContentView(R.layout.activity_main)

        arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)

        arCoreSessionHelper.exceptionCallback = { exception ->
            val message = when (exception) {
                is UnavailableArcoreNotInstalledException,
                is UnavailableUserDeclinedInstallationException -> getString(R.string.install_arcode)
                is UnavailableApkTooOldException -> getString(R.string.update_arcode)
                is UnavailableSdkTooOldException -> getString(R.string.update_app)
                is UnavailableDeviceNotCompatibleException -> getString(R.string.no_arcore_support)
                is CameraNotAvailableException -> getString(R.string.camera_not_available)
                else -> getString(R.string.failed_to_create_session)
            }
            Log.e(TAG, message, exception)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        }

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

        lifecycle.addObserver(arCoreSessionHelper)

    }

}