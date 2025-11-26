package com.lingdu.ldm.activity.view

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.lingdu.ldm.activity.MIUIActivity

/**
 * 全局大标题：
 * 把一个 TextView 加到 MIUIActivity 的内容容器上（frameLayout 里面），
 * 位置等同于你原来 Page 里那个 Text(padding = Padding(0, 280, 0, 65)).
 */
class GlobalHeader(
    private val activity: MIUIActivity
) {

    val textView: TextView

    init {
        // MIUIActivity 里：root.addView(frameLayout); root.addView(topBarContainer)
        // 所以 android.R.id.content 的第 0 个 child 就是页面容器 frameLayout
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
        val pageContainer = (root.getChildAt(0) as? ViewGroup) ?: root

        textView = TextView(activity).apply {
            // 初始文本用当前 Activity 的 title
            text = activity.title ?: ""
            textSize = 34f

            // 对应你原来 Typeface.create(null, 350, false) 那种粗一点的字
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                typeface = Typeface.create(Typeface.DEFAULT, 350, false)
            } else {
                typeface = Typeface.DEFAULT_BOLD
            }

            setTextColor(Color.BLACK)
            gravity = Gravity.LEFT

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // 关键：顶端偏移，跟 Text(paddingTop = 280) 一样，用像素值 280
                gravity = Gravity.TOP or Gravity.LEFT
                topMargin = 280   // 注意：不是 dp，是 px，跟 TextV 里 padding 一样用原始值
                leftMargin = 65
            }
        }

        // 把这个大标题加到页面内容容器里，这样它不会跟 fragment 一起被 replace 掉
        pageContainer.addView(textView)
    }

    /** 改标题文字 */
    fun setTitle(text: CharSequence?) {
        textView.text = text ?: ""
    }
}
