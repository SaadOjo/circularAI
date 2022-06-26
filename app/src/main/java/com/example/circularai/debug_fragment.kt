package com.example.circularai

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.os.SystemClock
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.core.graphics.rotationMatrix
import androidx.fragment.app.FragmentActivity
import org.opencv.core.Mat
import org.w3c.dom.Text
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


private var gyroRotationMatrix = FloatArray(16)

// number of coordinates per vertex in this array
const val COORDS_PER_VERTEX = 3
/*
var objectCoords = floatArrayOf(     // in counterclockwise order:
    0.0f, 0.622008459f, 0.0f,      // top
    -0.5f, -0.311004243f, 0.0f,    // bottom left
    0.5f, -0.311004243f, 0.0f      // bottom right
)
 */
var objectCoords = floatArrayOf(     // in counterclockwise order:
    -1.0f, -1.0f,  1.0f,
     1.0f, -1.0f,  1.0f,      // top
     1.0f,  1.0f,  1.0f,      // top

     1.0f,  1.0f,  1.0f,
    -1.0f,  1.0f,  1.0f,      // top
    -1.0f, -1.0f,  1.0f,      // top

    -1.0f, -1.0f, -1.0f,
     1.0f, -1.0f, -1.0f,      // top
     1.0f,  1.0f, -1.0f,      // top

     1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,      // top
    -1.0f, -1.0f, -1.0f,      // top

     1.0f,  1.0f,  1.0f,
     1.0f,  1.0f, -1.0f,      // top
    -1.0f,  1.0f,  1.0f,      // top

     1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,      // top
    -1.0f,  1.0f,  1.0f,      // top


     1.0f, -1.0f,  1.0f,
     1.0f, -1.0f, -1.0f,      // top
    -1.0f, -1.0f,  1.0f,      // top

     1.0f, -1.0f, -1.0f,
    -1.0f, -1.0f, -1.0f,      // top
    -1.0f, -1.0f,  1.0f,      // top

     1.0f, -1.0f,  1.0f,
     1.0f, -1.0f, -1.0f,      // top
     1.0f,  1.0f, -1.0f,      // top

     1.0f,  1.0f, -1.0f,
     1.0f,  1.0f,  1.0f,      // top
     1.0f, -1.0f,  1.0f,      // top

    -1.0f, -1.0f,  1.0f,
    -1.0f, -1.0f, -1.0f,      // top
    -1.0f,  1.0f, -1.0f,      // top

    -1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f,  1.0f,      // top
    -1.0f, -1.0f,  1.0f,      // top


)

fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
    return GLES20.glCreateShader(type).also { shader ->

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
    }
}
class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer

    init {

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        //renderMode = RENDERMODE_CONTINUOUSLY
        renderer = MyGLRenderer()

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)
    }
}


class MyGLRenderer : GLSurfaceView.Renderer {

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var vPMatrixHandle: Int = 0

    val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    private val vertexCount: Int = objectCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private var mProgram: Int = 0

    private var vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(objectCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(objectCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }


    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val aMatrix = FloatArray(16)

    fun init(){
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }

    }

    //private lateinit var mTriangle: Triangle

    fun draw(mvpMatrix: FloatArray) {
        GLES20.glUseProgram(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // get handle to shape's transformation matrix
            vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix").also {PMatrixHandle ->
                GLES20.glUniformMatrix4fv(PMatrixHandle, 1, false, mvpMatrix, 0)

            }

            // Pass the projection and view transformation to the shader

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)


        }
    }


    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        init()
    }

    override fun onDrawFrame(unused: GL10) {
        val scratch = FloatArray(16)
        val time =  SystemClock.uptimeMillis() % 4000L
        val angle = 0.090f * time.toInt()
        //Matrix.setRotateM(aMatrix, 0, 90.0f, 1f, 0f, 0f)
        Matrix.setIdentityM(aMatrix,0)
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 6f, 0f, 0f, 0f,0f, 1.0f, 0.0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        //Matrix.rotateM(aMatrix, 0, 0.0f, 0f, 0f, -1.0f)
        //Matrix.setIdentityM(aMatrix, 0)
        Matrix.multiplyMM(rotationMatrix, 0, aMatrix,0 ,gyroRotationMatrix, 0)
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0 )
        //Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0 )
        draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat()/ height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0,60.0f ,ratio,0.1f,10.0f)
        //Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

}

class debug_fragment : Fragment(R.layout.fragment_debug), SensorEventListener {

    private lateinit var context: FragmentActivity
    lateinit var sensorManager: SensorManager
    lateinit var gyro_sensor: Sensor


    private lateinit var glView: GLSurfaceView

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyro_sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(this, gyro_sensor, SensorManager.SENSOR_DELAY_GAME)  // change speed accordingly

         gyroRotationMatrix[ 0] = 1F;
         gyroRotationMatrix[ 4] = 1F;
         gyroRotationMatrix[ 8] = 1F;
         gyroRotationMatrix[12] = 1F;

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        context = requireActivity()
        glView = MyGLSurfaceView(context)
        return glView
    }
    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null) {
            if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){

                SensorManager.getRotationMatrixFromVector(
                    gyroRotationMatrix , event.values);
            }
        }
    }

     override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
     }
 }