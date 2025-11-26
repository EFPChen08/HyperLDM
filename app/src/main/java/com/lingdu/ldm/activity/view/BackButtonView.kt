package com.lingdu.ldm.activity.view

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.MIUIActivity
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.isRtl

/**
 * 左上角“返回”按钮，封装成独立 View
 */
class BackButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ImageView(context, attrs) {

    private var overlayAlpha = 0
    private var overlayAnim: ValueAnimator? = null

    init {
        // 布局参数：默认加在 android.R.id.content 这种 FrameLayout 上
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { lp ->
            lp.gravity = Gravity.LEFT
            if (isRtl(context)) {
                lp.setMargins(
                    dp2px(context, 25f),
                    0,
                    0,
                    0
                )
            } else {
                lp.setMargins(
                    dp2px(context, 25f),
                    dp2px(context, 55f),
                    dp2px(context, 5f),
                    0
                )
            }
        }

        // 图标
        val raw = AppCompatResources
            .getDrawable(context, R.drawable.abc_ic_ab_back_material)!!
        val bg = DrawableCompat.wrap(raw).mutate()
        background = bg

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // --- 按压效果 ---
        fun applyOverlay(alpha: Int) {
            overlayAlpha = alpha

            val isNightMode = (resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            val baseColor = if (isNightMode) Color.WHITE else Color.BLACK

            bg.setColorFilter(baseColor, PorterDuff.Mode.SRC_IN)
            bg.alpha = 255 - alpha

            invalidate()
        }

        fun animateOverlay(toAlpha: Int, durationMs: Long) {
            overlayAnim?.cancel()
            overlayAnim = ValueAnimator.ofInt(overlayAlpha, toAlpha).apply {
                duration = durationMs
                interpolator = AccelerateDecelerateInterpolator() as TimeInterpolator
                addUpdateListener { anim ->
                    applyOverlay(anim.animatedValue as Int)
                }
                start()
            }
        }

        applyOverlay(0)

        // 触摸反馈
        setOnTouchListener { v, event ->
            fun isInside(e: MotionEvent): Boolean {
                val x = e.x
                val y = e.y
                return x >= 0 && x < v.width && y >= 0 && y < v.height
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    animateOverlay(128, 120)
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isInside(event)) {
                        if (overlayAlpha == 0) animateOverlay(128, 80)
                    } else {
                        if (overlayAlpha != 0) animateOverlay(0, 120)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    animateOverlay(0, 180)
                    if (isInside(event)) v.performClick()
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                    animateOverlay(0, 180)
                    true
                }

                else -> false
            }
        }

        // 点击 → 调用当前 Activity 的返回
        setOnClickListener {
            (context as? MIUIActivity)?.onBackPressed()
        }

        isClickable = true
        isFocusable = true
        visibility = View.VISIBLE
    }
}
