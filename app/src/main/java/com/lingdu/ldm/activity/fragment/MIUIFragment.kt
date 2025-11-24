/*
 * BlockMIUI
 * Copyright (C) 2022 fkj@fkj233.cn
 * https://github.com/577fkj/BlockMIUI
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License v2.1
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by 577fkj.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * GNU Lesser General Public License v2.1 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License v2.1
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/577fkj/BlockMIUI/blob/main/LICENSE>.
 */

@file:Suppress("DEPRECATION", "DuplicatedCode")

package com.lingdu.ldm.activity.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.Fragment
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationSet
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.annotation.Keep
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.MIUIActivity
import com.lingdu.ldm.activity.data.AsyncInit
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.view.BaseView
import com.lingdu.ldm.activity.view.BlockMiUIButton
import com.lingdu.ldm.switch.MIUISwitch
import android.animation.ValueAnimator
import android.os.Build
import android.view.MotionEvent
import android.view.animation.AccelerateInterpolator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import kotlin.math.max
import android.content.res.Configuration
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContentProviderCompat.requireContext

@Suppress("MemberVisibilityCanBePrivate")
@SuppressLint("ValidFragment")
@Keep
class MIUIFragment() : Fragment() {

    private fun computeRadii(mode: CornerMode): FloatArray {
        val r = dp2px(context, RADIUS_DP).toFloat()
        return when (mode) {
            CornerMode.FULL -> floatArrayOf(r, r, r, r, r, r, r, r)
            CornerMode.TOP -> floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            CornerMode.MIDDLE -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
            CornerMode.BOTTOM -> floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
        }
    }

    private var key = ""
    private lateinit var scrollView: ScrollView
    private lateinit var itemView: LinearLayout
    val callBacks: (() -> Unit)? by lazy { MIUIActivity.activity.getAllCallBacks() }
    private val async: AsyncInit? by lazy {
        MIUIActivity.activity.getThisAsync(key.ifEmpty { MIUIActivity.activity.getTopPage() })
    }

    private fun Context.isNightMode(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return uiMode == Configuration.UI_MODE_NIGHT_YES
    }
    private var dialog: Dialog? = null
    val handler: Handler by lazy { Handler(Looper.getMainLooper()) }

    private var prevIsButtonCard = false
    private val SEAM_OVERLAP_DP = 1f   // 组内卡片向上重叠 1dp，消除凹缝

    /** foreground 遮罩 tag key（随便挑个不冲突的 int） */
    private val TAG_PRESS_OVERLAY = 0x10203040

    constructor(keys: String) : this() {
        key = keys
    }

