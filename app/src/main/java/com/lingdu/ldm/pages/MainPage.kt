package com.lingdu.ldm

import android.graphics.Typeface
import android.view.View
import android.widget.Switch
import androidx.compose.ui.window.DialogWindowProvider
import com.lingdu.ldm.MainActivity.Companion.showToast
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.MIUIActivity
import com.lingdu.ldm.activity.annotation.BMMainPage
import com.lingdu.ldm.activity.data.BasePage
import com.lingdu.ldm.activity.data.Padding
import com.lingdu.ldm.activity.view.SpinnerV
import com.lingdu.ldm.activity.view.SwitchV
import com.lingdu.ldm.activity.view.TextSummaryV
import com.lingdu.ldm.activity.view.*
import com.lingdu.ldm.dialog.MIUIDialog
import com.lingdu.ldm.dialog.NewDialog
import com.lingdu.ldm.activity.view.TextSummaryWithArrowV
import com.lingdu.ldm.activity.fragment.MIUIFragment
import com.lingdu.ldm.activity.view.MIUIEditText
import com.lingdu.ldm.activity.view.TitleTextV
import com.lingdu.ldm.activity.view.BaseView
import com.lingdu.ldm.activity.view.BlockMiUIButton
import android.widget.LinearLayout
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.data.CardScope

@BMMainPage("Home")
class MainPage : BasePage() {
    override fun onCreate() {

        setTitle("设置")

        Text(
            text = "设置",
            textSize = 34f, // 字号，可选
            padding = Padding(0,280 ,0,110),
            typeface = Typeface.create(null, 350, false)
        )

        val okBtn = BlockMiUIButton(activity).apply {
            text = "弹Dialog"
            cancelStyle = false
            isEnabled = true
            setAllCaps(false)
            Margin(0f, 20f, 0f, 12f)
            setOnClickListener {
                MIUIDialog(activity) {
                    setTitle("这是一个信息框")
                    setMessage("信息框有很多功能，比如\n可以装逼，就像这样")
                    setLButton("取消") {
                        dismiss()
                    }
                    setRButton("确定") {
                        dismiss()
                    }
                }.show()
            }
            Finally { /* 全局最终回调，可选 */ }
        }

        val cancelBtn = BlockMiUIButton(activity).apply {
            text = "再弹出一个Toast"
            setAllCaps(false);
            cancelStyle = true
            Margin(0f, 12f, 0f, 20f)
            setOnClickListener { showToast(
                "这是一个吐司")
            }
        }

        CustomView(okBtn)
        CustomView(cancelBtn)

        TextSummaryWithArrowV(TextSummaryV("showTest2", onClickListener = {
            MIUIFragment("test2")
        }))
        TextSummaryWithArrowV(TextSummaryV("showAsyncTest", onClickListener = {
            MIUIFragment("async")
        }))
        TextSummaryWithArrowV(TextSummaryV("showTest2", onClickListener = {
            NewDialog(activity) {
                setTitle("Test")
                setMessage("TestMessage")
                Button("1") {
                    dismiss()
                }
                Button("2") {
                    dismiss()
                }
                Button("3") {
                    dismiss()
                }
                Button("4", cancelStyle = true) {
                    dismiss()
                }
            }.show()
        }))
        TextSummaryWithArrowV(TextSummaryV("showDialog", onClickListener = {
            MIUIDialog(activity) {
                setTitle("Test")
                setMessage("TestMessage")
                setEditText("", "test")
                setLButton("Cancel") {
                    dismiss()
                }
                setRButton("OK") {
                    dismiss()
                }
            }.show()
        }))
        TextSummary("TextSummary", tips="This is a TextSummary")
        TextSummaryWithArrowV(TextSummaryV("test", tips = "summary", onClickListener = {}))
        TextSummaryWithSwitch(TextSummaryV("test", tips = "summary"), SwitchV("test12312312"))
        TextWithSwitch(TextV("test"), SwitchV("test"))
        TextWithSpinner(TextV("Spinner"), SpinnerV("test") {
            add("test") { showToast("select test") }
            add("test1") {
                showToast("select test1")
                MIUIDialog(activity) {
                    setTitle("Test")
                    setMessage("TestMessage")
                    setLButton("cancel") {
                        dismiss()
                    }
                    setRButton("ok") {
                        dismiss()
                    }
                }.show()
            }
            add("test2") { showToast("select test2") }
            add("test3") { showToast("select test3") }
            add("test4") { showToast("select test4") }
            add("test5") { showToast("select test5") }
            add("test6") { showToast("select test6") }
            add("test7") { showToast("select test7") }
            add("test8") { showToast("select test8") }
            add("test9") { showToast("select test9") }
            add("test10") { showToast("select test10") }
            add("test11") { showToast("select test11") }
            add("test12") { showToast("select test12") }
            add("test13") { showToast("select test13") }
            add("test14") { showToast("select test14") }
        })
        TextSummaryWithSpinner(TextSummaryV("Spinner", tips = "Summary"), SpinnerV("test12312323123123123123123") {
            add("test12312323123123123123123") { showToast("select test") }
            add("test1") { showToast("select test1") }
            add("test2") { showToast("select test2") }
            add("test3") { showToast("select test3") }
        })
        Line()
        TitleText("Title")
        TextSummaryWithArrowV(TextSummaryV("test", tips = "summary"))
        Text("SeekbarWithText")

        WhiteCard{
            add(MIUISeekBarV("seekbar", 0, 100, 0))
        }

        Line()
        TitleText("DataBinding")
        val binding = GetDataBinding({ MIUIActivity.safeSP.getBoolean("binding", false) }) { view, flags, data ->
            when (flags) {
                1 -> (view as Switch).isEnabled = data as Boolean
                2 -> view.visibility = if (data as Boolean) View.VISIBLE else View.GONE
            }
        }
        TextWithSwitch(TextV("data-binding"), SwitchV("binding", dataBindingSend = binding.bindingSend))
        TextSummaryWithArrowV(TextSummaryV("test"), dataBindingRecv = binding.getRecv(2))

        // 页面底部留 40dp 空白
        CustomView(View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp2px(activity, 40f)
            )
        })

    }
}