package com.gerbort.sensor_handling

import android.app.Activity
import android.hardware.SensorEventListener

internal interface AppSensor: SensorEventListener {

    /**
     * Register the sensor for execution.
     * @return true if registering was success, or false if sensor is not supported
     */
    fun register(activity: Activity): Boolean

    fun unregister()

}