    companion object {
        const val TAG_FORCE_CARD = 0x10203041          // 强制走白卡背景/分组
        const val TAG_DISABLE_CARD_PRESS = 0x10203042  // 白卡但禁用按压/点击
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        scrollView = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true

            val bgColor = if (context.isNightMode()) "#000000" else "#F7F7F7"
            setBackgroundColor(Color.parseColor(bgColor))

            addView(LinearLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor(bgColor))
                itemView = this
                if (async?.skipLoadItem != true) initData()
            })
        }
        async?.let { Thread { it.onInit(this) }.start() }
        return scrollView
    }

    // ====== 卡片背景/分组逻辑 ======
    private enum class CornerMode { FULL, TOP, MIDDLE, BOTTOM }
    private val RADIUS_DP = 18f

    private fun containsBlockMiUIButton(v: View?): Boolean {
        if (v == null) return false
        if (v is BlockMiUIButton) return true
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                if (containsBlockMiUIButton(v.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun makeCardDrawable(mode: CornerMode, colorStr: String): GradientDrawable {
        val radii = computeRadii(mode)
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setColor(Color.parseColor(colorStr))
        }
    }

    /** ✅ 专用遮罩（不靠 state list，alpha 我们手动动画） */
    private fun makePressOverlay(mode: CornerMode): GradientDrawable {
        val baseColor = if (isNightMode()) "#24FFFFFF" else "#1E000000"
        return makeCardDrawable(mode, baseColor).apply {
            alpha = 0
        }
    }


    private fun isNightMode(): Boolean {
        val uiMode = context?.resources?.configuration?.uiMode ?: 0
        val nightMask = uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightMask == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }


    private fun createPressForeground(mode: CornerMode): StateListDrawable {
        val pressedColor = if (isNightMode()) {
            "#24FFFFFF"   // ✅ 深色模式：14% 白遮罩
        } else {
            "#1E000000"   // 浅色模式：12% 黑遮罩（原来的）
        }

        return StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_pressed),
                makeCardDrawable(mode, pressedColor)
            )
            addState(
                intArrayOf(),
                makeCardDrawable(mode, "#00000000")
            )
        }
    }



    /**
     * ✅ 普通卡片 foreground = 黑遮罩 overlay
     * - mode 变化时只更新半径，不替换 drawable，保证动画稳定
     */
    private fun setCardBg(target: LinearLayout?, mode: CornerMode, isButton: Boolean) {
        target ?: return

        // 深浅色卡片底
        val cardColor = if (context.isNightMode()) "#242424" else "#FFFFFF"
        target.background = makeCardDrawable(mode, cardColor)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isButton) {
                target.foreground = null
                target.setTag(TAG_PRESS_OVERLAY, null)
            } else {
                val isSpinnerRow =
                    (target.childCount > 0 && target.getChildAt(0).getTag(R.id.miui_is_spinner_row) == true)

                if (isSpinnerRow) {
                    target.foreground = null
                    target.setTag(TAG_PRESS_OVERLAY, null)
                } else {
                    // ✅ 取旧 overlay（不替换，避免动画被重置）
                    val old = target.getTag(TAG_PRESS_OVERLAY) as? GradientDrawable

                    val baseColor = if (isNightMode()) "#24FFFFFF" else "#1E000000"
                    val radii = computeRadii(mode)

                    val overlay = if (old != null) {
                        // 只更新圆角和颜色，保留当前 alpha
                        old.cornerRadii = radii
                        old.setColor(Color.parseColor(baseColor))
                        old
                    } else {
                        makePressOverlay(mode).also {
                            target.setTag(TAG_PRESS_OVERLAY, it)
                        }
                    }

                    target.foreground = overlay
                }
            }
        }


    }


    private fun findScrollView(root: View): View? {
        if (root is androidx.recyclerview.widget.RecyclerView) return root
        if (root is androidx.core.widget.NestedScrollView) return root
        if (root is android.widget.ScrollView) return root

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val hit = findScrollView(root.getChildAt(i))
                if (hit != null) return hit
            }
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val act = activity as? MIUIActivity ?: return
        val thresholdPx = dp2px(act, 160f).toFloat()

        val scroll = findScrollView(view)

        when (scroll) {
            is androidx.recyclerview.widget.RecyclerView -> {
                scroll.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                    override fun onScrolled(rv: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                        val offset = rv.computeVerticalScrollOffset()
                        act.setTopBarProgress(offset / thresholdPx)
                    }
                })
            }
            is androidx.core.widget.NestedScrollView -> {
                scroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    act.setTopBarProgress(scrollY / thresholdPx)
                }
            }
            is android.widget.ScrollView -> {
                scroll.viewTreeObserver.addOnScrollChangedListener {
                    act.setTopBarProgress(scroll.scrollY / thresholdPx)
                }
            }
        }
    }

    private fun containsInteractive(v: View?): Boolean {
        if (v == null) return false
        if (v.isClickable || v.hasOnClickListeners() || v.isFocusable || v.isFocusableInTouchMode) return true

        if (v is Button || v is EditText || v is Switch || v is SeekBar || v is Spinner ||
            v is CompoundButton || v is CheckBox || v is RadioButton
        ) return true

        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                if (containsInteractive(v.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun stripInnerPressOverlays(v: View?) {
        if (v == null) return

        if (v is MIUISwitch || v is SeekBar || v is Spinner || v is EditText ||
            v is Button || v is CompoundButton
        ) return

        if (v is ViewGroup) {
            v.background = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) v.foreground = null
            for (i in 0 until v.childCount) {
                stripInnerPressOverlays(v.getChildAt(i))
            }
        }
    }

    private fun findMiuiSwitch(v: View?): MIUISwitch? {
        if (v == null) return null
        if (v is MIUISwitch) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val r = findMiuiSwitch(v.getChildAt(i))
                if (r != null) return r
            }
        }
        return null
    }

    // ====== 数据初始化 ======
    fun initData() {
        for (item: BaseView in MIUIActivity.activity.getThisItems(
            key.ifEmpty { MIUIActivity.activity.getTopPage() }
        )) {
            addItem(item)
        }
    }

    // ====== Loading Dialog ======
    fun showLoading() {
        handler.post {
            dialog = Dialog(MIUIActivity.context, R.style.Translucent_NoTitle).apply {
                setCancelable(false)
                setContentView(LinearLayout(context).apply {
                    layoutParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                    )
                    addView(ImageView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            dp2px(context, 60f),
                            dp2px(context, 60f)
                        ).also {
                            it.setMargins(
                                dp2px(context, 20f),
                                dp2px(context, 20f),
                                dp2px(context, 20f),
                                dp2px(context, 20f)
                            )
                        }
                        background = context.getDrawable(R.drawable.ic_loading)
                        startAnimation(AnimationSet(true).apply {
                            interpolator = LinearInterpolator()
                            addAnimation(
                                RotateAnimation(
                                    0f, +359f,
                                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                                    RotateAnimation.RELATIVE_TO_SELF, 0.5f
                                ).apply {
                                    repeatCount = -1
                                    startOffset = 0
                                    duration = 1000
                                }
                            )
                        })
                    })
                })
            }
            dialog?.show()
        }
    }

    fun closeLoading() {
        handler.post { dialog?.dismiss() }
    }

    // ====== 添加控件 + 分组卡片逻辑 ======
    private var cardStreak = 0
    private var prevCardWrapper: LinearLayout? = null

    private fun containsSeekBar(v: View?): Boolean {
        if (v == null) return false
        if (v is SeekBar) return true
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                if (containsSeekBar(v.getChildAt(i))) return true
            }
        }
        return false
    }

    private fun getExistingTouchListener(v: View): View.OnTouchListener? {
        return try {
            val m = View::class.java.getDeclaredMethod("getListenerInfo")
            m.isAccessible = true
            val li = m.invoke(v) ?: return null
            val liClz = Class.forName("android.view.View\$ListenerInfo")
            val f = liClz.getDeclaredField("mOnTouchListener")
            f.isAccessible = true
            f.get(li) as? View.OnTouchListener
        } catch (_: Throwable) {
            null
        }
    }


    /**
     * ✅ 普通卡片按压渐显/渐隐遮罩（稳定版）
     * 直接动画 overlay.alpha，不依赖 state 切换
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachFadePress(wrapper: LinearLayout) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val overlay = wrapper.getTag(TAG_PRESS_OVERLAY) as? GradientDrawable ?: return

        // ✅ 这里能稳定拿到 TextWithSpinnerV/其它 BaseView 先前写进去的 touch
        val oldTouch = getExistingTouchListener(wrapper)

        val TAG_PRESS_ANIM = 0x55667788

        fun cancelAnim() {
            (wrapper.getTag(TAG_PRESS_ANIM) as? ValueAnimator)?.cancel()
        }
        fun startAnim(anim: ValueAnimator) {
            wrapper.setTag(TAG_PRESS_ANIM, anim)
            anim.start()
        }

        wrapper.setOnTouchListener { v, ev ->
            // 1) 我们自己的遮罩动画
            cancelAnim()
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startAnim(ValueAnimator.ofInt(overlay.alpha, 255).apply {
                        duration = 120
                        interpolator = LinearInterpolator()
                        addUpdateListener { overlay.alpha = it.animatedValue as Int }
                    })
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    startAnim(ValueAnimator.ofInt(overlay.alpha, 0).apply {
                        duration = 180
                        interpolator = AccelerateInterpolator()
                        addUpdateListener { overlay.alpha = it.animatedValue as Int }
                    })
                }
            }

            // 2) 把事件交回旧 listener（它在 ACTION_UP 里 popup.show()）
            oldTouch?.onTouch(v, ev)

            // ✅ 永远不吃事件：让 click/子控件继续正常
            false
        }
    }



    @SuppressLint("ClickableViewAccessibility")
    fun addItem(item: BaseView) {
        handler.post {
            if (!isAdded || activity == null || view == null) return@post

            val ctx = context ?: return@post
            val content = item.create(ctx, callBacks)

            val wrapper = LinearLayout(MIUIActivity.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(dp2px(context, 30f), 0, dp2px(context, 30f), 0)
            }

            item.onDraw(this@MIUIFragment, wrapper, content)

            val forceCard =
                (wrapper.getTag(TAG_FORCE_CARD) == true) ||
                        (content.getTag(TAG_FORCE_CARD) == true)

            val disablePress =
                (wrapper.getTag(TAG_DISABLE_CARD_PRESS) == true) ||
                        (content.getTag(TAG_DISABLE_CARD_PRESS) == true)

            val isCard = forceCard || containsInteractive(wrapper) || containsInteractive(content)

            val lp = wrapper.layoutParams as LinearLayout.LayoutParams

            if (isCard) {
                lp.setMargins(dp2px(context, 20f), 0, dp2px(context, 20f), 0)
                lp.topMargin = if (cardStreak > 0) -dp2px(context, SEAM_OVERLAP_DP) else 0
                lp.bottomMargin = 0

                stripInnerPressOverlays(content)

                if (containsSeekBar(wrapper) || containsSeekBar(content)) {
                    wrapper.setPadding(
                        dp2px(context, 30f),
                        dp2px(context, 30f),
                        dp2px(context, 30f),
                        dp2px(context, 5f)
                    )
                } else {
                    wrapper.setPadding(dp2px(context, 30f), 0, dp2px(context, 30f), 0)
                }

                var isButtonCard = containsBlockMiUIButton(wrapper) || containsBlockMiUIButton(content)

                if (disablePress) isButtonCard = true


                // ====== 分组圆角 ======
                if (cardStreak == 0) {
                    setCardBg(wrapper, CornerMode.FULL, isButtonCard)
                    cardStreak = 1
                } else if (cardStreak == 1) {
                    setCardBg(prevCardWrapper, CornerMode.TOP, prevIsButtonCard)
                    setCardBg(wrapper, CornerMode.BOTTOM, isButtonCard)
                    cardStreak = 2
                } else {
                    setCardBg(prevCardWrapper, CornerMode.MIDDLE, prevIsButtonCard)
                    setCardBg(wrapper, CornerMode.BOTTOM, isButtonCard)
                    cardStreak++
                }

                prevCardWrapper = wrapper
                prevIsButtonCard = isButtonCard

                if (isButtonCard) {
                    wrapper.isClickable = false
                    wrapper.isFocusable = false
                    wrapper.setOnClickListener(null)
                    wrapper.setOnTouchListener(null)
                } else {
                    wrapper.isClickable = true
                    wrapper.isFocusable = false

                    val sw = findMiuiSwitch(wrapper) ?: findMiuiSwitch(content)
                    if (sw != null) {
                        wrapper.setOnClickListener {
                            if (sw.isEnabled) {
                                sw.isChecked = !sw.isChecked
                                callBacks?.invoke()
                            }
                        }

                        sw.isClickable = false
                        sw.isFocusable = false
                        sw.isFocusableInTouchMode = false
                        sw.background = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            sw.foreground = null
                        }
                        sw.isDuplicateParentStateEnabled = true
                    }

                    // ✅ 普通卡片按压动画
                    attachFadePress(wrapper)
                }

            } else {
                cardStreak = 0
                prevCardWrapper = null
                prevIsButtonCard = false
                lp.setMargins(0, 0, 0, 0)
                lp.topMargin = 0
                wrapper.background = null
                wrapper.isClickable = false
                wrapper.setOnTouchListener(null)
            }

            wrapper.layoutParams = lp
            itemView.addView(wrapper)
        }
    }

    fun clearAll() {
        handler.post { itemView.removeAllViews() }
    }
}
