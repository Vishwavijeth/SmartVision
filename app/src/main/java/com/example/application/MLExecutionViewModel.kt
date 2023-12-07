package com.example.application

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService

private val TAG = "OCR"

class MLExecutionViewModel: ViewModel() {
    private val _resultingBitmap = MutableLiveData<ModelExecutionResult>()

    val resultingBitmap: LiveData<ModelExecutionResult>
        get() = _resultingBitmap

    private val viewModelJob = Job()

    fun processFramesForOCR(
        frames: List<Bitmap>,
        ocrModel: OCRModelExecutor?,
        inferenceExecutor: ExecutorService
    ) {
        try {

            val targetWidth = 320
            val targetHeight = 320

            for (frame in frames) {
                inferenceExecutor.execute {
                    try {
                        if (ocrModel != null) {

                            val grayscaleFrame = convertToGrayscale(frame)

                            val resizedFrame = Bitmap.createScaledBitmap(grayscaleFrame, targetWidth, targetHeight, false)
                            val result = ocrModel.execute(resizedFrame)
                            _resultingBitmap.postValue(result as ModelExecutionResult?)
                            // Process the OCR result here (e.g., update UI, store results, etc.)
                        } else {
                            Log.d(TAG, "OCRModelExecutor is null")
                        }
                    } catch (e: Exception) {
                        Log.d(TAG, "Error executing OCRModelExecutor: ${e.message}")
                        // Handle the exception as needed
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frames: ${e.message}")
            // Handle the exception as needed
        }
    }

    private fun convertToGrayscale(inputBitmap: Bitmap): Bitmap {
        val outputBitmap = Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f) // Set saturation to 0 to convert to grayscale
        val colorMatrixColorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixColorFilter
        canvas.drawBitmap(inputBitmap, 0f, 0f, paint)
        return outputBitmap
    }
}