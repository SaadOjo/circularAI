package com.example.circularai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
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

/*
var objectCoords = floatArrayOf(     // in counterclockwise order:

    //Front Square
    -1.0f, -1.0f,  1.0f,
     1.0f, -1.0f,  1.0f,
     1.0f,  1.0f,  1.0f,

     1.0f,  1.0f,  1.0f,
    -1.0f,  1.0f,  1.0f,
    -1.0f, -1.0f,  1.0f,

    //Top Square
     1.0f,  1.0f,  1.0f,
     1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f,  1.0f,

     1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f, -1.0f,
    -1.0f,  1.0f,  1.0f,

    //right square
     1.0f, -1.0f,  1.0f,
     1.0f, -1.0f, -1.0f,
     1.0f,  1.0f, -1.0f,

     1.0f,  1.0f, -1.0f,
     1.0f,  1.0f,  1.0f,
     1.0f, -1.0f,  1.0f,

)
 */

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

fun square_texture_coordinates(x: Float, y: Float): FloatArray{
    return floatArrayOf(      x,    y,
                        x+0.25f,    y,
                        x+0.25f,    y+0.25f,

                        x+0.25f,    y+0.25f,
                              x,    y+0.25f,
                              x,    y,
        )
}

fun square_plane_coords(tM:FloatArray):FloatArray{

    val base_array = floatArrayOf(
                                //Front Square
                                -1.0f, -1.0f,  1.0f,
                                 1.0f, -1.0f,  1.0f,
                                 1.0f,  1.0f,  1.0f,

                                 1.0f,  1.0f,  1.0f,
                                -1.0f,  1.0f,  1.0f,
                                -1.0f, -1.0f,  1.0f,
    )

    var res = FloatArray(0)
    var interm = FloatArray(4)
    for(i in 0..5){
        Matrix.multiplyMV(interm, 0, tM, 0, base_array.sliceArray(i*3 .. i*3 + 2) + floatArrayOf(1f),0 )
        res = res + interm.sliceArray(0..2)
    }
    return res;
}
var plane_transformation_matrix = FloatArray(16)


class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer
    private val my_context = context

    init {

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)
        //renderMode = RENDERMODE_CONTINUOUSLY
        renderer = MyGLRenderer(my_context)

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer)
    }
}


class MyGLRenderer(context: Context) : GLSurfaceView.Renderer {

    private val my_context = context
    private var objectCoords: FloatArray
    private var textureCoords: FloatArray

    init {
        Matrix.setIdentityM(plane_transformation_matrix, 0)
        objectCoords = square_plane_coords(plane_transformation_matrix)

        Matrix.setIdentityM(plane_transformation_matrix, 0)
        Matrix.rotateM(plane_transformation_matrix, 0, -90f,1f,0f,0f)
        objectCoords = objectCoords + square_plane_coords(plane_transformation_matrix)


        Matrix.setIdentityM(plane_transformation_matrix, 0)
        Matrix.rotateM(plane_transformation_matrix, 0, 90f,1f,0f,0f)
        objectCoords = objectCoords + square_plane_coords(plane_transformation_matrix)

        Matrix.setIdentityM(plane_transformation_matrix, 0)
        Matrix.rotateM(plane_transformation_matrix, 0, -90f,0f,1f,0f)
        objectCoords = objectCoords + square_plane_coords(plane_transformation_matrix)

        Matrix.setIdentityM(plane_transformation_matrix, 0)
        Matrix.rotateM(plane_transformation_matrix, 0, 90f,0f,1f,0f)
        objectCoords = objectCoords + square_plane_coords(plane_transformation_matrix)

        Matrix.setIdentityM(plane_transformation_matrix, 0)
        Matrix.rotateM(plane_transformation_matrix, 0, 180f,0f,1f,0f)
        objectCoords = objectCoords + square_plane_coords(plane_transformation_matrix)


        textureCoords = square_texture_coordinates(0.25f, 0.5f)  +
        square_texture_coordinates(0.25f, 0.75f) +
        square_texture_coordinates(0.25f, 0.25f) +
        square_texture_coordinates(0f, 0.5f)     +
        square_texture_coordinates(0.5f, 0.5f) +
        square_texture_coordinates(0.75f, 0.5f)


    }


    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0
    private var texPositionHandle: Int = 0
    private var texUniformHandle: Int = 0
    private var vPMatrixHandle: Int = 0

    val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    private val vertexCount: Int = objectCoords.size / COORDS_PER_VERTEX
    private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private var mProgram: Int = 0

    private var vertexBuffer = to_float_buffer(objectCoords)
    private var texCoordsBuffer = to_float_buffer(textureCoords)


    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private val aMatrix = FloatArray(16)
    private lateinit var textureBitmap: Bitmap;

    fun to_float_buffer(float_array: FloatArray): FloatBuffer{
                // (number of coordinate values * 4 bytes per float)
            return ByteBuffer.allocateDirect(float_array.size * 4).run {
                // use the device hardware's native byte order
                order(ByteOrder.nativeOrder())

                // create a floating point buffer from the ByteBuffer
                asFloatBuffer().apply {
                    // add the coordinates to the FloatBuffer
                    put(float_array)
                    // set the buffer to read the first coordinate
                    position(0)
                }
            }
    }

    private fun init(){

        val VERTEX_SHADER_NAME = "shaders/vertexShader.vert"
        val FRAGMENT_SHADER_NAME = "shaders/fragmentShader.frag"
        val vertexShader =
            ShaderUtil.loadGLShader("SHADER",my_context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME)
        val fragmentShader =
            ShaderUtil.loadGLShader("SHADER", my_context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME)




        //val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        //val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }



        textureBitmap =
            BitmapFactory.decodeStream(my_context.assets.open("garbage.png"))

        //Enable blend
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        //Uses to prevent transparent area to turn in black
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val textureUnit = IntArray(1)


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glGenTextures(textureUnit.size, textureUnit, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureUnit[0])

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        textureBitmap.recycle()




    }

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
        }
        texPositionHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoords").also {

            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                2,
                GLES20.GL_FLOAT,
                false,
                2*4,
                texCoordsBuffer
            )
        }

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
            // Set color for drawing the triangle
            GLES20.glUniform4fv(colorHandle, 1, color, 0)
        }

        // get handle to shape's transformation matrix
        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix").also {PMatrixHandle ->
            GLES20.glUniformMatrix4fv(PMatrixHandle, 1, false, mvpMatrix, 0)

        }

        texUniformHandle = GLES20.glGetUniformLocation(mProgram, "uTexture").also {
            GLES20.glUniform1i(it, 0)
        }

        // Pass the projection and view transformation to the shader

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texPositionHandle)

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
        // Redraw background color and clear depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
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

         context =  requireActivity()

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