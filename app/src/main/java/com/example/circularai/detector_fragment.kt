package com.example.circularai

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.ImageView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResult
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
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors
import kotlin.properties.Delegates


class detector_fragment : Fragment(R.layout.fragment_detector) {

    private lateinit var context: FragmentActivity
    private lateinit var bitmapBuffer: Bitmap
    private val executor = Executors.newSingleThreadExecutor()
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var viewFinder: ImageView

    lateinit var COLOR_GLASS: Scalar
    lateinit var COLOR_METAL: Scalar
    lateinit var COLOR_PAPER: Scalar
    lateinit var COLOR_PLASTIC: Scalar
    val COLOR_TEXT  = Scalar(255.0, 255.0, 255.0, 255.0)


    private lateinit var classification_array: Array<MutableList<String>>
    val types = arrayOf("glass", "paper", "metal", "plastic")
    val types_reverse_map_mutable = mutableMapOf<String, Int>()


    lateinit var colors_array:Array<Scalar>
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
            tflite
        )
    }
    private val labelString by lazy{
        FileUtil.loadLabels(context, LABELS_PATH)
    }
    private val labelType by lazy{
        val reader = BufferedReader(InputStreamReader(context.assets.open(LABELS_TYPE_PATH),"UTF-8"))
        val labelType = ArrayList<Int>()
        var type:Int
        var lineString: String?
        while (true) {
            lineString = reader.readLine()
            Log.i("LOOP","$lineString")
            if(lineString == null){ break}else{
                type = types_reverse_map_mutable[lineString!!.split(",")[1].replace("\\s".toRegex(), "")]?:0
            }
            labelType.add(type)
        }
        return@lazy labelType
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

        //bind fragments


        types.forEachIndexed { index, s -> types_reverse_map_mutable.put(s,index)}
        val types_reverse_map = types_reverse_map_mutable.toMap()
        COLOR_GLASS = color_to_scalar(resources.getColor(R.color.Glass))
        COLOR_METAL = color_to_scalar(resources.getColor(R.color.Metal))
        COLOR_PAPER = color_to_scalar(resources.getColor(R.color.Paper))
        COLOR_PLASTIC = color_to_scalar(resources.getColor(R.color.Plastic))

        colors_array = Array<Scalar>(types.size,{Scalar(0.0)})

        colors_array[types_reverse_map["glass"]?:0] = COLOR_GLASS
        colors_array[types_reverse_map["metal"]?:0] = COLOR_METAL
        colors_array[types_reverse_map["paper"]?:0] = COLOR_PAPER
        colors_array[types_reverse_map["plastic"]?:0] = COLOR_PLASTIC


        val top_left_fragment = RecyclingFragment(types[0])
        val top_right_fragment = RecyclingFragment(types[1])
        val bottom_left_fragment = RecyclingFragment(types[2])
        val bottom_right_fragment = RecyclingFragment(types[3])

        placeFragment(top_left_fragment, R.id.top_left_fragment_container)
        placeFragment(top_right_fragment, R.id.top_right_fragment_container)
        placeFragment(bottom_left_fragment, R.id.bottom_left_fragment_container)
        placeFragment(bottom_right_fragment, R.id.bottom_right_fragment_container)

        val fragments = mapOf<String, RecyclingFragment>(
            types[0] to top_left_fragment,
            types[1] to top_right_fragment,
            types[2] to bottom_left_fragment,
            types[3] to bottom_right_fragment
        )
        //set titles
        fragments.forEach { (string, fragment) ->
            setFragmentResult(string, bundleOf("type" to string))
        }


        super.onViewCreated(view, savedInstanceState)
        context = requireActivity()
        val parent = context.getParent()
        val status  = OpenCVLoader.initDebug()
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

        //Log.i("MATRIX",mat.)

        //var mutable_list = Array(4){MutableList<String>(0){""} }
        classification_array = Array(types.size){MutableList<String>(0, {""})}
        classification_array.forEach { it.clear() }
        prediction_filtered.forEach({add_prediction(mat, it)})
        //val pred = prediction_filtered[0]
        //Log.i("CLASSIFICATION ARRAY","${classification_array[2]}")

        //classification_array[0] = arrayOf("First").toList()
        //classification_array[1] = arrayOf("Second", "Second","Second").toList()
        //classification_array[2] = arrayOf("Third").toList()
        //classification_array[3] = arrayOf("Fourth", "Fourth","Fourth").toList()

        classification_array.forEachIndexed{index, list ->
            setFragmentResult(types[index], bundleOf("list" to list.toList()))
        }

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
        var rectangle_color = colors_array[labelType[pred.label]];

        var label_string = labelString[pred.label]
        classification_array[labelType[pred.label]].add(label_string)

        Imgproc.rectangle(mat, rect, rectangle_color,8)
        Imgproc.putText(mat, label_string, Point(pred.location.left*width.toDouble(), pred.location.top*height.toDouble() - height*0.03), Imgproc.FONT_HERSHEY_SIMPLEX,2.0, COLOR_TEXT,5)
    }



    companion object {
        private val TAG = detector_fragment::class.java.simpleName
        private const val ACCURACY_THRESHOLD = 0.5f
        private const val MODEL_PATH = "coco_ssd_mobilenet_v1_1.0_quant.tflite"
        private const val LABELS_PATH = "coco_ssd_mobilenet_v1_1.0_labels.txt"
        private const val LABELS_TYPE_PATH = "object_trash_type.txt"
    }

    private fun color_to_scalar(c: Int): Scalar {
        val color = Color.valueOf(c)
        return Scalar(
            color.red() * 255.toDouble(),
            color.blue() * 255.toDouble(),
            color.green() * 255.toDouble(),
            255.0
        )
    }

    fun placeFragment(fragment: Fragment, fragmentID:Int): Boolean {

        parentFragmentManager.beginTransaction().apply {
            replace(fragmentID, fragment)
            addToBackStack(null)
            commit()
            return true
        }
    }
}