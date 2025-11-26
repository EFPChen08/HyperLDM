package com.lingdu.ldm.activity.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.lingdu.ldm.activity.dp2px

@SuppressLint("ViewConstructor")
class GlobalTitle(context: Context) : TextView(context) {

    init {
        // 1. 设置字号
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 34f)

        // 2. 设置边距 (左, 上, 右, 下)
        // 100dp 约等于 280-300px，足够避开状态栏
        setPadding(dp2px(context, 22f), dp2px(context, 100f), 0, dp2px(context, 20f))

        // 3. 字体设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            typeface = Typeface.create(null as Typeface?, 350, false)
        } else {
            typeface = Typeface.DEFAULT_BOLD
        }

        // 4. 初始化颜色
        updateTextColor()

        // 5. 布局参数
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // 6. 关键设置
        //elevation = 20f // 提高海拔，保证在最上层

        // ❌ 调试模式：先设为红色背景，看看它到底在哪！
        // 如果你能看见红色条了，就把下面这行删掉或改成 Color.TRANSPARENT
        // setBackgroundColor(Color.parseColor("#33FF0000"))
        setBackgroundColor(Color.TRANSPARENT)

        isClickable = false
        isFocusable = false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateTextColor()
    }

    private fun updateTextColor() {
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        setTextColor(if (isNight) Color.WHITE else Color.BLACK)
    }

    fun updateTitle(newTitle: String) {
        text = newTitle
        alpha = 1f
        visibility = VISIBLE
        // ❌ 不要再调用 bringToFront()，否则会压住 blurView
    }

}