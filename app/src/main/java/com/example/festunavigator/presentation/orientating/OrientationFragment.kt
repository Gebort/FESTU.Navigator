package com.example.festunavigator.presentation.orientating

import android.animation.ObjectAnimator
import android.graphics.Path
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.core.view.doOnLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.festunavigator.R
import com.example.festunavigator.databinding.FragmentOrientationBinding
import com.example.festunavigator.presentation.preview.MainShareModel
import com.example.festunavigator.presentation.scanner.ScannerFragment
import com.google.ar.core.Plane
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrientationFragment : Fragment() {

    private var _binding: FragmentOrientationBinding? = null
    private val binding get() = _binding!!

    private val mainModel: MainShareModel by activityViewModels()

    private var navigating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrientationBinding.inflate(inflater, container, false)
        return binding.root
    }

    //In the onViewCreated() function, mainModel.frame is collected from the MainShareModel using
    // a coroutine. The frame is checked for any vertical planes using allPlanes.any { it.type == Plane.Type.VERTICAL }.

    // If a HORIZONTAL_UPWARD_FACING plane is detected and the device is not already navigating to the ScannerFragment,
    // then the navigating variable is set to true and a bundle is created with an integer value
    // of ScannerFragment.TYPE_INITIALIZE.
    //
    // The ScannerFragment is then navigated to using findNavController().navigate() with the bundle as an argument.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainModel.frame.collect { frame ->
                    frame?.let {
                        if (frame.session.allPlanes.any { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }) {
                            if (!navigating){
                                navigating = true
                                val bundle = Bundle()
                                bundle.putInt(
                                    ScannerFragment.SCAN_TYPE,
                                    ScannerFragment.TYPE_INITIALIZE
                                )
                                System.out.println("####################################")
                                System.out.println("### WAS NOT NAVIGATING ! ###")
                                System.out.println("####################################")
                                findNavController().navigate(R.id.action_orientationFragment_to_scannerFragment, args = bundle)
                            }
                            //@sahar added to test
                            System.out.println("//////////////////////////////////")
                            System.out.println("///// WAS NAVIGATING ////////////")
                            System.out.println("//////////////////////////////////")

                        }
                        println("LLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL")
                        println("LL HORIZONTAL PLANE DETECTED LL")
                        println("LLLLLLLLLLLLLLLLLLLLLLLLLLLLLLL")


                    }

                }
            }
        }
    }

    //In the onResume() function, the arrow image is animated using an ObjectAnimator. The animation
    // moves the arrow image in a loop along a path defined by Path().apply{...}.
    override fun onResume() {
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

            ObjectAnimator.ofFloat(it, View.X, View.Y, animationPath).apply {
                duration = animationDuration
                repeatCount = Animation.INFINITE
                start()
            }
        }
        super.onResume()
    }
}