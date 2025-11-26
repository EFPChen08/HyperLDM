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
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.switch.MIUIProgBar
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
class PDialog(
    context: Context,
    private val newStyle: Boolean = true,
    val build: PDialog.() -> Unit
) : Dialog(context, R.style.CustomDialog) {

    // 上面：文字
    private val message by lazy {
        TextView(context).also { textView ->
            textView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart = dp2px(context, 20f)
                it.marginEnd = dp2px(context, 20f)
                it.topMargin = dp2px(context, 18f)
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            textView.setTextColor(context.getColor(R.color.whiteText))
            textView.gravity = Gravity.START
            textView.maxLines = 2
        }
    }

    // 下面：MIUIProgBar 圆角进度条
    private val progressBar by lazy {
        MIUIProgBar(context).also { bar ->
            bar.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart = dp2px(context, 20f)
                it.marginEnd = dp2px(context, 20f)
                it.topMargin = dp2px(context, 14f)
                it.bottomMargin = dp2px(context, 20f)
            }
            bar.minValue = 0
            bar.maxValue = 100
            bar.indeterminate = false   // 默认非循环
        }
    }

    // 根布局：垂直“文字 + 进度条”
    private val root = RelativeLayout(context).also { viewRoot ->
        viewRoot.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val content = LinearLayout(context).also { linearLayout ->
            linearLayout.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                dp2px(context, 100f)
            ).also {
                it.addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            linearLayout.orientation = LinearLayout.VERTICAL
            linearLayout.gravity = Gravity.CENTER_VERTICAL

            linearLayout.addView(message)
            linearLayout.addView(progressBar)
        }

        viewRoot.addView(content)
    }

    init {
        window?.setGravity(Gravity.BOTTOM)
        setContentView(root)

        window?.setBackgroundDrawable(GradientDrawable().apply {
            val radius = dp2px(context, 32f).toFloat()
            if (newStyle) {
                cornerRadius = radius
            } else {
                cornerRadii = floatArrayOf(
                    radius, radius,
                    radius, radius,
                    0f, 0f,
                    0f, 0f
                )
            }
            setColor(context.getColor(R.color.dialog_background))
        })
    }

    fun setMessage(text: CharSequence, isCenter: Boolean = false) {
        message.apply {
            this.text = text
            gravity = if (isCenter) Gravity.CENTER_HORIZONTAL else Gravity.START
        }
    }

    fun setMessage(textId: Int, isCenter: Boolean = false) {
        setMessage(context.getString(textId), isCenter)
    }

    fun setMax(max: Int) {
        progressBar.maxValue = max
    }

    fun setProgress(value: Int) {
        progressBar.indeterminate = false
        progressBar.setProgressPercent(value)
    }

    fun setIndeterminate(indeterminate: Boolean = true) {
        progressBar.indeterminate = indeterminate
    }

    override fun show() {
        build()

        window?.setWindowAnimations(R.style.DialogAnim)

        super.show()

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
    }
}
