package com.lingdu.ldm.activity.view

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlin.math.max   // 如果用到了 max 记得加这个


/**
 * 底部切换菜单：
 *  - 上图标，下文字
 *  - 背景：毛玻璃 + 半透明白色
 *  - 选中：纯黑；未选中：#999999
 */
class BottomPageMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    data class Item(
        val id: Int,
        val iconRes: Int,
        val title: String
    )

    // 内容区高度：60dp
    private val barHeightPx = dp2px(60f)

    // 额外增高：20dp（实际控件总高度 = 60 + 20）
    private val extraBottomSpacePx = dp2px(20f)

    private val blurView: BlurView
    private val container: LinearLayout
    private val items = mutableListOf<Item>()
    private val itemViews = mutableListOf<View>()

    private var selectedIndex = -1
    private var listener: ((index: Int, item: Item) -> Unit)? = null

    private val topDivider: View   // 新增：顶部细线

    val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    init {
        clipToPadding = false
        clipChildren = false
        setPadding(0, 0, 0, 0)

        // 1. 背景毛玻璃层
        blurView = createBlurBackground()
        addView(blurView)

        // 2. 菜单项容器
        container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                barHeightPx
            ).apply {
                gravity = Gravity.BOTTOM
                bottomMargin = extraBottomSpacePx
            }

            // 给内容往下挪一点，避免被顶部细线压住
            val lineHeight = max(1, dp2px(0.5f))
            setPadding(0, lineHeight, 0, 0)
        }
        addView(container)

        // 3. 顶部淡淡的线（颜色 232,232,232）
        val lineHeight = max(1, dp2px(0.5f)) // 至少 1px
        topDivider = View(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                lineHeight
            ).apply {
                gravity = Gravity.TOP
            }
            if(isNight) setBackgroundColor(Color.rgb(16, 16, 16)) else setBackgroundColor(Color.rgb(232, 232, 232))
        }
        // 最后 add，保证在最上层
        addView(topDivider)

        elevation = dp2px(4f).toFloat()
    }

    /** 使用 BlurView 做毛玻璃（依旧铺满整个 View） */
    private fun createBlurBackground(): BlurView {
        val activity = context as? Activity
            ?: throw IllegalStateException("BottomPageMenuView 只能在 Activity 里使用")

        val radius = dp2px(24f).toFloat()

        return BlurView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )

            val rootView = activity.window
                .decorView
                .findViewById<ViewGroup>(android.R.id.content)
            val windowBg = activity.window.decorView.background
                ?: ColorDrawable(Color.TRANSPARENT)

            setupWith(rootView)
                .setFrameClearDrawable(windowBg)
                .setBlurAlgorithm(RenderScriptBlur(context))
                .setBlurRadius(22f)
                .setBlurAutoUpdate(true)
                .setHasFixedTransformationMatrix(true)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                // 你想加圆角再改这里，目前全是直角
                cornerRadii = floatArrayOf(
                    0f, 0f,
                    0f, 0f,
                    0f, 0f,
                    0f, 0f
                )
                if(isNight) setColor(Color.argb(230, 0, 0, 0)) else setColor(Color.argb(230, 255, 255, 255))
            }
        }
    }

    /** 整体高度 = 内容 60dp + 额外 20dp（完全填到屏幕底，包括手势条后面） */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = barHeightPx + extraBottomSpacePx
        val fixedHeight = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, fixedHeight)
    }

    // ================== 对外 API ==================

    fun setItems(list: List<Item>, defaultIndex: Int = 0) {
        items.clear()
        items.addAll(list)
        container.removeAllViews()
        itemViews.clear()

        list.forEachIndexed { index, item ->
            val v = createItemView(item)
            container.addView(v)
            itemViews.add(v)

            v.setOnClickListener {
                select(index, fromUser = true)
            }
        }

        select(defaultIndex.coerceIn(list.indices), fromUser = false)
    }

    fun setOnItemSelectedListener(l: ((index: Int, item: Item) -> Unit)?) {
        listener = l
    }

    fun setSelectedIndex(index: Int) {
        select(index, fromUser = false)
    }

    fun getBarTotalHeight(): Int = barHeightPx + extraBottomSpacePx

    // ================== 内部实现 ==================

    private data class ItemHolder(
        val icon: ImageView,
        val label: TextView
    )

    private fun createItemView(item: Item): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val iconSize = dp2px(24f)
        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setImageResource(item.iconRes)
        }

        val label = TextView(context).apply {
            text = item.title
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp2px(2f), 0, 0)
        }

        layout.addView(icon)
        layout.addView(label)

        layout.tag = ItemHolder(icon, label)

        return layout
    }

    private fun select(index: Int, fromUser: Boolean) {
        if (index !in items.indices) return
        if (index == selectedIndex) return

        selectedIndex = index

        for (i in itemViews.indices) {
            val holder = itemViews[i].tag as? ItemHolder ?: continue
            val color = if (i == selectedIndex) if(isNight) Color.parseColor("#ffffff") else Color.parseColor("#000000") else if(isNight) Color.parseColor("#808080") else Color.parseColor("#999999")
            holder.label.setTextColor(color)
            holder.icon.setColorFilter(color)
        }

        if (fromUser) {
            listener?.invoke(selectedIndex, items[selectedIndex])
        }
    }

    private fun dp2px(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}
