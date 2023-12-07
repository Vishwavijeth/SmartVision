
package com.example.application

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.segmenter.ImageSegmenter
import org.tensorflow.lite.task.vision.segmenter.OutputType
import org.tensorflow.lite.task.vision.segmenter.Segmentation
import java.io.File

class ImageSegmentationHelper(
    var numThreads: Int = 2,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val imageSegmentationListener: SegmentationListener?
) {
    private var imageSegmenter: ImageSegmenter? = null
    private var filename: String? = null
    private var file: File? = null

    init {
        setupImageSegmenter()
    }

    fun clearImageSegmenter() {
        imageSegmenter = null
    }

    fun getData(value: File?){
        if (value != null){
            Log.d(ImageClassifierHelper.TAG, "model name received")
            //filename = value
            file = value
            Log.d(ImageClassifierHelper.TAG, "name: $filename")
        }else{
            Log.d(ImageClassifierHelper.TAG, "no model name received from the camera fragment")
        }
    }

    private fun setupImageSegmenter() {

        val optionsBuilder =
            ImageSegmenter.ImageSegmenterOptions.builder()

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageSegmentationListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        optionsBuilder.setOutputType(OutputType.CATEGORY_MASK)

        val modelName =
            when (currentModel) {
                currentModel -> "imageSegmentation.tflite"
                else -> "imageSegmentation.tflite"
            }

        try {
            imageSegmenter =
                ImageSegmenter.createFromFileAndOptions(
                    context,
                    modelName,
                    optionsBuilder.build()
                )
        } catch (e: IllegalStateException) {
            imageSegmentationListener?.onError(
                "Image segmentation failed to initialize. See error logs for details"
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun segment(image: Bitmap, imageRotation: Int) {

        if (imageSegmenter == null) {
            setupImageSegmenter()
        }

        var inferenceTime = SystemClock.uptimeMillis()

        val imageProcessor =
            ImageProcessor.Builder()
                .add(Rot90Op(-imageRotation / 90))
                .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val segmentResult = imageSegmenter?.segment(tensorImage)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime

        imageSegmentationListener?.onResults(
            segmentResult,
            inferenceTime,
            tensorImage.height,
            tensorImage.width
        )
    }

    interface SegmentationListener {
        fun onError(error: String)
        fun onResults(
            results: List<Segmentation>?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val customModel = 3
        //const val MODEL_DEEPLABV3 = "imageSegmentation.tflite"

    }
}