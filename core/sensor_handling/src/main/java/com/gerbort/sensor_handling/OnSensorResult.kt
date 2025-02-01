package com.gerbort.sensor_handling

fun interface OnSensorResult {

    fun onNewValue(value: Double)

}