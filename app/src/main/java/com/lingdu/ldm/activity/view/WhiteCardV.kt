package com.lingdu.ldm.activity.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.lingdu.ldm.activity.fragment.MIUIFragment

/**
 * 包一层“纯白卡片但不可按压/不可点击”的容器
 * 内部控件照常可交互（如果有 SeekBar/Switch 之类）
 */
class WhiteCardV(private val inner: BaseView) : BaseView {

    override fun getType(): BaseView = this

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        return inner.create(context, callBacks).also { v ->
            // ✅ 强制白卡 + 禁用按压
            v.setTag(MIUIFragment.TAG_FORCE_CARD, true)
            v.setTag(MIUIFragment.TAG_DISABLE_CARD_PRESS, true)
        }
    }

    override fun onDraw(thiz: MIUIFragment, group: LinearLayout, view: View) {
        // 让 inner 自己去 layout（TextSummaryV / SeekBarV 等原逻辑不丢）
        inner.onDraw(thiz, group, view)
    }
}
