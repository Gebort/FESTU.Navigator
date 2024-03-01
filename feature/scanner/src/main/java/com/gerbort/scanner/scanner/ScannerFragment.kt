package com.gerbort.scanner.scanner

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.gerbort.core_ui.frame_holder.FrameProducer
import com.gerbort.hit_test.HitTestUseCase
import com.gerbort.scanner.ConfirmType
import com.gerbort.scanner.LabelObject
import com.gerbort.scanner.ScannerEvent
import com.gerbort.scanner.ScannerViewModel
import com.gerbort.scanner.helpers.DisplayRotationHelper
import com.gerbort.scanner.databinding.FragmentScannerBinding
import com.gerbort.text_recognition.domain.DetectTextUseCase
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.NotYetAvailableException
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.ar.arcore.ArFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScannerFragment: Fragment() {

    @Inject
    lateinit var hitTest: HitTestUseCase
    @Inject
    lateinit var analyzeImage: DetectTextUseCase
    @Inject
    lateinit var frameProducer: FrameProducer

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private val vm: ScannerViewModel by activityViewModels()

    private val args: ScannerFragmentArgs by navArgs()
    private val scanType by lazy { args.scanType }

    private lateinit var displayRotationHelper: DisplayRotationHelper
    private var lastDetectedObject: com.gerbort.text_recognition.domain.DetectedText? = null
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
            _binding = FragmentScannerBinding.inflate(
                inflater,
                container,
                false
            )
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

            vm.onEvent(ScannerEvent.NewScanType(
                when (scanType) {
                    TYPE_INITIALIZE -> ConfirmType.INITIALIZE
                    TYPE_ENTRY -> ConfirmType.ENTRY
                    else -> throw IllegalArgumentException("Unknown confirm type $scanType")
                }
            ))
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    frameProducer.getFrames().collect { frame ->
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
                                        val confirmationObject =
                                            LabelObject(
                                                label = lastDetectedObject!!.detectedObjectResult.label,
                                                pos = res.orientatedPosition,
                                            )

                                        vm.onEvent(
                                            ScannerEvent.NewConfirmationObject(confirmationObject)
                                        )
                                        toConfirm()
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

        private fun toConfirm(){
            if (!navigating){
                navigating = true
                val action = ScannerFragmentDirections.actionScannerFragmentToConfirmFragment()
                findNavController().navigate(action)
            }
        }

        private suspend fun tryGetDetectedObject(frame: ArFrame): com.gerbort.text_recognition.domain.DetectedText? {
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
                    image = cameraImage,
                    imageRotation = imageRotation,
                    displaySize = displaySize
                )

                cameraImage.close()

                detectedResult.getOrNull()?.let {
                    return com.gerbort.text_recognition.domain.DetectedText(it, frame.frame)
                }
                return null
            }
            cameraImage?.close()
            return null

        }

        private fun hitTestDetectedObject(detectedText: com.gerbort.text_recognition.domain.DetectedText, frame: ArFrame): com.gerbort.hit_test.HitTestResult? {

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
        ): Result<com.gerbort.hit_test.HitTestResult> {

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
            const val SMOOTH_DELAY = 0.5
    }
    }