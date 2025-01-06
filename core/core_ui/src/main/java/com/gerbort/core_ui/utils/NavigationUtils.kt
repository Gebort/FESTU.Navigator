package com.gerbort.core_ui.utils

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.gerbort.core_ui.R

fun NavController.navigateWithFade(deeplink: Uri) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_in)
            .setPopEnterAnim(R.anim.fade_out)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    navigate(deeplink, navOptions)
}

fun NavController.navigateWithSlide(deeplink: Uri) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.slide_down)
            .setPopEnterAnim(R.anim.slide_up)
            .setPopExitAnim(R.anim.slide_down)
            .build()
    navigate(deeplink, navOptions)
}

fun NavController.navigateSlideInFadeOut(deeplink: Uri) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.fade_out)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    navigate(deeplink, navOptions)
}

fun NavController.navigateFadeInSlideOut(deeplink: Uri) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.slide_down)
            .setPopEnterAnim(R.anim.slide_up)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    navigate(deeplink, navOptions)
}