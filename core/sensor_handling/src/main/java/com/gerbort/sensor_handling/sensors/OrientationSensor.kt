package com.gerbort.sensor_handling.sensors

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.view.Surface
import com.gerbort.sensor_handling.AppSensor
import com.gerbort.sensor_handling.OnSensorResult
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt


internal class OrientationSensor(private val onSensorResult: OnSensorResult) : AppSensor {
    private lateinit var sensorManager: SensorManager
    private var activity: Activity? = null // current activity for call to getWindowManager().getDefaultDisplay().getRotation()

    // raw inputs from Android sensors
    private var normGravity: Float = 0f // length of raw gravity vector received in onSensorChanged(...).  NB: should be about 10
    private var normGravityVector: FloatArray? // Normalised gravity vector, (i.e. length of this vector is 1), which points straight up into space
    private var normMagField: Float = 0f // length of raw magnetic field vector received in onSensorChanged(...).
    private var normMagFieldValues: FloatArray? = null // Normalised magnetic field vector, (i.e. length of this vector is 1)

    // accuracy specifications. SENSOR_UNAVAILABLE if unknown, otherwise SensorManager.SENSOR_STATUS_UNRELIABLE, SENSOR_STATUS_ACCURACY_LOW, SENSOR_STATUS_ACCURACY_MEDIUM or SENSOR_STATUS_ACCURACY_HIGH
    private var gravityAccuracy: Int = 0 // accuracy of gravity sensor
    private var magneticFieldAccuracy: Int = 0 // accuracy of magnetic field sensor

    // values calculated once gravity and magnetic field vectors are available
    private var normEastVector: FloatArray // normalised cross product of raw gravity vector with magnetic field values, points east
    private var normNorthVector: FloatArray // Normalised vector pointing to magnetic north
    private var orientationOK: Boolean // set true if m_azimuth_radians and m_pitch_radians have successfully been calculated following a call to onSensorChanged(...)
    private var azimuthRadians: Float = 0f // angle of the device from magnetic north
    private var pitchRadians: Float = 0f // tilt angle of the device from the horizontal.  m_pitch_radians = 0 if the device if flat, m_pitch_radians = Math.PI/2 means the device is upright.
    private var pitchAxisRadians: Float = 0f // angle which defines the axis for the rotation m_pitch_radians

    init {
        normGravityVector = normMagFieldValues
        normEastVector = FloatArray(3)
        normNorthVector = FloatArray(3)
        orientationOK = false
    }

