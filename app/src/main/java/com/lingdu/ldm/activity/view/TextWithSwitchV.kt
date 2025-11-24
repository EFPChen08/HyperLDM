package com.lingdu.ldm.activity.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.lingdu.ldm.activity.data.DataBinding
import com.lingdu.ldm.activity.data.LayoutPair
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.fragment.MIUIFragment

class TextWithSwitchV(
    private val textV: TextV,
    private val switchV: SwitchV,
    private val dataBindingRecv: DataBinding.Binding.Recv? = null
) : BaseView {

    override fun getType(): BaseView = this

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        textV.notShowMargins(true)
        val row = LinearContainerV(
            LinearContainerV.HORIZONTAL,
            arrayOf(
                LayoutPair(
                    textV.create(context, callBacks),
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).also { it.gravity = Gravity.CENTER_VERTICAL }
                ),
                LayoutPair(
                    switchV.create(context, callBacks),
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.gravity = Gravity.CENTER_VERTICAL }
                )
            ),
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                // 行内自己的上下间距，保留
                it.setMargins(0, dp2px(context, 15.75f), 0, dp2px(context, 15.75f))
            }
        ).create(context, callBacks)

        dataBindingRecv?.setView(row)
        return row
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDraw(thiz: MIUIFragment, group: LinearLayout, view: View) {
        thiz.apply {
            group.apply {
                // 把这一行真正的内容加进去
                addView(view)

                // 让整行可点击（按压效果 & 圆角背景由 MIUIFragment 的 wrapper 负责）
                isClickable = true
                isFocusable = false

                // 点击整行切换 switch
                setOnClickListener {
                    if (switchV.switch.isEnabled) {
                        switchV.click()
                        callBacks?.invoke()
                    }
                }

                // 不要再自己处理触摸事件/改背景了
                setOnTouchListener(null)
                // 不要设置 background，交给 MIUIFragment 统一设置圆角白底 + 按下灰
                // background = null  // 不写也行，外层 wrapper 会覆盖
            }
        }
    }
}
