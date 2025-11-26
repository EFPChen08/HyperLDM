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
import com.lingdu.ldm.MainActivity
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.isRtl
import com.lingdu.ldm.activity.data.MIUIPopupData
import com.lingdu.ldm.activity.view.MIUIPopup

/**
 * 右上角的“更多”按钮，单独封装成一个类
 */
class MoreButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ImageView(context, attrs) {

    private var overlayAlpha = 0
    private var overlayAnim: ValueAnimator? = null

    init {
        // 布局参数：父容器是 FrameLayout(android.R.id.content)
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { lp ->
            lp.gravity = Gravity.RIGHT
            if (isRtl(context)) {
                lp.setMargins(
                    dp2px(context, 25f),
                    0,
                    0,
                    0
                )
            } else {
                lp.setMargins(
                    dp2px(context, 5f),
                    dp2px(context, 55f),
                    dp2px(context, 25f),
                    0
                )
            }
        }

        // 图标资源
        val raw = AppCompatResources.getDrawable(context, R.drawable.abc_ic_menu_overflow_material)!!
        val bg = DrawableCompat.wrap(raw).mutate()
        background = bg

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // ===== 按压/松开 的覆盖效果 =====
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

        // 点击事件：弹出 MIUIPopup 菜单
        setOnClickListener {
            val list = arrayListOf(
                MIUIPopupData(
                    "设置",
                    null
                ) {
                    // 点击「设置」
                    MainActivity.showToast("打开设置")
                    // TODO: 打开设置页面
                },
                MIUIPopupData(
                    "关于",
                    null
                ) {
                    // 点击「关于」
                    MainActivity.showToast("打开关于")
                    // TODO: 打开关于页面
                }
            )

            MIUIPopup(
                context = context,
                view = this,
                currentValue = "",      // 不需要高亮
                dropDownWidth = 200f,
                dataBacks = { /* 普通菜单不需要回传选中值 */ },
                arrayList = list
            ).show()
        }

        isClickable = true
        isFocusable = true
        visibility = View.VISIBLE
    }
}
