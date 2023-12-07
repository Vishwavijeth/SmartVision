package com.example.application

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File

class ImageClassifierHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val imageClassifierListener: ClassifierListener?
)

{
    private var filename: String? = null
    private var file: File? = null
    private var imageClassifier: ImageClassifier? = null

    init {
        //setupImageClassifier()
    }

    fun clearImageClassifier() {
        imageClassifier = null
    }

    fun getData(value: File?){
        if (value != null){
            Log.d(TAG, "model name received")
            //filename = value
            file = value
            Log.d(TAG, "name: $filename")
        }else{
            Log.d(TAG, "no model name received from the camera fragment")
        }
    }

    private fun setupImageClassifier()
    {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageClassifierListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try
        {
            if (filename != null){
                Log.d(TAG, "Custom model inside the ICHelper class: $filename")
            }else{
                Log.d(TAG, "filename is null")
            }
        }catch (e: Exception){
            Log.d(TAG, "Error inside ICHelper: $e")
        }
        Log.d(TAG, "custom file name (test log): $filename")

        val modelName =
            when (currentModel)
            {
                MODEL_MOBILENETV1 -> "ic_mobilenetv1.tflite"
                MODEL_EFFICIENTNETV0 -> "ic-lite0.tflite"
                MODEL_EFFICIENTNETV1 -> "ic-lite1.tflite"
                MODEL_EFFICIENTNETV2 -> "ic-lite2.tflite"
                custom -> filename
                else -> filename
            }

        try
        {

            if (currentModel == custom)
            {
                imageClassifier = ImageClassifier.createFromFileAndOptions(file, optionsBuilder.build())
            }
            else
            {
                imageClassifier = ImageClassifier.createFromFileAndOptions(context, modelName,optionsBuilder.build())
            }

        } catch (e: IllegalStateException) {
            imageClassifierListener?.onError(
                "Image classifier failed to initialize"
            )

            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    fun classify(image: Bitmap, rotation: Int)
    {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor = ImageProcessor.Builder().build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        imageClassifierListener?.onResults(
            results,
            inferenceTime
        )
    }

    private fun getOrientationFromRotation(rotation: Int) : ImageProcessingOptions.Orientation
    {
        when (rotation) {
            Surface.ROTATION_270 ->
                return ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 ->
                return ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 ->
                return ImageProcessingOptions.Orientation.TOP_LEFT
            else ->
                return ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object
    {

        const val TAG: String = "ICHelper"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTNETV0 = 1
        const val MODEL_EFFICIENTNETV1 = 2
        const val MODEL_EFFICIENTNETV2 = 3
        const val custom = 4

    }
}
