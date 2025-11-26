package com.lingdu.ldm

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.WindowCompat
import com.lingdu.ldm.activity.MIUIActivity
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.isRtl
import com.lingdu.ldm.activity.view.MIUIPopup
import com.lingdu.ldm.activity.data.MIUIPopupData
import com.lingdu.ldm.activity.helper.PageSlideSwitcher
import com.lingdu.ldm.activity.view.MoreButtonView
import com.lingdu.ldm.activity.view.BottomPageMenuView
import com.lingdu.ldm.activity.view.GlobalHeader
import com.lingdu.ldm.activity.view.GlobalTitle

class MainActivity : MIUIActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var activity: MIUIActivity
        private val handler by lazy { Handler(Looper.getMainLooper()) }

        fun showToast(string: String) {
            Log.d("BlockMIUI", "Show Toast: $string")
            handler.post {
                Toast.makeText(activity, string, Toast.LENGTH_LONG).show()
            }
        }
    }

    // MainActivity 里加一个变量，记录当前选中的值（用于高亮）
    private var currentTabIndex: Int = 0
    private lateinit var pageSwitcher: PageSlideSwitcher

    private var popupCurrentValue: String = "选项一"
    private lateinit var bottomMenu: BottomPageMenuView
    lateinit var globalHeader: GlobalHeader
    private val moreButton by lazy { MoreButtonView(this) }

    init {
        activity = this
        registerPage(MainPage::class.java , "主页")
        registerPage(TestPage::class.java, "设置")
    }

    private fun switchPageWithFade(key: String) {
        val rootContainer = findViewById<ViewGroup>(android.R.id.content)

        // MIUIActivity 的内容布局应该是第 0 个 child
        val pageContainer = rootContainer.getChildAt(0) ?: run {
            showFragment(key)
            return
        }

        val duration = 160L

        // 先把之前可能没完成的动画取消掉，避免连点出现鬼畜
        pageContainer.animate().cancel()

        pageContainer.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                // 真正切页
                showFragment(key)

                pageContainer.alpha = 0f
                pageContainer.animate()
                    .alpha(1f)
                    .setDuration(duration)
                    .start()
            }
            .start()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        setSP(getPreferences(0))
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)

        val rootContainer = findViewById<ViewGroup>(android.R.id.content)

        globalHeader = GlobalHeader(this)
        globalHeader.setTitle("")

        // 实例上的 pageSwitcher
        pageSwitcher = PageSlideSwitcher(this)

        moreButton.visibility = View.VISIBLE
        rootContainer.addView(moreButton)

        // 用字段而不是局部 val
        bottomMenu = BottomPageMenuView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.BOTTOM }

            setItems(
                listOf(
                    BottomPageMenuView.Item(0, R.drawable.abc_loading, "闹钟"),
                    BottomPageMenuView.Item(1, R.drawable.abc_loading, "世界时钟")
                ),
                defaultIndex = 0
            )

            currentTabIndex = 0
            pageSwitcher.setCurrentIndex(currentTabIndex)

            setOnItemSelectedListener { index, _ ->
                if (index == currentTabIndex) return@setOnItemSelectedListener

                val key = when (index) {
                    0 -> "__main__"
                    1 -> "TestPage"
                    else -> "__main__"
                }

                pageSwitcher.switchTo(key, index)
                currentTabIndex = index
            }
        }

        rootContainer.addView(bottomMenu)
    }
}