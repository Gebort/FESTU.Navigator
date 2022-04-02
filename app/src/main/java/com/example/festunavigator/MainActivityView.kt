package com.example.festunavigator

import android.opengl.GLSurfaceView
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.festunavigator.common.helpers.SnackbarHelper
import com.example.festunavigator.common.samplerender.SampleRender
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode

/**
 * Wraps [R.layout.activity_main] and controls lifecycle operations for [GLSurfaceView].
 */
class MainActivityView(val activity: MainActivity, renderer: AppRenderer) : DefaultLifecycleObserver {
    val root = View.inflate(activity, R.layout.activity_main, null)
    val surfaceView = root.findViewById<ArSceneView>(R.id.sceneView).apply {
        planeRenderer.isVisible = true
        instructions.enabled = false


    }
    val place = root.findViewById<Button>(R.id.place_button)
    val x_edit = root.findViewById<EditText>(R.id.x_edit)
    val y_edit = root.findViewById<EditText>(R.id.y_edit)
    val z_edit = root.findViewById<EditText>(R.id.z_edit)


    //val useCloudMlSwitch = root.findViewById<SwitchCompat>(R.id.useCloudMlSwitch)
    //val scanButton = root.findViewById<AppCompatButton>(R.id.scanButton)
    //val resetButton = root.findViewById<AppCompatButton>(R.id.clearButton)
    val snackbarHelper = SnackbarHelper().apply {
        setParentView(root.findViewById(R.id.con_layout))
        setMaxLines(6)
    }

    override fun onResume(owner: LifecycleOwner) {
        surfaceView.onResume(owner)
    }

    override fun onPause(owner: LifecycleOwner) {
        surfaceView.onPause(owner)
    }

    fun post(action: Runnable) = root.post(action)

    /**
     * Toggles the scan button depending on if scanning is in progress.
     */
//    fun setScanningActive(active: Boolean) = when(active) {
//        true -> {
//            scanButton.isEnabled = false
//            scanButton.setText(activity.getString(R.string.scan_busy))
//        }
//        false -> {
//            scanButton.isEnabled = true
//            scanButton.setText(activity.getString(R.string.scan_available))
//        }
//    }
}