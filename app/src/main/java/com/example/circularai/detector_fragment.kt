package com.example.circularai

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.fonts.Font
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.example.android.camerax.tflite.ObjectDetectionHelper
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class detector_fragment : Fragment(R.layout.fragment_detector) {

    private lateinit var context: FragmentActivity
    private lateinit var bitmapBuffer: Bitmap
    private val executor = Executors.newSingleThreadExecutor()
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private val isFrontFacing get() = lensFacing == CameraSelector.LENS_FACING_FRONT
    private lateinit var viewFinder: ImageView

    private val tfImageBuffer = TensorImage(DataType.UINT8)

    private val tfImageProcessor by lazy {
        val cropSize = minOf(bitmapBuffer.width, bitmapBuffer.height)
        ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)
            )
            //.add(Rot90Op(-90))
            .add(NormalizeOp(0f, 1f))
            .build()
    }

    private val nnApiDelegate by lazy  {
        NnApiDelegate()
    }

    private val tflite by lazy {

        Interpreter(
            FileUtil.loadMappedFile(context, MODEL_PATH),
            Interpreter.Options().addDelegate(nnApiDelegate))
    }

    private val detector by lazy {
        ObjectDetectionHelper(
            tflite,
            FileUtil.loadLabels(context, LABELS_PATH)
        )
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewFinder = view.findViewById(R.id.view_finder)

        super.onViewCreated(view, savedInstanceState)
        context = requireActivity()
        val parent = context.getParent()
        val status  = OpenCVLoader.initDebug()
        Log.i("OPENCV", "status $status")
        bindCameraUseCases()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Terminate all outstanding analyzing jobs (if there is any).
        /*
        executor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
         */
        Log.i("VIEW","view destroyed")
    }

    override fun onResume() {
        super.onResume()
    }
    fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** Declare and bind preview and analysis use cases */
    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindCameraUseCases() = viewFinder.post {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener ({

            // Camera provider is now guaranteed to be available
            val cameraProvider = cameraProviderFuture.get()


            // Set up the image analysis use case which will process frames in real time
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640,480))
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            var frameCounter = 0
            var lastFpsTimestamp = System.currentTimeMillis()

            imageAnalysis.setAnalyzer(executor) { image ->
                if (!::bitmapBuffer.isInitialized) {
                    // The image rotation and RGB image buffer are initialized only once
                    // the analyzer has started running
                    //imageRotationDegrees = image.imageInfo.rotationDegrees
                    bitmapBuffer = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888
                    )
                }
                //Log.i("IMAGE", "image width = ${image.width}, height=${image.height}")
                // Copy out RGB bits to our shared buffer
                image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
                val rotated_bitmap = bitmapBuffer.rotate(90.0f)
                // Process the image in Tensorflow
                val tfImage = tfImageProcessor.process(tfImageBuffer.apply { load(rotated_bitmap) })

                // Perform the object detection for the current frame
                val predictions = detector.predict(tfImage)
                //Log.i("DETECTOR", "$predictions")
                // Report only the top prediction
                //reportPrediction(predictions.maxByOrNull { it.score })

                val result_bitmap = augment_results(rotated_bitmap, predictions)
                viewFinder.post { viewFinder.setImageBitmap(result_bitmap) }


                // Compute the FPS of the entire pipeline
                val frameCount = 10
                if (++frameCounter % frameCount == 0) {
                    frameCounter = 0
                    val now = System.currentTimeMillis()
                    val delta = now - lastFpsTimestamp
                    val fps = 1000 * frameCount.toFloat() / delta
                    //Log.d(TAG, "FPS: ${"%.02f".format(fps)} with tensorSize: ${tfImage.width} x ${tfImage.height}")
                    lastFpsTimestamp = now
                }

                image.close()

            }

            // Create a new camera selector each time, enforcing lens facing
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

            // Apply declared configs to CameraX using the same lifecycle owner
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                this as LifecycleOwner, cameraSelector, imageAnalysis)

            camera.cameraControl.setLinearZoom(0.4f)
        }, ContextCompat.getMainExecutor(context))
    }

    fun augment_results(bitmap:Bitmap, predictions:List<ObjectDetectionHelper.ObjectPrediction>):Bitmap{

        var prediction_filtered = predictions.filter{ it.score > ACCURACY_THRESHOLD }

        if(prediction_filtered.size > 0){
            prediction_filtered = prediction_filtered.sortedBy { 1 - it.score }
        }

        if(prediction_filtered.size > 3){
            prediction_filtered = prediction_filtered.take(3)
        }

        if(prediction_filtered.size == 0){  return bitmap }


        //Log.i("PRED","$prediction_filtered")
        val mat = Mat()
        Utils.bitmapToMat(bitmap,mat)
        val width = bitmap.width
        val height = bitmap.height

        prediction_filtered.forEach({add_prediction(mat, it)})
        //val pred = prediction_filtered[0]

        val newBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, newBitmap)
        return newBitmap
    }
    fun add_prediction(mat:Mat, pred:ObjectDetectionHelper.ObjectPrediction){
        val width = mat.cols()
        val height = mat.rows()
        val dbl_array = DoubleArray(4)
        dbl_array.set(0,pred.location.left*width.toDouble())
        dbl_array.set(1,pred.location.top*height.toDouble())
        dbl_array.set(2,(pred.location.right - pred.location.left)*width.toDouble())
        dbl_array.set(3,(pred.location.bottom - pred.location.top)*height.toDouble())
        val rect = org.opencv.core.Rect(dbl_array)
        Imgproc.rectangle(mat, rect, Scalar(0.0),4)
        Imgproc.putText(mat, pred.label, Point(pred.location.left*width.toDouble(), pred.location.top*height.toDouble() - height*0.03), Imgproc.FONT_HERSHEY_SIMPLEX,2.0, Scalar(0.0),5)
    }

    companion object {
        private val TAG = detector_fragment::class.java.simpleName

        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
    }
}