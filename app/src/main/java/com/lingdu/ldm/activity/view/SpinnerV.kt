package com.lingdu.ldm.activity.view

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.data.DataBinding
import com.lingdu.ldm.activity.data.LayoutPair
import com.lingdu.ldm.activity.data.MIUIPopupData
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.isRtl

/**
 * Spinner / 下拉框
 * @param currentValue current select value / 当前选中的值 (name)
 * @param dropDownWidth Spinner width / 下拉框宽度
 * @param dataBindingSend Data binding send / 数据绑定发送 (name)
 * @param dataBindingRecv Data binding recv / 数据绑定接收
 * @param data Spinner data / 下拉框数据
 */
class SpinnerV(
    var currentValue: String,
    val dropDownWidth: Float = 150F,
    val dataBindingSend: DataBinding.Binding.Send? = null,
    private val dataBindingRecv: DataBinding.Binding.Recv? = null,
    val data: SpinnerData.() -> Unit
) : BaseView {

    class SpinnerData {
        val arrayList: ArrayList<MIUIPopupData> = arrayListOf()

        fun add(name: String, dataBindingSend: DataBinding.Binding.Send? = null, callBacks: () -> Unit) {
            arrayList.add(MIUIPopupData(name, dataBindingSend, callBacks))
        }
    }

    private lateinit var context: Context

    lateinit var select: TextView

    override fun getType(): BaseView = this

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        this.context = context
        return LinearContainerV(
            LinearContainerV.HORIZONTAL,
            arrayOf(
                LayoutPair(
                    TextView(context).also {
                        it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        it.gravity = if (isRtl(context)) Gravity.LEFT else Gravity.RIGHT
                        it.text = currentValue
                        it.setTextColor(context.getColor(R.color.spinner))
                        select = it
                        it.width = dp2px(context, dropDownWidth)
                        if (isRtl(context))
                            it.setPadding(dp2px(context, 5f), 0, dp2px(context, 30f), 0)
                        else
                            it.setPadding(dp2px(context, 30f), 0, dp2px(context, 5f), 0)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            it.paint.typeface = Typeface.create(null, 400, false)
                        } else {
                            it.paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                        }
                        it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                ),
                LayoutPair(
                    ImageView(context).also {
                        it.background = context.getDrawable(R.drawable.ic_up_down)
                    },
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.gravity = Gravity.CENTER_VERTICAL }
                )
            ),
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS,
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).also { it.gravity = Gravity.CENTER_VERTICAL }
        ).create(context, callBacks).also {
            dataBindingRecv?.setView(it)
        }
    }
}
