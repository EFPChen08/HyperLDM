@file:Suppress("FunctionName")

package com.lingdu.ldm.activity.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.dp2px

/**
 * NewDialog 里的 Button 拆出来的独立按钮
 * 颜色/背景/行为 与 Dialog 版本保持一致
 *
 * margin 改为调用时手动设置：
 * BlockMiUIButton(context).Margin(0f,10f,0f,10f)
 */
class BlockMiUIButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle
) : Button(context, attrs, defStyleAttr) {

    /** 是否是取消样式（左按钮灰） */
    var cancelStyle: Boolean = false
        set(value) {
            field = value
            updateStyle()
        }

    /** 最终回调（和 NewDialog.Finally 一样） */
    private var finallyCallBacks: ((View) -> Unit)? = null

    /** 用户设置的点击回调（保持 setOnClickListener 行为一致） */
    private var userClickListener: OnClickListener? = null

    // margin（dp）默认全 0，由调用方决定
    private var marginLeftDp = 0f
    private var marginTopDp = 0f
    private var marginRightDp = 0f
    private var marginBottomDp = 0f

    init {
        // layoutParams 和 NewDialog 一致（但 margin 不再写死）
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp2px(context, 48f),
            1f
        ).also {
            it.gravity = Gravity.CENTER
            applyMarginsTo(it)
        }

        setPadding(0, 0, 0, 0)
        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 17.5f)

        // 去掉按压抬起动画（跟 Dialog 一样）
        stateListAnimator = null

        updateStyle()

        // 绑定统一点击分发（和 Dialog Button() 一样）
        super.setOnClickListener {
            userClickListener?.onClick(it)
            finallyCallBacks?.invoke(it)
        }
    }

    /** 让 setEnabled 时自动刷新颜色（Dialog 里是靠初始化设置一次，我们这里做成动态） */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        updateStyle()
    }

    /** 拦截用户 setOnClickListener，保证回调顺序一致 */
    override fun setOnClickListener(l: OnClickListener?) {
        userClickListener = l
    }

    /** Dialog 同款 Finally */
    fun Finally(callBacks: (View) -> Unit) {
        finallyCallBacks = callBacks
    }

    private fun updateStyle() {
        // textColor 逻辑完全照 NewDialog
        setTextColor(
            context.getColor(
                if (!isEnabled) {
                    R.color.disable_button_text
                } else {
                    if (cancelStyle) R.color.LButtonText else R.color.RButtonText
                }
            )
        )

        // background 逻辑完全照 NewDialog
        background = context.getDrawable(
            if (cancelStyle) R.drawable.l_button_background else R.drawable.r_button_background
        )
    }

    /** 方便你像 DSL 一样设置 */
    fun Style(cancel: Boolean) = apply { cancelStyle = cancel }

    /**
     * 调用时手动输入 margin（单位 dp）
     * 例如：.Margin(0f, 10f, 0f, 10f)
     */
    fun Margin(
        leftDp: Float = marginLeftDp,
        topDp: Float = marginTopDp,
        rightDp: Float = marginRightDp,
        bottomDp: Float = marginBottomDp
    ) = apply {
        marginLeftDp = leftDp
        marginTopDp = topDp
        marginRightDp = rightDp
        marginBottomDp = bottomDp
        updateMargins()
    }

    private fun updateMargins() {
        val lp = (layoutParams as? LinearLayout.LayoutParams)
            ?: LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp2px(context, 48f),
                1f
            ).also { it.gravity = Gravity.CENTER }

        applyMarginsTo(lp)
        layoutParams = lp
    }

    private fun applyMarginsTo(lp: LinearLayout.LayoutParams) {
        lp.setMargins(
            dp2px(context, marginLeftDp),
            dp2px(context, marginTopDp),
            dp2px(context, marginRightDp),
            dp2px(context, marginBottomDp)
        )
    }
}
