package com.lingdu.ldm.activity.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.lingdu.ldm.activity.fragment.MIUIFragment

/**
 * 一张纯白大卡片（不可按）里塞多个 BaseView
 * - 外观：走 MIUIFragment 的白卡/分组逻辑
 * - 行为：整卡不可点、无按压态
 * - 内部控件照常可交互
 */
class WhiteCardContainerV(
    private val children: List<BaseView>
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
        }
    }

    override fun onDraw(thiz: MIUIFragment, group: LinearLayout, view: View) {
        val root = view as LinearLayout
        root.removeAllViews()

        // 把子项按它们自己的 onDraw 塞进 root
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

        group.addView(root)

        // ✅ 塞一个隐藏 BlockMiUIButton
        // MIUIFragment 会把这张卡判定为 "buttonCard"
        // => 有白卡背景，但 wrapper 不可点、无按压遮罩
        group.addView(BlockMiUIButton(thiz.context).apply {
            visibility = View.GONE
            isClickable = false
            isFocusable = false
            layoutParams = LinearLayout.LayoutParams(0, 0)
            text = ""
        })
    }
}
