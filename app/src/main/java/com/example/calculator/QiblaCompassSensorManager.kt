package com.example.calculator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Sensor orientation and compass state payload.
 */
data class CompassSensorState(
    val heading: Float = 0f,              // Azimuth in degrees (0..360) clockwise from North
    val pitch: Float = 0f,                // Forward/backward tilt (-90..90 deg)
    val roll: Float = 0f,                 // Left/right tilt (-180..180 deg)
    val isLevel: Boolean = true,          // True if phone is held relatively flat (within +/- 15 deg)
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val hasMagnetometer: Boolean = true,
    val hasAccelerometer: Boolean = true,
    val isSensorActive: Boolean = false
)

/**
 * Manages device Accelerometer and Magnetometer sensors to compute
 * real-time filtered azimuth heading, pitch, roll, and device flatness.
 */
class QiblaCompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _compassState = MutableStateFlow(
        CompassSensorState(
            hasMagnetometer = magnetometer != null || rotationVector != null,
            hasAccelerometer = accelerometer != null || rotationVector != null
        )
    )
    val compassState: StateFlow<CompassSensorState> = _compassState.asStateFlow()

    // Sensor raw arrays
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    // Low-pass filter smoothing coefficient (alpha)
    // 0.15f provides responsive yet jitter-free needle movement
    private val ALPHA = 0.18f

    // Internal tracking of smoothed heading to handle 0/360 wrap-around smoothly
    private var smoothedHeading = 0f
    private var lastAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH

    fun startListening() {
        if (sensorManager == null) {
            _compassState.value = _compassState.value.copy(
                hasMagnetometer = false,
                hasAccelerometer = false,
                isSensorActive = false
            )
            return
        }

        hasGravity = false
        hasGeomagnetic = false

        // Register Accelerometer and Magnetometer
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        // Also register rotation vector if available for higher accuracy fusion fallback
        if (rotationVector != null && (accelerometer == null || magnetometer == null)) {
            sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
        }

        _compassState.value = _compassState.value.copy(
            isSensorActive = true,
            hasMagnetometer = magnetometer != null || rotationVector != null,
            hasAccelerometer = accelerometer != null || rotationVector != null
        )
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        _compassState.value = _compassState.value.copy(isSensorActive = false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                applyLowPassFilter(event.values, gravityValues)
                hasGravity = true
                calculateOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                applyLowPassFilter(event.values, geomagneticValues)
                hasGeomagnetic = true
                calculateOrientation()
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                // Secondary fused sensor fallback
                if (!hasGravity || !hasGeomagnetic) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    processOrientationMatrix(rotationMatrix)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD || sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            lastAccuracy = accuracy
            _compassState.value = _compassState.value.copy(accuracy = accuracy)
        }
    }

    /**
     * Exponential Moving Average (EMA) low pass filter.
     */
    private fun applyLowPassFilter(input: FloatArray, output: FloatArray) {
        for (i in input.indices) {
            output[i] = output[i] + ALPHA * (input[i] - output[i])
        }
    }

    private fun calculateOrientation() {
        if (!hasGravity || !hasGeomagnetic) return

        val rMatrix = FloatArray(9)
        val iMatrix = FloatArray(9)

        val success = SensorManager.getRotationMatrix(rMatrix, iMatrix, gravityValues, geomagneticValues)
        if (success) {
            processOrientationMatrix(rMatrix)
        }
    }

    private fun processOrientationMatrix(rMatrix: FloatArray) {
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rMatrix, orientation)

        // Azimuth (rad to deg) -> 0..360
        val azimuthRad = orientation[0]
        var rawHeadingDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        rawHeadingDeg = (rawHeadingDeg + 360f) % 360f

        // Smooth angle transitions across the 0° / 360° discontinuity
        var delta = rawHeadingDeg - smoothedHeading
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        smoothedHeading = (smoothedHeading + delta * 0.25f + 360f) % 360f

        // Pitch & Roll (tilt calculation)
        val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

        // Device is considered level if tilt is within +/- 15 degrees
        val isLevel = abs(pitchDeg) <= 15f && abs(rollDeg) <= 15f

        _compassState.value = CompassSensorState(
            heading = smoothedHeading,
            pitch = pitchDeg,
            roll = rollDeg,
            isLevel = isLevel,
            accuracy = lastAccuracy,
            hasMagnetometer = magnetometer != null || rotationVector != null,
            hasAccelerometer = accelerometer != null || rotationVector != null,
            isSensorActive = true
        )
    }
}
