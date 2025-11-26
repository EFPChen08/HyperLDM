package com.lingdu.ldm

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.WindowCompat
import com.lingdu.ldm.activity.MIUIActivity
import com.lingdu.ldm.activity.view.BackButtonView
import com.lingdu.ldm.SetPage
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.isRtl

class SetActivity : MIUIActivity() {

    // 移除 companion object，改为类成员变量
    // 直接使用 'this' 作为 Activity Context
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    // 将 backButton 定义为实例变量
    private val backButton by lazy { BackButtonView(this) }

    init {
        // 这里的 registerPage 应该是 MIUIActivity 的逻辑，用来加载 SetPage Fragment
        registerPage(SetPage::class.java , "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 先调用 super.onCreate，让父类 MIUIActivity 初始化它的布局和 Fragment
        super.onCreate(savedInstanceState)

        WindowCompat.enableEdgeToEdge(window)
        setSP(getPreferences(0))

        // 2. 将自定义按钮添加到 Activity 的根视图层级上
        // android.R.id.content 是 Activity 最底层的 FrameLayout
        val rootContainer = findViewById<ViewGroup>(android.R.id.content)

        // 确保按钮可见
        backButton.visibility = View.VISIBLE

        // 3. 将按钮添加到界面上
        rootContainer.addView(backButton)
    }

    // 简单的 Toast 工具方法（非静态）
    fun showToast(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show()
    }
}