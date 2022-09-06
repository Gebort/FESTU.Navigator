package com.example.festunavigator.data

import android.app.Application
import com.example.festunavigator.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        const val ADMIN_MODE = "admin"
        const val USER_MODE = "user"
        const val mode = BuildConfig.FLAVOR
    }
}