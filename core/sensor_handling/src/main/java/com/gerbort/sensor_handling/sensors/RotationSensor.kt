package com.gerbort.sensor_handling.sensors

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import com.gerbort.sensor_handling.AppSensor
import com.gerbort.sensor_handling.OnSensorResult


class RotationSensor(private val onSensorResult: OnSensorResult): AppSensor {

    private lateinit var sensorManager: SensorManager

    private val mMatrixR = FloatArray(9)
    private val mMatrixValues = FloatArray(3)

    override fun register(activity: Activity): Boolean {
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return false
        sensorManager.registerListener(this, rsensor,
            SensorManager.SENSOR_DELAY_GAME)
        return true
    }

    override fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(p0: SensorEvent) {
        when (p0.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Get rotation matrix
                SensorManager.getRotationMatrixFromVector(mMatrixR, p0.values)

                SensorManager.getOrientation(mMatrixR, mMatrixValues)

                // Use this value in degrees
                onSensorResult.onNewValue(Math.toDegrees(mMatrixValues[0].toDouble()))
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}