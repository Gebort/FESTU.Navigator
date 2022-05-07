package com.example.festunavigator.presentation

import android.opengl.GLSurfaceView
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.festunavigator.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.sceneview.ar.ArSceneView

/**
 * Wraps [R.layout.activity_main] and controls lifecycle operations for [GLSurfaceView].
 */
class MainActivityView(val activity: MainActivity, renderer: AppRenderer) : DefaultLifecycleObserver {
    val root = View.inflate(activity, R.layout.activity_main, null)
    val surfaceView = root.findViewById<ArSceneView>(R.id.sceneView).apply {
        planeRenderer.isVisible = false
        instructions.enabled = false

    }
    val place: Button = root.findViewById(R.id.place_button)
    val delete: Button = root.findViewById(R.id.delete_button)
    val link: Button = root.findViewById(R.id.link_button)
    val entry: Button = root.findViewById(R.id.entry_button)
    val scanText: TextView = root.findViewById(R.id.scan_text)
    val scanImage: ImageView = root.findViewById(R.id.borders_image)
    val confirmLayout: FrameLayout = root.findViewById(R.id.confirm_layout)
    val routeLayout: FrameLayout = root.findViewById(R.id.route_layout)
    val routeBigLayout: FrameLayout = root.findViewById(R.id.route_big_layout)
    val acceptButton: ImageButton = root.findViewById(R.id.accept_button)
    val rejectButton: ImageButton = root.findViewById(R.id.reject_button)
    val fromInput: TextInputEditText = root.findViewById(R.id.from_input)
    val toInput: TextInputEditText = root.findViewById(R.id.to_input)
    val searchInput: TextInputEditText = root.findViewById(R.id.search_input)
    val searchLayout: TextInputLayout = root.findViewById(R.id.search_layout)
    val entryRecyclerView: RecyclerView = root.findViewById(R.id.entry_recyclerView)
    val destinationText: TextView = root.findViewById(R.id.destination_text)
    val adminPanel: CardView = root.findViewById(R.id.admin_panel)


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