/*
 * BlockMIUI
 * …原版权信息保持不变…
 */

package com.lingdu.ldm.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.dp2px
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class LDialog(
    context: Context,
    private val newStyle: Boolean = true,
    val build: LDialog.() -> Unit
) : Dialog(context, R.style.CustomDialog) {

    // 左边：加载动画图标
    private val loadingView by lazy {
        ImageView(context).also { iv ->
            val size = dp2px(context, 32f)
            iv.layoutParams = LinearLayout.LayoutParams(size, size)
            iv.setImageResource(R.drawable.abc_loading)   // ✅ 这里用你新的矢量加载图
        }
    }

    // 右边：文字
    private val message by lazy {
        TextView(context).also { textView ->
            textView.layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).also {
                it.marginStart = dp2px(context, 16f)
                it.marginEnd = dp2px(context, 16f)
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            textView.setTextColor(context.getColor(R.color.whiteText))
            textView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            textView.maxLines = 2
        }
    }

    // 根布局：高度 100dp，横向 左动画 + 右文字
    private val root = RelativeLayout(context).also { viewRoot ->
        viewRoot.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val content = LinearLayout(context).also { linearLayout ->
            linearLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                dp2px(context, 100f)          // ✅ 内容高度 100dp
            ).also {
                it.addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            linearLayout.orientation = LinearLayout.HORIZONTAL
            linearLayout.gravity = Gravity.CENTER_VERTICAL
            linearLayout.setPadding(
                dp2px(context, 20f),
                dp2px(context, 20f),
                dp2px(context, 20f),
                dp2px(context, 20f)
            )

            linearLayout.addView(loadingView)
            linearLayout.addView(message)
        }

        viewRoot.addView(content)
    }

    init {
        // 你原来是贴底部，如果想居中可以改成 Gravity.CENTER
        window?.setGravity(Gravity.BOTTOM)
        setContentView(root)

        // 保留你原来的圆角 + 背景色逻辑
        window?.setBackgroundDrawable(GradientDrawable().apply {
            val radius = dp2px(context, 32f).toFloat()
            if (newStyle) {
                cornerRadius = radius
            } else {
                cornerRadii = floatArrayOf(
                    radius, radius,  // 左上
                    radius, radius,  // 右上
                    0f, 0f,          // 右下
                    0f, 0f           // 左下
                )
            }
            setColor(context.getColor(R.color.dialog_background))
        })
    }

    /** 设置右侧文字（字符串） */
    fun setMessage(text: CharSequence, isCenter: Boolean = false) {
        message.apply {
            this.text = text
            gravity = if (isCenter) {
                Gravity.CENTER_VERTICAL or Gravity.CENTER_HORIZONTAL
            } else {
                Gravity.CENTER_VERTICAL or Gravity.START
            }
        }
    }

    /** 设置右侧文字（资源 id） */
    fun setMessage(textId: Int, isCenter: Boolean = false) {
        setMessage(context.getString(textId), isCenter)
    }

    override fun show() {
        // 先让外部通过 build 配置文字、是否可取消等
        build()

        window?.setWindowAnimations(R.style.DialogAnim)

        super.show()

        // 调整宽度 & 位置
        val layoutParams = window!!.attributes
        layoutParams.dimAmount = 0.3F
        val resources = context.resources
        val dm: DisplayMetrics = resources.displayMetrics
        val width = dm.widthPixels
        if (newStyle) {
            layoutParams.width = (width * 0.94f).roundToInt()
            layoutParams.y = (width * 0.03f).roundToInt()
        } else {
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        }
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        window!!.attributes = layoutParams

        // 开始循环旋转动画
        loadingView.startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.loading_rotate)
        )
    }

    override fun dismiss() {
        // 关闭时停止动画，防止泄露
        loadingView.clearAnimation()
        super.dismiss()
    }
}
