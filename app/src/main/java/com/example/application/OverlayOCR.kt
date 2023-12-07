package com.example.application

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class OverlayOCR(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var boundingBoxInfoList: ArrayList<BoundingBoxInfo> = ArrayList()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        for (boundingBoxInfo in boundingBoxInfoList) {
            val left = boundingBoxInfo.left * scaleFactor
            val top = boundingBoxInfo.top * scaleFactor
            val right = boundingBoxInfo.right * scaleFactor
            val bottom = boundingBoxInfo.bottom * scaleFactor

            // Draw bounding box around detected texts
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected texts
            val drawableText =
                "Confidence: ${String.format("%.2f", boundingBoxInfo.confidence)}"

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )

            // Draw text for detected text
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }
    }

    fun setBoundingBoxInfo(boundingBoxes: List<BoundingBoxInfo>, scaleFactor: Float) {
        //this.boundingBoxInfoList = boundingBoxes
        this.scaleFactor = scaleFactor
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}

data class BoundingBoxInfo(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float
)
