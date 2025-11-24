package com.lingdu.ldm.activity.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.lingdu.ldm.activity.MIUIActivity
import com.lingdu.ldm.activity.data.DataBinding
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.fragment.MIUIFragment
import com.lingdu.ldm.switch.MIUISeekBar

class MIUISeekBarV(
    val key: String,
    private val min: Int,
    private val max: Int,
    private val defaultProgress: Int,
    private val dataSend: DataBinding.Binding.Send? = null,
    private val dataBindingRecv: DataBinding.Binding.Recv? = null,
    private val onChange: ((Int) -> Unit)? = null
) : BaseView {

    override fun getType(): BaseView = this

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        val initProgress =
            if (MIUIActivity.safeSP.containsKey(key)) MIUIActivity.safeSP.getInt(key, defaultProgress)
            else {
                MIUIActivity.safeSP.putAny(key, defaultProgress)
                defaultProgress
            }

        return MIUISeekBar(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minValue = min
            maxValue = max
            progress = initProgress

            listener = object : MIUISeekBar.OnProgressChangeListener {
                override fun onProgressChanged(value: Int, fromUser: Boolean) {
                    callBacks?.invoke()
                    onChange?.invoke(value)
                    dataSend?.send(value)
                    MIUIActivity.safeSP.putAny(key, value)
                }
                override fun onStartTrackingTouch() {}
                override fun onStopTrackingTouch() {}
            }

            dataBindingRecv?.setView(this)
        }
    }

    override fun onDraw(thiz: MIUIFragment, group: LinearLayout, view: View) {
        group.addView(LinearLayout(thiz.context).apply {
            setPadding(
                dp2px(context, 0f),
                dp2px(context, 24f),
                dp2px(context, 0f),
                dp2px(context, 24f)
            )
            addView(
                view,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        })
    }
}
