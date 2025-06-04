package com.app.pustakam.android.hardware.sensors.accerometer

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.app.pustakam.util.log_d

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccelerometerManager(val context : Context) : SensorEventListener {
    private val _accState = MutableStateFlow<Triple<Float, Float, Float>>(Triple(0f,0f,0f))
    val accState =_accState.asStateFlow()
    private var accelerometer: Sensor? = null
    private val sensorManager : SensorManager =
        lazy { context.getSystemService(SENSOR_SERVICE) } as SensorManager
    init {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        _accState.update { it.copy(ax,ay,az) }
        log_d("gyroscope","x=$ax, y=$ay, z=$az")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        log_d("SensorAccuracy", "Sensor=${sensor?.name}, Accuracy=$accuracy")
    }

    fun registorAccelerometer(){
        accelerometer?.let{
            sensorManager.registerListener(this, it,SensorManager.SENSOR_DELAY_NORMAL )
        }
    }
    fun unRegistorAccelerometer(){
        accelerometer?.let{
            sensorManager.unregisterListener(this)
        }
    }
}