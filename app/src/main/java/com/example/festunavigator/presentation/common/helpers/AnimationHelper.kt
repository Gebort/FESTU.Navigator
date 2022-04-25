package com.example.festunavigator.presentation.common.helpers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.animation.TranslateAnimation
import androidx.core.view.isGone

class AnimationHelper {

    private val smallSlidingDuration = 200L
    private val fadeDuration = 100L

    fun slideViewDown(view: View) {
        if (!view.isGone) {
            view.isGone = true
            val animate = TranslateAnimation(
                0F,
                0F,
                0F,
                view.height.toFloat()
            )

            animate.duration = smallSlidingDuration
            animate.fillAfter = true
            view.startAnimation(animate)
        }
    }

    fun slideViewUp(view: View) {
        if (view.isGone) {
            view.isGone = false
            val animate = TranslateAnimation(
                0F,
                0F,
                view.height.toFloat(),
                0F
            )

            animate.duration = smallSlidingDuration
            animate.fillAfter = true
            view.startAnimation(animate)
        }
    }

    fun placeViewOut(view: View) {
        view.isGone = true
        view.layout(
            0,
            view.height + view.y.toInt(),
            view.width,
            0
        )
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
                    .setListener(null)
            }
        }
    }

    fun fadeHide(view: View) {
        if (!view.isGone) {
            view.animate()
                .alpha(0f)
                .setDuration(fadeDuration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.isGone = true
                    }
                })

        }
    }
}