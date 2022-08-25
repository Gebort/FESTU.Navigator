package com.example.festunavigator.presentation.scanner

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.festunavigator.data.App
import com.example.festunavigator.databinding.FragmentScannerBinding
import com.example.festunavigator.domain.hit_test.HitTestResult
import com.example.festunavigator.domain.ml.DetectedText
import com.example.festunavigator.domain.use_cases.AnalyzeImage
import com.example.festunavigator.domain.use_cases.HitTest
import com.example.festunavigator.presentation.LabelObject
import com.example.festunavigator.presentation.common.helpers.DisplayRotationHelper
import com.example.festunavigator.presentation.confirmer.ConfirmFragment
import com.example.festunavigator.presentation.preview.MainEvent
import com.example.festunavigator.presentation.preview.MainShareModel
import com.example.festunavigator.presentation.preview.PreviewFragment
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.scene.destroy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val SMOOTH_DELAY = 0.5

@AndroidEntryPoint
class ScannerFragment: Fragment() {

    private val mainModel: MainShareModel by activityViewModels()

    @Inject
    lateinit var hitTest: HitTest
    @Inject
    lateinit var analyzeImage: AnalyzeImage

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val args: ScannerFragmentArgs by navArgs()
    private val scanType by lazy { args.scanType }

    private lateinit var displayRotationHelper: DisplayRotationHelper
    private var lastDetectedObject: DetectedText? = null
    private var scanningNow: Boolean = false
    private var currentScanSmoothDelay: Double = 0.0
    private var scanningJob: Job? = null
    private var lastFrameTime = System.currentTimeMillis()

    private var navigating = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        displayRotationHelper = DisplayRotationHelper(context)
    }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            _binding = FragmentScannerBinding.inflate(inflater, container, false)
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    mainModel.frame.collect { frame ->
                        frame?.let { onFrame(it) }

                    }
                }
            }

        }

        override fun onResume() {

            lastDetectedObject = null
            scanningNow = false
            currentScanSmoothDelay = 0.0
            scanningJob?.cancel()
            scanningJob = null
            navigating = false

            super.onResume()
            displayRotationHelper.onResume()
        }

        override fun onPause() {
            super.onPause()
            displayRotationHelper.onPause()
        }

        private fun onFrame(frame: ArFrame) {

            if (currentScanSmoothDelay > 0) {
                currentScanSmoothDelay -= getFrameInterval()
            }

            if (!scanningNow) {
                if (scanningJob?.isActive != true) {
                    scanningJob?.cancel()
                    scanningJob =
                        viewLifecycleOwner.lifecycleScope.launch {
                                if (currentScanSmoothDelay <= 0 && lastDetectedObject != null) {

                                    val res = hitTestDetectedObject(lastDetectedObject!!, frame)
                                    if (res != null && !navigating) {
                                        val confirmationObject = LabelObject(
                                            label = lastDetectedObject!!.detectedObjectResult.label,
                                            pos = res.orientatedPosition,
                                            anchor = res.hitResult.createAnchor()
                                        )

                                        mainModel.onEvent(
                                            MainEvent.NewConfirmationObject(
                                                confirmationObject
                                            )
                                        )
                                        toConfirm(
                                            when (scanType) {
                                                TYPE_INITIALIZE -> {
                                                    ConfirmFragment.CONFIRM_INITIALIZE
                                                }
                                                TYPE_ENTRY -> {
                                                    ConfirmFragment.CONFIRM_ENTRY
                                                }
                                                else -> {
                                                    throw IllegalArgumentException("Unrealised type")
                                                }
                                            }
                                        )
                                    } else {
                                        currentScanSmoothDelay = SMOOTH_DELAY
                                    }

                                } else {
                                    scanningNow = true
                                    val detectedObject = tryGetDetectedObject(frame)
                                    if (lastDetectedObject == null) {
                                        lastDetectedObject = detectedObject
                                        currentScanSmoothDelay = SMOOTH_DELAY
                                    } else if (detectedObject == null) {
                                        currentScanSmoothDelay = SMOOTH_DELAY
                                    } else {
                                        if (lastDetectedObject!!.detectedObjectResult.label !=
                                            detectedObject.detectedObjectResult.label
                                        ) {
                                            currentScanSmoothDelay = SMOOTH_DELAY
                                        }
                                        lastDetectedObject = detectedObject
                                    }
                                    scanningNow = false
                                }
                            }
                }
            }
        }

        private fun toConfirm(type: Int){
            if (!navigating){
                navigating = true
                val action = ScannerFragmentDirections.actionScannerFragmentToConfirmFragment(type)
                findNavController().navigate(action)
            }

        }

        private suspend fun tryGetDetectedObject(frame: ArFrame): DetectedText? {
            val camera = frame.camera
            val session = frame.session

            if (camera.trackingState != TrackingState.TRACKING) {
                return null
            }
            val cameraImage = frame.tryAcquireCameraImage()
            if (cameraImage != null) {
                val cameraId = session.cameraConfig.cameraId
                val imageRotation =
                    displayRotationHelper.getCameraSensorToDisplayRotation(cameraId)
                val displaySize = Pair(
                    session.displayWidth,
                    session.displayHeight
                )
                val detectedResult = analyzeImage(
                    cameraImage,
                    imageRotation,
                    PreviewFragment.DESIRED_CROP,
                    displaySize
                )

                cameraImage.close()

                detectedResult.getOrNull()?.let {
                    return DetectedText(it, frame.frame)
                }
                return null
            }
            cameraImage?.close()
            return null

        }

        private fun hitTestDetectedObject(detectedText: DetectedText, frame: ArFrame): HitTestResult? {

            val detectedObject = detectedText.detectedObjectResult
            return useHitTest(
                detectedObject.centerCoordinate.x,
                detectedObject.centerCoordinate.y,
                frame
            )
                .getOrNull()
        }

        private fun useHitTest(
            x: Float,
            y: Float,
            frame: ArFrame
        ): Result<HitTestResult> {

            return hitTest(frame, Float2(x, y))
        }

        private fun getFrameInterval(): Long {
            val frameTime = System.currentTimeMillis() - lastFrameTime
            lastFrameTime = System.currentTimeMillis()
            return frameTime
        }

        private fun ArFrame.tryAcquireCameraImage() = try {
            frame.acquireCameraImage()
        } catch (e: NotYetAvailableException) {
            null
        } catch (e: Throwable) {
            throw e
        }

        companion object {
            const val SCAN_TYPE = "scanType"
            const val TYPE_INITIALIZE = 0
            const val TYPE_ENTRY = 1
    }
    }