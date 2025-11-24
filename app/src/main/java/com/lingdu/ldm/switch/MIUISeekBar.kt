package com.lingdu.ldm.switch

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.dp2px
import kotlin.math.abs
import kotlin.math.max

class MIUISeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface OnProgressChangeListener {
        fun onProgressChanged(value: Int, fromUser: Boolean)
        fun onStartTrackingTouch()
        fun onStopTrackingTouch()
    }

    var listener: OnProgressChangeListener? = null

    var minValue: Int = 0
        set(value) { field = value; invalidate() }

    var maxValue: Int = 100
        set(value) { field = max(1, value); invalidate() }

    var progress: Int = 0
        set(value) {
            field = value.coerceIn(minValue, maxValue)
            invalidate()
        }

    // ✅ MIUI 蓝（你可换成 switch 的那支 color）
    @ColorInt var trackOnColor: Int =
        try { context.getColor(R.color.colorAccent) } catch (_:Throwable){ 0xFF3D7BFF.toInt() }

    val isNight = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    @ColorInt val trackOffColor = if (isNight) 0xFF434343.toInt() else 0xFFE9E9E9.toInt()
    @ColorInt var thumbColor: Int = 0xFFFFFFFF.toInt()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp2px(context, 4f).toFloat()
    }

    private val trackRect = RectF()

    private val trackHeightPx = dp2px(context, 28f).toFloat()

    // ===== thumb 缩放动画 =====
    private val baseThumbRadiusPx = dp2px(context, 13f).toFloat()
    private var thumbScale = 0.85f
    private val pressedScale = 0.95f  // 按下放大一点点
    private var scaleAnim: ValueAnimator? = null

    // ===== 点击/拖动行为控制 =====
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var isDragging = false
    private var pendingJump = false
    private var jumpAnim: ValueAnimator? = null

    private var downX = 0f
    private var lastMoveX = 0f

    private val normalScale = 0.85f
    private val releaseScale = 0.90f


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

        val radius = trackRect.height() / 2f

        // 1) 灰底轨道
        trackPaint.color = trackOffColor
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint)

        // t = 0..1
        val t = if (maxValue == minValue) 0f
        else (progress - minValue).toFloat() / (maxValue - minValue).toFloat()

        // 2) thumb 始终在轨道内
        val minCX = trackRect.left + radius
        val maxCX = trackRect.right - radius
        val thumbCx = (minCX + (maxCX - minCX) * t).coerceIn(minCX, maxCX)

        // 3) 蓝色进度只在 t>0 时画（最低进度只有蓝圈圆球）
        if (t > 0.0001f) {
            val fillRadius = baseThumbRadiusPx
            val fillEnd = (thumbCx + fillRadius).coerceAtMost(trackRect.right)
            val progRect = RectF(trackRect.left, trackRect.top, fillEnd, trackRect.bottom)

            progressPaint.color = trackOnColor
            canvas.drawRoundRect(progRect, radius, radius, progressPaint)
        }



        // 4) 画 thumb（白心 + MIUI 蓝圈）
        val rThumb = baseThumbRadiusPx * thumbScale
        thumbPaint.color = thumbColor
        thumbStrokePaint.color = trackOnColor
        canvas.drawCircle(thumbCx, cy, rThumb, thumbPaint)
        canvas.drawCircle(thumbCx, cy, rThumb, thumbStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        lastMoveX = x

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                parent?.requestDisallowInterceptTouchEvent(true)

                // 按下缩放
                startThumbScale(true)

                // 如果按在 thumb 附近 → 立即拖动
                val onThumb = isTouchOnThumb(x, event.y)
                if (onThumb) {
                    isDragging = true
                    pendingJump = false
                    listener?.onStartTrackingTouch()
                } else {
                    // 按在轨道上 → 先不跳
                    isDragging = false
                    pendingJump = true
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (pendingJump) {
                    // 手指开始移动 → 先动画滑到手指位置再跟随
                    if (abs(x - downX) > touchSlop) {
                        pendingJump = false
                        startJumpToX(x) {
                            isDragging = true
                            listener?.onStartTrackingTouch()
                            // jump 结束后立刻对齐当前手指（防止落后）
                            updateProgressByX(lastMoveX, true)
                        }
                    }
                    return true
                }

                if (isDragging) {
                    // 正常拖动跟随
                    updateProgressByX(x, true)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 松开缩放恢复
                startThumbScale(false)

                if (pendingJump) {
                    // 这是一次“轻点轨道” → 抬手才滑过去
                    pendingJump = false
                    startJumpToX(x, onEnd = null)
                } else {
                    // 拖动结束
                    if (isDragging) listener?.onStopTrackingTouch()
                }

                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ---------- 计算/设置进度 ----------

    private fun updateProgressByX(x: Float, fromUser: Boolean) {
        val v = calcProgressForX(x)
        if (v != progress) {
            progress = v
            listener?.onProgressChanged(v, fromUser)
        }
    }

    private fun calcProgressForX(x: Float): Int {
        val radius = trackHeightPx / 2f
        val left = paddingLeft.toFloat() + radius
        val right = width.toFloat() - paddingRight.toFloat() - radius
        val clampedX = x.coerceIn(left, right)
        val t = (clampedX - left) / max(1f, (right - left))
        return (minValue + t * (maxValue - minValue)).toInt()
    }

    // ---------- thumb 缩放动画（像 switch） ----------

    private fun startThumbScale(pressed: Boolean) {
        scaleAnim?.cancel()

        if (pressed) {
            // ✅ 按下：直接放大到 pressedScale
            scaleAnim = ValueAnimator.ofFloat(thumbScale, pressedScale).apply {
                duration = 160
                addUpdateListener {
                    thumbScale = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            // ✅ 松开：两段
            // 1) 先回到 releaseScale（略大一点）
            val first = ValueAnimator.ofFloat(thumbScale, releaseScale).apply {
                duration = 90
                addUpdateListener {
                    thumbScale = it.animatedValue as Float
                    invalidate()
                }
            }

            // 2) 再轻微回弹到 normalScale
            val second = ValueAnimator.ofFloat(releaseScale, normalScale).apply {
                duration = 140
                interpolator = android.view.animation.OvershootInterpolator(0.8f)
                addUpdateListener {
                    thumbScale = it.animatedValue as Float
                    invalidate()
                }
            }

            first.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    second.start()
                }
            })

            scaleAnim = first
            first.start()
        }
    }

    // ---------- 点击轨道时的 jump 动画 ----------

    private fun startJumpToX(targetX: Float, onEnd: (() -> Unit)?) {
        val start = progress
        val end = calcProgressForX(targetX)

        jumpAnim?.cancel()
        jumpAnim = ValueAnimator.ofInt(start, end).apply {
            duration = 180
            addUpdateListener {
                val v = it.animatedValue as Int
                progress = v
                listener?.onProgressChanged(v, true)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    jumpAnim = null
                    onEnd?.invoke()
                }
                override fun onAnimationCancel(animation: Animator) {
                    jumpAnim = null
                }
            })
            start()
        }
    }

    // ---------- 命中 thumb ----------

    private fun isTouchOnThumb(x: Float, y: Float): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        val cy = h / 2f

        val radiusTrack = trackHeightPx / 2f
        val minCX = paddingLeft.toFloat() + radiusTrack
        val maxCX = w - paddingRight.toFloat() - radiusTrack

        val t = if (maxValue == minValue) 0f
        else (progress - minValue).toFloat() / (maxValue - minValue).toFloat()
        val thumbCx = (minCX + (maxCX - minCX) * t).coerceIn(minCX, maxCX)

        val rHit = baseThumbRadiusPx * 1.8f
        return abs(x - thumbCx) <= rHit && abs(y - cy) <= rHit
    }
}
