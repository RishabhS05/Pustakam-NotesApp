package com.app.pustakam.android.hardware.sensors.gyroscopeManager

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.app.pustakam.core.common.util.log_d
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class GyroscopeManager(context : Context) : SensorEventListener {
val _gyState = MutableStateFlow<Triple<Float, Float, Float>>(Triple(0f,0f,0f))
    val gyState =_gyState.asStateFlow()
    private var gyroscope: Sensor? = null
   private val sensorManager : SensorManager = lazy {
        context.getSystemService(SENSOR_SERVICE)
    } as SensorManager
    init {
     gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]
        _gyState.update {
            it.copy(gx,gy,gz)
        }
        log_d("gyroscope","x=$gx, y=$gy, z=$gz" )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        log_d("SensorAccuracy", "Sensor=${sensor?.name}, Accuracy=$accuracy")
    }
    fun registorGyroscope(){
        gyroscope?.let{
            sensorManager.registerListener(this, it,SensorManager.SENSOR_DELAY_NORMAL )
        }
    }
    fun unregistorGyroscope(){
        gyroscope?.let{
            sensorManager.unregisterListener(this)
        }
    }
}