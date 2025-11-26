package com.lingdu.ldm.activity.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.lingdu.ldm.activity.fragment.MIUIFragment

class CardContainerV(
    private val children: List<BaseView>,
    private val onClick: (() -> Unit)? = null
) : BaseView {

    private var storedCallBacks: (() -> Unit)? = null

    override fun getType(): BaseView = this

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        storedCallBacks = callBacks
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // ❌ 删除这里的 setOnClickListener
            // ❌ 删除这里的 setTag (不需要了)
            // ❌ 删除这里的 isClickable = true (让它保持不可点击，让事件穿透给外层)
        }
    }

    override fun onDraw(thiz: MIUIFragment, group: LinearLayout, view: View) {
        val root = view as LinearLayout
        root.removeAllViews()

        for (child in children) {
            val childContent = child.create(thiz.context, storedCallBacks)
            val childWrapper = LinearLayout(thiz.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            child.onDraw(thiz, childWrapper, childContent)
            root.addView(childWrapper)
        }

        // 把内容加入到 wrapper (group)
        group.addView(root)

        // ✅ 核心修改：把点击事件绑定给 group (也就是 MIUIFragment 的 wrapper)
        if (onClick != null) {
            group.setOnClickListener { onClick.invoke() }

            // 因为给 group 设置了点击监听，MIUIFragment 会自动识别它为“可交互卡片”
            // 从而自动加上：1. 白色背景 2. 按压动画
        } else {
            // 如果不可点击，才需要塞入隐藏按钮来骗出背景
            group.addView(BlockMiUIButton(thiz.context).apply {
                visibility = View.GONE
                isClickable = false
                isFocusable = false
                layoutParams = LinearLayout.LayoutParams(0, 0)
                text = ""
            })
        }
    }
}