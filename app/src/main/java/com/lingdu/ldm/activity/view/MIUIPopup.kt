package com.lingdu.ldm.activity.view

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.data.MIUIPopupData
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.isRtl
import kotlin.math.max
import android.animation.ValueAnimator
import android.animation.AnimatorListenerAdapter
import android.content.res.Configuration

class MIUIPopup(
    private val context: Context,
    view: View,
    private val currentValue: String,
    private val dropDownWidth: Float,
    private val dataBacks: (String) -> Unit,
    private val arrayList: ArrayList<MIUIPopupData>
) : ListPopupWindow(context) {

    val isNight = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    // ===== 全局遮罩（只负责视觉） =====
    private var scrimView: View? = null
    private val SCRIM_COLOR = if (isNight) Color.parseColor("#25FFFFFF") else Color.parseColor("#4D000000") // 30% 黑

    private val SCRIM_COLOR2 = Color.parseColor("#9C000000") // 30% 黑


    // ===== 动画/圆角裁切 =====
    private var isAnimatingDismiss = false
    private var dismissingWithAnim = false
    private var listBg: GradientDrawable? = null
    private var listBg2: GradientDrawable? = null

    private val targetRadiusPx by lazy { dp2px(context, 20f).toFloat() }
    private var clipRadiusPx: Float = 0f

    private val TAG_PRESS_ANIM = 0x55667788


    /** 生成矩形 drawable */
    private fun createRectangleDrawable(
        color: Int,
        strokeColor: Int = 0,
        strokeWidth: Int,
        radius: FloatArray?
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            if (strokeColor != 0) setStroke(strokeWidth, strokeColor)
            if (radius != null && radius.size == 4) {
                cornerRadii = floatArrayOf(
                    radius[0], radius[0], radius[1], radius[1],
                    radius[2], radius[2], radius[3], radius[3]
                )
            }
        }
    }

    /** 生成 selector */
    private fun createStateListDrawable(
        pressedDrawable: GradientDrawable,
        normalDrawable: GradientDrawable
    ): StateListDrawable {
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), pressedDrawable)
            addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
            addState(intArrayOf(-android.R.attr.state_focused), normalDrawable)
        }
    }

    private fun getInternalPopupWindow(): PopupWindow? {
        return try {
            val f = ListPopupWindow::class.java.getDeclaredField("mPopup")
            f.isAccessible = true
            f.get(this) as? PopupWindow
        } catch (_: Throwable) { null }
    }

    private fun configPopupWindowForAnimDismiss() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val pw = getInternalPopupWindow() ?: return
        pw.elevation = 0f
        pw.isFocusable = true
        pw.isOutsideTouchable = true
        pw.setBackgroundDrawable(ColorDrawable(0x00000000))

        pw.setTouchInterceptor { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_OUTSIDE -> { dismiss(); true }
                MotionEvent.ACTION_DOWN -> {
                    val x = ev.x
                    val y = ev.y
                    val inside = x >= 0 && x < v.width && y >= 0 && y < v.height
                    if (!inside) { dismiss(); true } else false
                }
                else -> false
            }
        }
    }

    private fun attachScrim() {
        val act = (anchorView?.context as? Activity) ?: return
        val decor = act.window.decorView as? ViewGroup ?: return
        if (scrimView != null) return

        scrimView = View(act).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(if(isNight) SCRIM_COLOR2 else SCRIM_COLOR)
            isClickable = false
            isFocusable = false
        }
        decor.addView(scrimView)
    }

    private fun detachScrim() {
        val act = (anchorView?.context as? Activity) ?: return
        val decor = act.window.decorView as? ViewGroup ?: return
        scrimView?.let { decor.removeView(it) }
        scrimView = null
    }

    /** ✅ 向上找最近的“卡片 wrapper”（前景就是你的黑遮罩 overlay） */
    private fun findCardHost(): View? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return anchorView
        var v: View? = anchorView
        while (v != null) {
            val fg = v.foreground
            if (fg is GradientDrawable) return v
            v = (v.parent as? View)
        }
        return anchorView
    }

    /** ✅ Popup show：让宿主卡片遮罩直接拉满 */
    private fun holdHostPressed() {
        val host = findCardHost() ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val overlay = host.foreground as? GradientDrawable ?: return

        // ✅ 干掉宿主上任何还在跑的渐隐 / 渐显
        (host.getTag(TAG_PRESS_ANIM) as? ValueAnimator)?.cancel()
        host.setTag(TAG_PRESS_ANIM, null)

        // ✅ 遮罩拉满，保持“按下选中态”
        overlay.alpha = 255
        host.invalidate()
    }


    /** ✅ Popup dismiss：宿主卡片遮罩渐隐 */
    private fun releaseHostPressedWithFade() {
        val host = findCardHost() ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val overlay = host.foreground as? GradientDrawable ?: return

        // ✅ 先取消旧的（防连点叠加）
        (host.getTag(TAG_PRESS_ANIM) as? ValueAnimator)?.cancel()

        val anim = ValueAnimator.ofInt(overlay.alpha, 0).apply {
            duration = 180
            interpolator = AccelerateInterpolator()
            addUpdateListener { overlay.alpha = it.animatedValue as Int }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    host.setTag(TAG_PRESS_ANIM, null)
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    host.setTag(TAG_PRESS_ANIM, null)
                }
            })
        }

        // ✅ 把这次 dismiss 渐隐记到 tag 里，下一次 show 能 cancel 掉
        host.setTag(TAG_PRESS_ANIM, anim)
        anim.start()
    }


    private fun playShowMorph(lv: View) {
        val bg = listBg ?: return
        val w = lv.width.toFloat()
        val h = lv.height.toFloat()

        val startRadius = max(w, h)
        val endRadius = targetRadiusPx
        val startScale = 0.06f

        lv.alpha = 0f
        lv.scaleX = startScale
        lv.scaleY = startScale
        lv.pivotY = 0f
        lv.pivotX = if (isRtl(context)) w else 0f

        bg.cornerRadius = startRadius
        clipRadiusPx = startRadius
        if (Build.VERSION.SDK_INT >= 21) lv.invalidateOutline()

        val interp = OvershootInterpolator(1.1f)

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = interp
            addUpdateListener {
                val t = it.animatedValue as Float

                val s = startScale + (1f - startScale) * t
                lv.scaleX = s
                lv.scaleY = s
                lv.alpha = t

                val r = startRadius + (endRadius - startRadius) * t
                bg.cornerRadius = r
                clipRadiusPx = r
                if (Build.VERSION.SDK_INT >= 21) lv.invalidateOutline()

                lv.invalidate()
            }
            start()
        }
    }

    private fun playDismissMorph(lv: View, onEnd: () -> Unit) {
        val bg = listBg ?: run { onEnd(); return }
        val w = lv.width.toFloat()
        val h = lv.height.toFloat()

        val startRadius = max(w, h)
        val endRadius = targetRadiusPx
        val startScale = 0.06f

        val interp = OvershootInterpolator(1.1f)

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 320
            interpolator = interp
            addUpdateListener {
                val t = it.animatedValue as Float

                val s = startScale + (1f - startScale) * t
                lv.scaleX = s
                lv.scaleY = s
                lv.alpha = t

                val r = startRadius + (endRadius - startRadius) * t
                bg.cornerRadius = r
                clipRadiusPx = r
                if (Build.VERSION.SDK_INT >= 21) lv.invalidateOutline()

                lv.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    init {
        setBackgroundDrawable(ColorDrawable(0x00000000))
        width = dp2px(context, dropDownWidth)
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        isModal = true
        anchorView = view

        setAdapter(object : BaseAdapter() {
            override fun getCount(): Int = arrayList.size
            override fun getItem(p0: Int): Any = arrayList[p0]
            override fun getItemId(p0: Int): Long = p0.toLong()

            override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
                val thisText = arrayList[p0].getName()
                val dataBinding = arrayList[p0].getDataBindingSend()

                return (p1 as? LinearLayout ?: LinearLayout(context)).apply {
                    val radius = floatArrayOf(0f, 0f, 0f, 0f)

                    val pressedDrawable = createRectangleDrawable(
                        context.getColor(R.color.popup_background_click),
                        0, 0, radius
                    )
                    val normalDrawable = createRectangleDrawable(
                        0x00000000, 0, 0, radius
                    )
                    background = createStateListDrawable(pressedDrawable, normalDrawable)

                    removeAllViews()

                    addView((object : TextView(context) {
                        init { isFocusable = true }
                        override fun isFocused(): Boolean = true
                    }).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            dp2px(context, dropDownWidth - 35),
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        if (isRtl(context))
                            setPadding(dp2px(context, 10f), dp2px(context, 25f), dp2px(context, 25f), dp2px(context, 25f))
                        else
                            setPadding(dp2px(context, 25f), dp2px(context, 25f), dp2px(context, 10f), dp2px(context, 25f))

                        isSingleLine = true
                        text = thisText
                        ellipsize = TextUtils.TruncateAt.MARQUEE
                        paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)

                        if (currentValue == thisText)
                            setTextColor(context.getColor(R.color.popup_select_text))
                        else
                            setTextColor(context.getColor(R.color.whiteText))
                    })

                    addView(ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).also {
                            it.gravity = Gravity.CENTER_VERTICAL
                            if (isRtl(context))
                                it.setMargins(dp2px(context, 20f), 0, 0, 0)
                            else
                                it.setMargins(0, 0, dp2px(context, 20f), 0)
                        }
                        background = context.getDrawable(R.drawable.ic_popup_select)
                        if (currentValue != thisText) visibility = View.GONE
                    })

                    setOnClickListener {
                        (it as ViewGroup).apply {
                            for (i in 0 until childCount) {
                                val mView = getChildAt(i)
                                if (mView is TextView) {
                                    val v = mView.text.toString()
                                    dataBacks(v)
                                    dataBinding?.send(v)
                                    break
                                }
                            }
                        }
                        arrayList[p0].getCallBacks()()
                        dismiss()
                    }
                }
            }
        })

        setOnDismissListener {
            if (!dismissingWithAnim) detachScrim()
        }
    }

    override fun show() {
        // ✅ 先把宿主卡片遮罩拉满保持按下态
        holdHostPressed()

        attachScrim()
        super.show()
        configPopupWindowForAnimDismiss()

        val lv = listView ?: return


        listBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = targetRadiusPx
            setColor(Color.WHITE)
        }

        listBg2 = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = targetRadiusPx
            setColor(Color.parseColor("#2C2C2C"))
        }

        lv.background = if(isNight) listBg2 else listBg

        if (Build.VERSION.SDK_INT >= 21) {
            lv.clipToOutline = true
            lv.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, clipRadiusPx)
                }
            }
            lv.cacheColorHint = Color.TRANSPARENT
        }

        lv.clipChildren = true
        lv.clipToPadding = true

        lv.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        lv.isScrollbarFadingEnabled = false
        lv.isVerticalScrollBarEnabled = false

        val hideRunnable = Runnable {
            lv.isVerticalScrollBarEnabled = false
            lv.invalidate()
        }
        fun cancelHide() = lv.removeCallbacks(hideRunnable)
        fun scheduleHide() {
            cancelHide()
            lv.postDelayed(hideRunnable, 150)
        }

        lv.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    cancelHide()
                    if (!lv.isVerticalScrollBarEnabled) {
                        lv.isVerticalScrollBarEnabled = true
                        lv.invalidate()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> scheduleHide()
            }
            false
        }

        lv.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, state: Int) {
                when (state) {
                    AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
                        cancelHide()
                        view.isVerticalScrollBarEnabled = true
                        view.invalidate()
                    }
                    AbsListView.OnScrollListener.SCROLL_STATE_FLING,
                    AbsListView.OnScrollListener.SCROLL_STATE_IDLE -> scheduleHide()
                }
            }
            override fun onScroll(view: AbsListView?, first: Int, count: Int, total: Int) {}
        })

        lv.divider = null
        lv.dividerHeight = 0

        lv.isFocusableInTouchMode = true
        lv.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                true
            } else false
        }

        var started = false
        fun startShowAnimOnce() {
            if (started) return
            started = true
            playShowMorph(lv)
        }

// ✅ 保险 1：PreDraw（正常情况）
        lv.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                lv.viewTreeObserver.removeOnPreDrawListener(this)
                startShowAnimOnce()
                return true
            }
        })

// ✅ 保险 2：极快二连点时 PreDraw 可能错过，用 post 兜底
        lv.post { startShowAnimOnce() }

    }

    override fun dismiss() {
        if (isAnimatingDismiss) return
        val lv = listView

        // ✅ 收起时宿主卡片遮罩渐隐
        releaseHostPressedWithFade()

        if (lv == null) {
            dismissingWithAnim = false
            super.dismiss()
            detachScrim()
            listBg = null
            return
        }

        dismissingWithAnim = true
        isAnimatingDismiss = true

        playDismissMorph(lv) {
            isAnimatingDismiss = false
            try {
                super.dismiss()
            } finally {
                dismissingWithAnim = false
                detachScrim()
                listBg = null
            }
        }
    }
}
