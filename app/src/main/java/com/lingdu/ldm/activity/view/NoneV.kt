package com.lingdu.ldm.activity.view

import android.content.Context
import android.graphics.Color      // ★ 新增这行
import android.view.View
import android.widget.LinearLayout
import com.lingdu.ldm.activity.data.DataBinding
import com.lingdu.ldm.activity.dp2px

class NoneV(private val dataBindingRecv: DataBinding.Binding.Recv? = null): BaseView {

    override fun getType(): BaseView {
        return this
    }

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        return View(context).also {
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp2px(context, 0.9f)
            )
            layoutParams.setMargins(
                0,
                dp2px(context, 5f),
                0,
                dp2px(context, 5f)
            )
            it.layoutParams = layoutParams

            // 原来是用 R.color.line 画线
            // it.setBackgroundColor(context.resources.getColor(R.color.line, null))

            // ★ 改成完全透明
            it.setBackgroundColor(Color.TRANSPARENT)

            dataBindingRecv?.setView(it)
        }
    }
}
