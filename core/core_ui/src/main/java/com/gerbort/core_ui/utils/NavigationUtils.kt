package com.gerbort.core_ui.utils

import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import com.gerbort.core_ui.R

fun NavController.navigateWithFade(uri: String) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.fade_in)
            .setPopEnterAnim(R.anim.fade_out)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    navigateUri(uri, navOptions)
}

fun NavController.navigateWithSlide(uri: String) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.slide_down)
            .setPopEnterAnim(R.anim.slide_up)
            .setPopExitAnim(R.anim.slide_down)
            .build()
    navigateUri(uri, navOptions)
}

fun NavController.navigateSlideInFadeOut(uri: String) {
    val navOptions =
        NavOptions.Builder()
            .setEnterAnim(R.anim.slide_up)
            .setExitAnim(R.anim.fade_out)
            .setPopEnterAnim(R.anim.fade_in)
            .setPopExitAnim(R.anim.fade_out)
            .build()
    navigateUri(uri, navOptions)
}

fun NavController.navigateFadeInSlideOut(uri: String, navOptions: NavOptions.Builder = NavOptions.Builder()) {
        navOptions
            .setEnterAnim(R.anim.fade_in)
            .setExitAnim(R.anim.slide_down)
            .setPopEnterAnim(R.anim.slide_up)
            .setPopExitAnim(R.anim.fade_out)
    navigateUri(uri, navOptions.build())
}

fun NavController.navigateUri(uri: String, navOptions: NavOptions? = null) {
    val request = NavDeepLinkRequest.Builder
        .fromUri(uri.toUri())
        .build()
    navigate(request, navOptions)
}

