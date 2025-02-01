package com.gerbort.sensor_handling

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.gerbort.core_ui.azimuth_holder.AzimuthConsumer
import com.gerbort.sensor_handling.sensors.AccelerometerSensor
import com.gerbort.sensor_handling.sensors.OrientationSensor
import com.gerbort.sensor_handling.sensors.RotationSensor
import javax.inject.Inject

class SensorHandler @Inject constructor(
    private val azimuthConsumer: AzimuthConsumer
): SensorEventListener, OnSensorResult {

    private var currentSensor: AppSensor? = null

    private val sensorsByPriority = listOf(
        lazy { RotationSensor(this) },
        lazy { OrientationSensor(this) },
        lazy { AccelerometerSensor(this) }
    )


    override fun onSensorChanged(event: SensorEvent) {
        currentSensor?.onSensorChanged(event)
    }

    override fun onAccuracyChanged(p0: Sensor, p1: Int) {
        currentSensor?.onAccuracyChanged(p0, p1)
    }

    fun register(activity: Activity) {
        sensorsByPriority.forEach { sensorLazy ->
            val sensor = sensorLazy.value
            if (sensor.register(activity)) {
                currentSensor = sensor
                return
            }
        }
    }

    fun unregister() {
        currentSensor?.unregister()
    }

    override fun onNewValue(value: Double) {
        azimuthConsumer.newAzimuth(value)
    }


}