    override fun register(activity: Activity): Boolean {
        this.sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        this.activity = activity // current activity required for call to getWindowManager().getDefaultDisplay().getRotation()
        normGravityVector = FloatArray(3)
        normMagFieldValues = FloatArray(3)
        orientationOK = false
        val sensorGravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val sensorMagField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sensorGravity == null || sensorMagField == null) {
            gravityAccuracy = SENSOR_UNAVAILABLE
            magneticFieldAccuracy = SENSOR_UNAVAILABLE
            return false
        }
        sensorManager.registerListener(this, sensorGravity, SensorManager.SENSOR_DELAY_GAME)
        gravityAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        sensorManager.registerListener(this, sensorMagField, SensorManager.SENSOR_DELAY_GAME)
        magneticFieldAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        return true
    }

    override fun unregister() {
        activity = null
        normMagFieldValues = null
        normGravityVector = normMagFieldValues
        orientationOK = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(evnt: SensorEvent) {
        val sensorType = evnt.sensor.type
        when (sensorType) {
            Sensor.TYPE_GRAVITY -> {
                if (normGravityVector == null) normGravityVector = FloatArray(3)
                System.arraycopy(evnt.values, 0, normGravityVector, 0, normGravityVector!!.size)
                normGravity =
                    sqrt((normGravityVector!![0] * normGravityVector!![0] + normGravityVector!![1] * normGravityVector!![1] + normGravityVector!![2] * normGravityVector!![2]).toDouble()).toFloat()
                var i = 0
                while (i < normGravityVector!!.size) {
                    normGravityVector!![i] /= normGravity
                    i++
                }
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (normMagFieldValues == null) normMagFieldValues = FloatArray(3)
                System.arraycopy(
                    evnt.values,
                    0,
                    normMagFieldValues,
                    0,
                    normMagFieldValues!!.size
                )
                normMagField =
                    sqrt((normMagFieldValues!![0] * normMagFieldValues!![0] + normMagFieldValues!![1] * normMagFieldValues!![1] + normMagFieldValues!![2] * normMagFieldValues!![2]).toDouble()).toFloat()
                var i = 0
                while (i < normMagFieldValues!!.size) {
                    normMagFieldValues!![i] /= normMagField
                    i++
                }
            }
        }
        if (normGravityVector != null && normMagFieldValues != null) {
            // first calculate the horizontal vector that points due east
            val eastX =
                normMagFieldValues!![1] * normGravityVector!![2] - normMagFieldValues!![2] * normGravityVector!![1]
            val eastY =
                normMagFieldValues!![2] * normGravityVector!![0] - normMagFieldValues!![0] * normGravityVector!![2]
            val eastZ =
                normMagFieldValues!![0] * normGravityVector!![1] - normMagFieldValues!![1] * normGravityVector!![0]
            val normEast =
                sqrt((eastX * eastX + eastY * eastY + eastZ * eastZ).toDouble()).toFloat()
            if (normGravity * normMagField * normEast < 0.1f) {  // Typical values are  > 100.
                orientationOK =
                    false // device is close to free fall (or in space?), or close to magnetic north pole.
            } else {
                normEastVector[0] = eastX / normEast
                normEastVector[1] = eastY / normEast
                normEastVector[2] = eastZ / normEast

                // next calculate the horizontal vector that points due north
                val mdotG = (normGravityVector!![0] * normMagFieldValues!![0] + normGravityVector!![1] * normMagFieldValues!![1] + normGravityVector!![2] * normMagFieldValues!![2])
                val northX = normMagFieldValues!![0] - normGravityVector!![0] * mdotG
                val northY = normMagFieldValues!![1] - normGravityVector!![1] * mdotG
                val northZ = normMagFieldValues!![2] - normGravityVector!![2] * mdotG
                val normNorth =
                    sqrt((northX * northX + northY * northY + northZ * northZ).toDouble()).toFloat()
                normNorthVector[0] = northX / normNorth
                normNorthVector[1] = northY / normNorth
                normNorthVector[2] = northZ / normNorth

                // take account of screen rotation away from its natural rotation
                val rotation = activity!!.windowManager.defaultDisplay.rotation
                var screenAdjustment = 0f
                when (rotation) {
                    Surface.ROTATION_0 -> screenAdjustment = 0f
                    Surface.ROTATION_90 -> screenAdjustment = Math.PI.toFloat() / 2
                    Surface.ROTATION_180 -> screenAdjustment = Math.PI.toFloat()
                    Surface.ROTATION_270 -> screenAdjustment = 3 * Math.PI.toFloat() / 2
                }

                // NB: the rotation matrix has now effectively been calculated. It consists of the three vectors m_NormEastVector[], m_NormNorthVector[] and m_NormGravityVector[]

                // calculate all the required angles from the rotation matrix
                // NB: see https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
                var sin = normEastVector[1] - normNorthVector[0]
                var cos = normEastVector[0] + normNorthVector[1]
                azimuthRadians = (if (sin != 0f && cos != 0f) atan2(
                    sin.toDouble(),
                    cos.toDouble()
                ) else 0).toFloat()
                pitchRadians = acos(normGravityVector!![2].toDouble()).toFloat()
                sin = -normEastVector[1] - normNorthVector[0]
                cos = normEastVector[0] - normNorthVector[1]
                val aximuthPlusTwoPitchAxisRadians = (if (sin != 0f && cos != 0f) atan2(
                    sin.toDouble(),
                    cos.toDouble()
                ) else 0).toFloat()
                pitchAxisRadians = (aximuthPlusTwoPitchAxisRadians - azimuthRadians) / 2
                azimuthRadians += screenAdjustment
                pitchAxisRadians += screenAdjustment
                //Log.d("testing", "ORIENTATION azimuth: $azimuthRadians")
                orientationOK = true
                onSensorResult.onNewValue(Math.toDegrees(azimuthRadians.toDouble()))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        val sensorType = sensor.type
        when (sensorType) {
            Sensor.TYPE_GRAVITY -> gravityAccuracy = accuracy
            Sensor.TYPE_MAGNETIC_FIELD -> magneticFieldAccuracy = accuracy
        }
    }

    companion object {
        const val SENSOR_UNAVAILABLE: Int = -1
    }
}