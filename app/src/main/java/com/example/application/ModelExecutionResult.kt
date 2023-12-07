package com.example.application

import android.graphics.Bitmap

class ModelExecutionResult(
    val bitmapResult: Bitmap,
    val executionLog: String,
    val itemsFound: Map<String, Int>,
){

    companion object {
        const val TAG = "ModelExecution"
    }

}