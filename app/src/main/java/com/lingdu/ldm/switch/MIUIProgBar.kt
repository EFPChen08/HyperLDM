package com.lingdu.ldm.switch

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.dp2px
import kotlin.math.max

class MIUIProgBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnProgressChangeListener {
        fun onProgressChanged(value: Int, fromUser: Boolean)
    }

    var listener: OnProgressChangeListener? = null

    var minValue: Int = 0
        set(value) {
            field = value
            if (progress < field) progress = field
            invalidate()
        }

    var maxValue: Int = 100
        set(value) {
            field = max(1, value)
            if (progress > field) progress = field
            invalidate()
        }

    /** 普通模式下的进度：0~100 */
    var progress: Int = 0
        set(value) {
            val newValue = value.coerceIn(minValue, maxValue)
            if (field != newValue) {
                field = newValue
                if (!indeterminate) {
                    listener?.onProgressChanged(newValue, false)
                }
                invalidate()
            }
        }

    /** 是否为循环样式（不显示具体进度） */
    var indeterminate: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (field) {
                startIndeterminateAnim()
            } else {
                stopIndeterminateAnim()
                invalidate()
            }
        }

    // ===== 颜色 =====
    private val isNight = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    @ColorInt
    private val trackOffColor = if (isNight) 0xFF434343.toInt() else 0xFFE9E9E9.toInt()

    @ColorInt
    private val trackOnColor: Int =
        try { context.getColor(R.color.colorAccent) } catch (_: Throwable) { 0xFF3D7BFF.toInt() }

    // ===== 画笔 / 几何 =====
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val trackRect = RectF()

    // ✅ 轨道高度：20dp（圆角半径 = 10dp）
    private val trackHeightPx = dp2px(context, 6f).toFloat()

    // ===== 循环动画相关 =====
    private var indeterminateAnimator: ValueAnimator? = null
    private var indeterminateT: Float = 0f   // 0..1，表示滑动进度

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredH = trackHeightPx + paddingTop + paddingBottom
        val height = resolveSize(desiredH.toInt(), heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val width = when (widthMode) {
            MeasureSpec.EXACTLY, MeasureSpec.AT_MOST -> widthSize
            else -> dp2px(context, 220f)
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cy = h / 2f

        val left = paddingLeft.toFloat()
        val right = w - paddingRight.toFloat()
        val top = cy - trackHeightPx / 2f
        val bottom = cy + trackHeightPx / 2f
        trackRect.set(left, top, right, bottom)

        val radius = trackRect.height() / 2f   // ✅ 药丸形圆角

        // 1) 背景轨道
        trackPaint.color = trackOffColor
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        if (indeterminate) {
            drawIndeterminate(canvas, left, right, top, bottom, radius)
        } else {
            drawDeterminate(canvas, left, right, top, bottom, radius)
        }
    }

    // ===== 普通进度模式 =====
    private fun drawDeterminate(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        radius: Float
    ) {
        val denom = max(1, maxValue - minValue)
        val t = (progress - minValue).toFloat() / denom.toFloat()

        if (t <= 0f) return

        val progRight = left + (right - left) * t.coerceIn(0f, 1f)
        val progRect = RectF(left, top, progRight, bottom)
        progressPaint.color = trackOnColor
        canvas.drawRoundRect(progRect, radius, radius, progressPaint)
    }

    // ===== 循环进度模式：固定长度蓝条匀速从左滑到右，再从左重新进 =====
    private fun drawIndeterminate(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        bottom: Float,
        radius: Float
    ) {
        val width = right - left

        // 固定段宽：整条的 30%
        val segmentFrac = 0.3f
        val segmentWidth = width * segmentFrac

        // offset：[-segmentFrac, 1]，这样一开始在左侧之外，最后滑到最右侧外面
        val offset = -segmentFrac + (1f + segmentFrac) * indeterminateT  // 从 -0.3 ~ 1

        val segLeft = left + offset * width
        val segRight = segLeft + segmentWidth

        val clampedLeft = segLeft.coerceAtLeast(left)
        val clampedRight = segRight.coerceAtMost(right)

        if (clampedRight <= clampedLeft) return

        val segRect = RectF(clampedLeft, top, clampedRight, bottom)
        progressPaint.color = trackOnColor
        canvas.drawRoundRect(segRect, radius, radius, progressPaint)
    }

    // ===== 循环动画控制：匀速循环滑动 =====
    private fun startIndeterminateAnim() {
        if (indeterminateAnimator != null) return

        indeterminateAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200L                // 一圈 1.2s，可按需要调长/调短
            interpolator = LinearInterpolator() // 匀速，看起来最直观不抽象
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                indeterminateT = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopIndeterminateAnim() {
        indeterminateAnimator?.cancel()
        indeterminateAnimator = null
        indeterminateT = 0f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (indeterminate) startIndeterminateAnim()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopIndeterminateAnim()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView == this) {
            if (visibility == VISIBLE && indeterminate) {
                startIndeterminateAnim()
            } else {
                stopIndeterminateAnim()
            }
        }
    }

    /** 方便外部直接用 0~100 传进度 */
    fun setProgressPercent(value: Int) {
        progress = value
    }
}
