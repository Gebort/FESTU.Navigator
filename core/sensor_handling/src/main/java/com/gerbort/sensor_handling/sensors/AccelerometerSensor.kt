package com.gerbort.sensor_handling.sensors

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.util.Log
import com.gerbort.sensor_handling.AppSensor
import com.gerbort.sensor_handling.OnSensorResult

internal class AccelerometerSensor(private val onSensorResult: OnSensorResult): AppSensor {

    private lateinit var sensorManager: SensorManager

    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private var azimuth = 0.0

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
            mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
            mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]
        }

        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0]
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1]
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2]
        }

        val R = FloatArray(9)
        val I = FloatArray(9)
        val success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)
        if (success) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(R, orientation)
               Log.d("testing", "ACCELEROMETER azimuth: " + orientation[0])
            azimuth = Math.toDegrees(orientation[0].toDouble()) // orientation
            azimuth = (azimuth + 360) % 360
            onSensorResult.onNewValue(azimuth)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun register(activity: Activity): Boolean {
        sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (gsensor == null || msensor == null ) {
            return false
        }
        sensorManager.registerListener(this, gsensor,
            SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, msensor,
            SensorManager.SENSOR_DELAY_GAME)
        return true
    }

    override fun unregister() {
        sensorManager.unregisterListener(this)
    }
}