package com.example.festunavigator.presentation.common.helpers

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

fun View.viewRequestInput() {
    viewHideInput()
    isActivated = true
    val hasFocus = requestFocus()
    hasFocus.let {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        imm.showSoftInput(this, 0)
    }
}

fun View.viewHideInput() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}