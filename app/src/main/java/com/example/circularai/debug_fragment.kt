package com.example.circularai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import org.w3c.dom.Text


class debug_fragment : Fragment(R.layout.fragment_debug), SensorEventListener {

    lateinit var sensorManager: SensorManager
    lateinit var gyro_sensor: Sensor
    private var rotationMatrix = FloatArray(16)

    lateinit var title: TextView
    lateinit var e1: TextView
    lateinit var e2: TextView
    lateinit var e3: TextView

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireActivity()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyro_sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, gyro_sensor, SensorManager.SENSOR_DELAY_UI)  // change speed accordingly

        title = view.findViewById<TextView>(R.id.titile_tv)
        e1 = view.findViewById<TextView>(R.id.debug_1_tv)
        e2 = view.findViewById<TextView>(R.id.debug_2_tv)
        e3 = view.findViewById<TextView>(R.id.debug_3_tv)

        title.text = "Gyroscope Data"

        rotationMatrix[ 0] = 1F;
        rotationMatrix[ 4] = 1F;
        rotationMatrix[ 8] = 1F;
        rotationMatrix[12] = 1F;
    }

    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){

                // convert the rotation-vector to a 4x4 matrix. the matrix
                // is interpreted by Open GL as the inverse of the
                // rotation-vector, which is what we want.
                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix , event.values);
                e1.text = "X: ${event.values[0]}"
                e2.text = "X: ${event.values[1]}"
                e3.text = "X: ${event.values[2]}"
            }
        }

    }

     override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
     }
 }