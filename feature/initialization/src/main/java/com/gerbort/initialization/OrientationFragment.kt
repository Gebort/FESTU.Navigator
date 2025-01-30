package com.gerbort.initialization

import android.animation.ObjectAnimator
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gerbort.core_ui.frame_holder.FrameProducer
import com.gerbort.initialization.databinding.FragmentOrientationBinding
import com.gerbort.initialization.navigation.OrientationNavigator
import com.google.ar.core.Plane
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OrientationFragment : Fragment() {

    private var _binding: FragmentOrientationBinding? = null
    private val binding get() = _binding!!

    @Inject lateinit var frameProducer: FrameProducer
    @Inject lateinit var navigator: OrientationNavigator

    private var phoneIconAnimation: ObjectAnimator? = null

    private var navigating = false
    private var scanningWall = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrientationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                frameProducer.getFrames().collect { frame ->
                    frame?.let {
                        if (frame.session.allPlanes.any { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }) {
                            if (!scanningWall) {
                                scanningWall = true
                                binding.textView2.setText(R.string.orientate_wall)
                            }
                        }
                        if (frame.session.allPlanes.any { it.type == Plane.Type.VERTICAL }) {
                            if (!navigating){
                                navigating = true
                                navigator.navigateOrientationToScanner()
                            }
                        }
                    }

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.imageView.isVisible = true
        binding.imageView.doOnLayout {
            val x = it.x
            val y = it.y
            val animationPath = Path().apply {
                setLastPoint(x, y)
                lineTo(x + x/2, y)
                lineTo(x, y - y/4, )
                lineTo(x - x/2, y)
                lineTo(x, y)
            }
            val animationDuration = 3000L

            phoneIconAnimation = ObjectAnimator.ofFloat(it, View.X, View.Y, animationPath).apply {
                duration = animationDuration
                repeatCount = Animation.INFINITE
                start()
            }
        }
    }

    override fun onPause() {
        binding.imageView.isVisible = false
        phoneIconAnimation?.pause()
        super.onPause()
    }
}