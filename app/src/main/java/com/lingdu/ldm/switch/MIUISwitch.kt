package com.lingdu.ldm.switch

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.CompoundButton
import kotlin.math.abs
import kotlin.math.max


class MIUISwitch @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CompoundButton(context, attrs) {


    // 0f=关 1f=开
    private var progress = if (isChecked) 1f else 0f

    // 按住时圆球缩放（初始=1f，保持原大小）
    private var thumbScale = 0.85f

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downProgress = 0f
    private var dragging = false

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val trackRect = RectF()

    // 默认尺寸（接近你原来 Switch 的视觉宽高）
    private val trackWidthDp = 53f
    private val trackHeightDp = 30f
    private val paddingDp = 2f

    // 颜色
    private val isNight: Boolean by lazy {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        uiMode == Configuration.UI_MODE_NIGHT_YES
    }

    // 关的时候：浅色=E6E6E6，深色=505050
    private val colorOff: Int by lazy {
        if (isNight) 0xFF505050.toInt() else 0xFFE6E6E6.toInt()
    }

    // 开的时候：你没说改，就保持原来的蓝色
    private val colorOn: Int = 0xFF2B7CFF.toInt()

    // 滑块：浅色=FFFFFF，深色=EEEEEE
    private val colorThumb: Int by lazy {
        if (isNight) 0xFFEEEEEE.toInt() else 0xFFFFFFFF.toInt()
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private val progressAnim = ValueAnimator().apply {
        duration = 180
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    private val scaleAnim = ValueAnimator().apply {
        duration = 120
        addUpdateListener {
            thumbScale = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        //isClickable = true // 这一行也可以删
        isFocusable = false
    }


    // ✅ 外层任何时候想把 clickable 关掉，都无效
    override fun setClickable(clickable: Boolean) {
        super.setClickable(true)
    }


    // ✅ 同理，外层想关 focus 也行，但不会影响触摸
    override fun setFocusable(focusable: Boolean) {
        super.setFocusable(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val hSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val wSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        val w = if (wSpecMode == MeasureSpec.EXACTLY) wSpecSize else dp(trackWidthDp).toInt()
        val h = if (hSpecMode == MeasureSpec.EXACTLY) hSpecSize else dp(trackHeightDp).toInt()

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val pad = dp(paddingDp)
        val trackRadius = h / 2f

        // ✅ 圆球初始半径跟随轨道高度（保持原本大小）
        val baseThumbR = trackRadius - pad
        val thumbR = baseThumbR * thumbScale

        // track
        trackRect.set(0f, 0f, w, h)
        trackPaint.color = lerpColor(colorOff, colorOn, progress)
        canvas.drawRoundRect(trackRect, trackRadius, trackRadius, trackPaint)

        // thumb center X
        val minX = pad + baseThumbR
        val maxX = w - pad - baseThumbR
        val cx = minX + (maxX - minX) * progress
        val cy = h / 2f

        thumbPaint.color = colorThumb
        canvas.drawCircle(cx, cy, thumbR, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                var p = parent
                while (p != null) {
                    p.requestDisallowInterceptTouchEvent(true)
                    p = p.parent
                }

                downX = event.x
                downProgress = progress
                dragging = false
                animateThumbScale(0.95f)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                if (!dragging && abs(dx) > touchSlop) dragging = true
                if (dragging) {
                    val p = calcProgressFromDx(dx)
                    setProgressInternal(p)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                animateThumbScale(0.85f)

                val targetChecked = if (dragging) {
                    progress > 0.5f
                } else {
                    !isChecked
                }

                // 只改一次
                setChecked(targetChecked)
                dragging = false

                var p = parent
                while (p != null) {
                    p.requestDisallowInterceptTouchEvent(false)
                    p = p.parent
                }

                // 不要 performClick()，否则又 toggle 一次
                // 如果你想要无障碍点击事件，可以：
                // sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED)

                return true
            }
        }
        return false
    }


    override fun setChecked(checked: Boolean) {
        val changed = checked != isChecked
        super.setChecked(checked)

        val target = if (checked) 1f else 0f
        if (changed && !dragging) {
            animateProgressTo(target)
        } else {
            progress = target
            invalidate()
        }
    }

    private fun setProgressInternal(p: Float) {
        progress = p.coerceIn(0f, 1f)
        invalidate()
    }

    private fun calcProgressFromDx(dx: Float): Float {
        val pad = dp(paddingDp)
        val r = (height / 2f) - pad
        val w = width.toFloat()
        val range = max(1f, w - 2 * pad - 2 * r)
        return (downProgress + dx / range).coerceIn(0f, 1f)
    }

    private fun animateProgressTo(target: Float) {
        progressAnim.cancel()
        progressAnim.setFloatValues(progress, target)
        progressAnim.start()
    }

    private fun animateThumbScale(target: Float) {
        scaleAnim.cancel()
        scaleAnim.setFloatValues(thumbScale, target)
        scaleAnim.start()
    }

    private fun lerpColor(c1: Int, c2: Int, t: Float): Int {
        val a1 = (c1 ushr 24) and 0xFF
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val a2 = (c2 ushr 24) and 0xFF
        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        val a = (a1 + (a2 - a1) * t).toInt()
        val r = (r1 + (r2 - r1) * t).toInt()
        val g = (g1 + (g2 - g1) * t).toInt()
        val b = (b1 + (b2 - b1) * t).toInt()

        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
