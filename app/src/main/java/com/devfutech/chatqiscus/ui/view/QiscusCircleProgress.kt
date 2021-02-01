package com.devfutech.chatqiscus.ui.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.devfutech.chatqiscus.R
import com.devfutech.chatqiscus.utils.QiscusConverterUtil
import kotlin.math.acos

class QiscusCircleProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {
    private val defaultFinishedColor = Color.rgb(66, 145, 241)
    private val defaultUnfinishedColor = Color.rgb(204, 204, 204)
    private val defaultTextColor = Color.WHITE
    private val defaultMax = 100
    private val defaultTextSize: Float = QiscusConverterUtil.sp2px(resources, 18f)
    private val minSize: Int = QiscusConverterUtil.dp2px(resources, 100f).toInt()
    private var textPaint: Paint? = null
    private val rectF = RectF()
    private var textSize = 0f
    private var textColor = 0
    var progress = 0
        set(progress) {
            field = progress
            if (this.progress > max) {
                field %= max
            }
            invalidate()
        }
    private var max = 0
        set(max) {
            if (max > 0) {
                field = max
                invalidate()
            }
        }
    private var finishedColor = 0
    private var unfinishedColor = 0
    private var prefixText: String? = ""
    private var suffixText: String? = "%"
    private val paint = Paint()
    private fun initByAttributes(attributes: TypedArray) {
        finishedColor = attributes.getColor(
            R.styleable.QiscusCircleProgress_qcircle_finished_color,
            defaultFinishedColor
        )
        unfinishedColor = attributes.getColor(
            R.styleable.QiscusCircleProgress_qcircle_unfinished_color,
            defaultUnfinishedColor
        )
        textColor = attributes.getColor(
            R.styleable.QiscusCircleProgress_qcircle_text_color,
            defaultTextColor
        )
        textSize = attributes.getDimension(
            R.styleable.QiscusCircleProgress_qcircle_text_size,
            defaultTextSize
        )
        max = attributes.getInt(R.styleable.QiscusCircleProgress_qcircle_max, defaultMax)
        progress = attributes.getInt(R.styleable.QiscusCircleProgress_qcircle_progress, 0)
        if (attributes.getString(R.styleable.QiscusCircleProgress_qcircle_prefix_text) != null) {
            setPrefixText(attributes.getString(R.styleable.QiscusCircleProgress_qcircle_prefix_text))
        }
        if (attributes.getString(R.styleable.QiscusCircleProgress_qcircle_suffix_text) != null) {
            setSuffixText(attributes.getString(R.styleable.QiscusCircleProgress_qcircle_suffix_text))
        }
    }

    private fun initPainters() {
        textPaint = TextPaint()
        (textPaint as TextPaint).color = textColor
        (textPaint as TextPaint).textSize = textSize
        (textPaint as TextPaint).isAntiAlias = true
        paint.isAntiAlias = true
    }

    override fun invalidate() {
        initPainters()
        super.invalidate()
    }

    private fun getTextSize(): Float {
        return textSize
    }

    private fun getTextColor(): Int {
        return textColor
    }

    private fun getFinishedColor(): Int {
        return finishedColor
    }

    private fun getUnfinishedColor(): Int {
        return unfinishedColor
    }

    private fun getPrefixText(): String? {
        return prefixText
    }

    private fun setPrefixText(prefixText: String?) {
        this.prefixText = prefixText
        this.invalidate()
    }

    private fun getSuffixText(): String? {
        return suffixText
    }

    private fun setSuffixText(suffixText: String?) {
        this.suffixText = suffixText
        this.invalidate()
    }

    private val drawText: String
        get() = getPrefixText() + progress + getSuffixText()

    override fun getSuggestedMinimumHeight(): Int {
        return minSize
    }

    override fun getSuggestedMinimumWidth(): Int {
        return minSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        rectF[0f, 0f, MeasureSpec.getSize(widthMeasureSpec).toFloat()] =
            MeasureSpec.getSize(heightMeasureSpec).toFloat()
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        val yHeight = progress / max.toFloat() * height
        val radius = width / 2f
        val angle = (acos(((radius - yHeight) / radius).toDouble()) * 180 / Math.PI).toFloat()
        val startAngle = 90 + angle
        val sweepAngle = 360 - angle * 2
        paint.color = getUnfinishedColor()
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
        canvas.save()
        canvas.rotate(180f, (width / 2).toFloat(), (height / 2).toFloat())
        paint.color = getFinishedColor()
        canvas.drawArc(rectF, 270 - angle, angle * 2, false, paint)
        canvas.restore()

        val text = drawText
        if (!TextUtils.isEmpty(text)) {
            val textHeight = textPaint!!.descent() + textPaint!!.ascent()
            canvas.drawText(
                text, (width - textPaint!!.measureText(text)) / 2.0f, (width - textHeight) / 2.0f,
                textPaint!!
            )
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putInt(INSTANCE_TEXT_COLOR, getTextColor())
        bundle.putFloat(INSTANCE_TEXT_SIZE, getTextSize())
        bundle.putInt(INSTANCE_FINISHED_STROKE_COLOR, getFinishedColor())
        bundle.putInt(INSTANCE_UNFINISHED_STROKE_COLOR, getUnfinishedColor())
        bundle.putInt(INSTANCE_MAX, max)
        bundle.putInt(INSTANCE_PROGRESS, progress)
        bundle.putString(INSTANCE_SUFFIX, getSuffixText())
        bundle.putString(INSTANCE_PREFIX, getPrefixText())
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            textColor = state.getInt(INSTANCE_TEXT_COLOR)
            textSize = state.getFloat(INSTANCE_TEXT_SIZE)
            finishedColor = state.getInt(INSTANCE_FINISHED_STROKE_COLOR)
            unfinishedColor = state.getInt(INSTANCE_UNFINISHED_STROKE_COLOR)
            initPainters()
            max = state.getInt(INSTANCE_MAX)
            progress = state.getInt(INSTANCE_PROGRESS)
            prefixText = state.getString(INSTANCE_PREFIX)
            suffixText = state.getString(INSTANCE_SUFFIX)
            super.onRestoreInstanceState(state.getParcelable(INSTANCE_STATE))
            return
        }
        super.onRestoreInstanceState(state)
    }

    companion object {
        private const val INSTANCE_STATE = "saved_instance"
        private const val INSTANCE_TEXT_COLOR = "text_color"
        private const val INSTANCE_TEXT_SIZE = "text_size"
        private const val INSTANCE_FINISHED_STROKE_COLOR = "finished_stroke_color"
        private const val INSTANCE_UNFINISHED_STROKE_COLOR = "unfinished_stroke_color"
        private const val INSTANCE_MAX = "max"
        private const val INSTANCE_PROGRESS = "progress"
        private const val INSTANCE_SUFFIX = "suffix"
        private const val INSTANCE_PREFIX = "prefix"
    }

    init {
        val attributes = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.QiscusCircleProgress,
            defStyleAttr,
            0
        )
        initByAttributes(attributes)
        attributes.recycle()
        initPainters()
    }
}
