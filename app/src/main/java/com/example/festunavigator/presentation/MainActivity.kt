package com.example.festunavigator.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.festunavigator.data.ml.classification.ARCoreSessionLifecycleHelper
import com.example.festunavigator.data.pathfinding.AStarImpl
import com.example.festunavigator.data.repository.GraphImpl
import com.example.festunavigator.domain.use_cases.*
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.exceptions.*

class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"
    lateinit var arCoreSessionHelper: ARCoreSessionLifecycleHelper

    lateinit var renderer: AppRenderer
    lateinit var view: MainActivityView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arCoreSessionHelper = ARCoreSessionLifecycleHelper(this)

        arCoreSessionHelper.exceptionCallback = { exception ->
            val message = when (exception) {
                is UnavailableArcoreNotInstalledException,
                is UnavailableUserDeclinedInstallationException -> "Please install ARCore"
                is UnavailableApkTooOldException -> "Please update ARCore"
                is UnavailableSdkTooOldException -> "Please update this app"
                is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
                is CameraNotAvailableException -> "Camera not available. Try restarting the app."
                else -> "Failed to create AR session: $exception"
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

        val repository = GraphImpl()
        val insertNodes = InsertNodes(repository)
        val deleteNodes = DeleteNodes(repository)
        val updateNodes = UpdateNodes(repository)
        val pathfinder = AStarImpl()
        val findWay = FindWay(pathfinder)
        val getTree = GetTree(repository)


        renderer = com.example.festunavigator.presentation.AppRenderer(
            this,
            deleteNodes,
            getTree,
            insertNodes,
            updateNodes,
            findWay
        )
        lifecycle.addObserver(renderer)
        view = MainActivityView(this, renderer)
        setContentView(view.root)
        renderer.bindView(view)
        lifecycle.addObserver(view)

        //setContentView(R.layout.activity_main)

    }

}