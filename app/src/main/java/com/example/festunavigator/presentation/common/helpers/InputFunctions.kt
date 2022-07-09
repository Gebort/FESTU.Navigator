package com.example.festunavigator.presentation.common.helpers

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

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