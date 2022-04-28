package com.example.festunavigator.presentation.common.helpers

import android.content.Context
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isGone
import androidx.core.view.isInvisible


class AnimationHelper {

    private val interpolator: Interpolator = AccelerateInterpolator()
    private val smallSlidingDuration = 200L
    private val bigSlidingDuration = 500L
    private val fadeDuration = 200L

    fun slideViewDown(view: View, fast: Boolean = true, onEnd: (() -> Unit)? = null) {
        if (!view.isInvisible) {
            view.animate()
                .translationY(view.height.toFloat())
                .setDuration(
                    if (fast) smallSlidingDuration else bigSlidingDuration
                )
                .withEndAction {
                    view.isInvisible = true
                    onEnd?.let { onEnd() }
                }
                .interpolator = interpolator
        }
    }

    fun slideViewUp(view: View, fast: Boolean = true, onEnd: (() -> Unit)? = null) {
        if (view.isInvisible) {
            view.isInvisible = false
            view.animate()
                .translationY(0f)
                .setDuration(
                    if (fast) smallSlidingDuration else bigSlidingDuration
                )
                .withEndAction {
                    onEnd?.let { onEnd() }
                }
                .interpolator = interpolator
        }
    }

    fun placeViewOut(view: View) {
        view.translationY = view.height.toFloat()
        view.isInvisible = true
    }

    fun fadeShow(view: View) {
        if (view.isGone) {
            view.apply {
                // Set the content view to 0% opacity but visible, so that it is visible
                // (but fully transparent) during the animation.
                alpha = 0f
                isGone = false

                // Animate the content view to 100% opacity, and clear any animation
                // listener set on the view.
                animate()
                    .alpha(1f)
                    .setDuration(fadeDuration)
                    .start()
            }
        }
    }

    fun fadeHide(view: View) {
        if (!view.isGone) {
            view.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .withEndAction { view.isGone = true }
                .start()

        }
    }

    fun viewRequestInput(view: View, context: Context) {
        view.isActivated = true
        val hasFocus = view.requestFocus()
        hasFocus.let {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }
    }

    fun viewHideInput(view: View, context: Context) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}