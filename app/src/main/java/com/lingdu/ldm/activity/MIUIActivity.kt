@file:Suppress("DEPRECATION")

package com.lingdu.ldm.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.FragmentManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import com.lingdu.ldm.R
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Keep
import com.lingdu.ldm.activity.annotation.BMMainPage
import com.lingdu.ldm.activity.annotation.BMMenuPage
import com.lingdu.ldm.activity.annotation.BMPage
import com.lingdu.ldm.activity.data.AsyncInit
import com.lingdu.ldm.activity.data.BasePage
import com.lingdu.ldm.activity.data.InitView
import com.lingdu.ldm.activity.data.SafeSharedPreferences
import com.lingdu.ldm.activity.fragment.MIUIFragment
import com.lingdu.ldm.activity.view.BaseView
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.WindowInsetsController
import android.view.WindowManager
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import android.view.ViewGroup


@Keep
open class MIUIActivity : Activity() {
    private var callbacks: (() -> Unit)? = null
    private var thisName: ArrayList<String> = arrayListOf()
    private lateinit var viewData: InitView
    private val dataList: HashMap<String, InitView.ItemData> = hashMapOf()
    private lateinit var initViewData: InitView.() -> Unit

    lateinit var blurView: BlurView

    // ===== 顶部栏 =====
    lateinit var topBarContainer: FrameLayout
    lateinit var topBarContent: LinearLayout

    fun setTopBarProgress(progress: Float) {
        val p = progress.coerceIn(0f, 1f)

        // 顶部栏整体不动 alpha，避免抖
        topBarContainer.alpha = 1f

        // 模糊层跟着渐显/渐隐
        blurView.alpha = p * 2

        val isNight = (resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 白雾叠加跟着渐显/渐隐，最高 90%
        val overlayAlpha = (p * 0.9f * 255).toInt()
        val overlayColor = if (isNight) (overlayAlpha shl 24) or 0x00F000000 else (overlayAlpha shl 24) or 0x00FF7F7F7
        blurView.setOverlayColor(overlayColor)

        // 文字/按钮跟着渐显/渐隐
        topBarContent.alpha = p

        topBarContainer.isClickable = p > 0.02f
    }



    companion object {
        var safeSP: SafeSharedPreferences = SafeSharedPreferences()

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        @SuppressLint("StaticFieldLeak")
        lateinit var activity: MIUIActivity
    }

    private val backButton by lazy {
        ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.gravity = Gravity.CENTER_VERTICAL
                if (isRtl(context)) it.setMargins(dp2px(activity, 5f), 0, 0, 0)
                else it.setMargins(0, 0, dp2px(activity, 5f), 0)
            }
            background = getDrawable(R.drawable.abc_ic_ab_back_material)
            visibility = View.GONE
            setOnClickListener { this@MIUIActivity.onBackPressed() }
        }
    }

    private val menuButton by lazy {
        ImageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = Gravity.CENTER_VERTICAL }
            background = getDrawable(R.drawable.abc_ic_menu_overflow_material)
            visibility = View.GONE
            if (isRtl(context)) setPadding(dp2px(activity, 25f), 0, 0, 0)
            else setPadding(0, 0, dp2px(activity, 25f), 0)
            setOnClickListener {
                showFragment(if (this@MIUIActivity::initViewData.isInitialized) "Menu" else "__menu__")
            }
        }
    }

    private fun getStatusBarHeight(): Int {
        val resId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId)
        else dp2px(this, 24f)
    }


    private val titleView by lazy {
        TextView(activity).apply {
            // 关键：宽度用 0 + weight=1，占满左右按钮之间的空间
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).also {
                it.gravity = Gravity.CENTER_VERTICAL
            }

            // 关键：文字居中
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER

            setTextColor(getColor(R.color.whiteText))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)

            // 关键：文字往下挪一点
            setPadding(0, dp2px(activity, 4f), 0, 0)
        }
    }


    private var frameLayoutId: Int = -1
    private val frameLayout by lazy {
        val mFrameLayout = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        frameLayoutId = View.generateViewId()
        mFrameLayout.id = frameLayoutId
        mFrameLayout
    }

    var isLoad = true
    var isExit = false

    fun setLightStatusBar() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        activity = this
        register()
        actionBar?.hide()

        // ===== 根布局改成 FrameLayout，让顶栏覆盖在内容上 =====
        val root = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background = getDrawable(R.color.foreground)
        }

        // ===== 顶部栏容器（覆盖层）=====
        topBarContainer = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )

            // 阴影去掉：不设置 elevation
            background = null
            alpha = 0f
            isClickable = false

            // ===== 模糊背景层（永远存在）=====
            blurView = BlurView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )

                val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                val windowBg = window.decorView.background ?: ColorDrawable(Color.TRANSPARENT)

                setupWith(rootView)
                    .setFrameClearDrawable(windowBg) // ✅ 关键：用 window 背景，不用 rootView.background
                    .setBlurAlgorithm(eightbitlab.com.blurview.RenderScriptBlur(activity))
                    .setBlurRadius(20f)
                    .setBlurAutoUpdate(true)
                    .setHasFixedTransformationMatrix(true)

                // 初始无白雾（但模糊已经在）
                setOverlayColor(0x00FFFFFF)
            }
            addView(blurView)

            // ===== 顶部栏内容 =====
            val statusH = getStatusBarHeight()
            topBarContent = LinearLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                minimumHeight = statusH + dp2px(activity, 54f)

                setPadding(
                    dp2px(activity, 25f),
                    statusH + dp2px(activity, 8f),
                    dp2px(activity, 25f),
                    dp2px(activity, 14f)
                )

                addView(backButton)
                addView(titleView)
                addView(menuButton)

                alpha = 0f // 文字初始隐藏
            }
            addView(topBarContent)
        }


        root.addView(frameLayout)
        root.addView(topBarContainer)
        setContentView(root)

        // ===== 原逻辑不动 =====
        if (savedInstanceState != null) {
            if (this::initViewData.isInitialized) {
                viewData = InitView(dataList).apply(initViewData)
                setMenuShow(viewData.isMenu)
                val list = savedInstanceState.getStringArrayList("this")!!
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                for (name: String in list) showFragment(name)
                if (list.size == 1) setBackupShow(viewData.mainShowBack)
                return
            }
            val list = savedInstanceState.getStringArrayList("this")!!
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            initAllPage()
            if (pageInfo.containsKey("__menu__")) setMenuShow(list.size == 1)
            for (name: String in list) showFragment(name)
        } else {
            if (isLoad) {
                if (this::initViewData.isInitialized) {
                    viewData = InitView(dataList).apply(initViewData)
                    setBackupShow(!viewData.mainShowBack)
                    setMenuShow(viewData.isMenu)
                    showFragment("Main")
                    return
                }
                initAllPage()
                showFragment("__main__")
            }
        }

        val showFragmentName = intent.getStringExtra("showFragment").toString()
        if (showFragmentName != "null" && showFragmentName.isNotEmpty()) {
            if (pageInfo.containsKey(showFragmentName)) {
                showFragment(showFragmentName)
                return
            }
        }

        setLightStatusBar()
    }

    private val pageInfo: HashMap<String, BasePage> = hashMapOf()
    private val pageList: HashMap<Class<out BasePage>, String> = HashMap()

    fun registerPage(basePage: Class<out BasePage>, title: String? = null) {
        pageList[basePage] = title ?: basePage.simpleName
    }

    open fun register() {}

    fun initAllPage() {
        pageList.forEach { (basePage, _) ->
            val mainPage = basePage.newInstance()
            mainPage.activity = this
            if (basePage.getAnnotation(BMMainPage::class.java) != null) {
                pageInfo["__main__"] = mainPage
            } else if (basePage.getAnnotation(BMMenuPage::class.java) != null) {
                menuButton.visibility = View.VISIBLE
                pageInfo["__menu__"] = mainPage
            } else if (basePage.getAnnotation(BMPage::class.java) != null) {
                pageInfo[basePage.simpleName] = mainPage
            } else {
                throw Exception("Page must be annotated with BMMainPage or BMMenuPage or BMPage")
            }
        }
    }

    fun initView(iView: InitView.() -> Unit) {
        initViewData = iView
    }

    override fun setTitle(title: CharSequence?) {
        titleView.text = title
    }

    fun setSP(sharedPreferences: SharedPreferences) { safeSP.mSP = sharedPreferences }
    fun getSP(): SharedPreferences? = safeSP.mSP

    fun showFragment(key: String) {
        if (this::initViewData.isInitialized) {
            title = dataList[key]?.title
            thisName.add(key)
            val frame = MIUIFragment(key)
            if (key != "Main" && fragmentManager.backStackEntryCount != 0) {
                fragmentManager.beginTransaction().let {
                    if (key != "Menu") {
                        if (isRtl(activity)) it.setCustomAnimations(
                            R.animator.slide_left_in,
                            R.animator.slide_right_out,
                            R.animator.slide_right_in,
                            R.animator.slide_left_out
                        )
                        else it.setCustomAnimations(
                            R.animator.slide_right_in,
                            R.animator.slide_left_out,
                            R.animator.slide_left_in,
                            R.animator.slide_right_out
                        )
                    } else {
                        if (isRtl(activity)) it.setCustomAnimations(
                            R.animator.slide_right_in,
                            R.animator.slide_left_out,
                            R.animator.slide_left_in,
                            R.animator.slide_right_out
                        )
                        else it.setCustomAnimations(
                            R.animator.slide_left_in,
                            R.animator.slide_right_out,
                            R.animator.slide_right_in,
                            R.animator.slide_left_out
                        )
                    }
                }.replace(frameLayoutId, frame).addToBackStack(key).commit()
                backButton.visibility = View.VISIBLE
                setMenuShow(dataList[key]?.hideMenu == false)
            } else {
                setBackupShow(viewData.mainShowBack)
                fragmentManager.beginTransaction().replace(frameLayoutId, frame).addToBackStack(key).commit()
            }
            return
        }

        if (!pageInfo.containsKey(key)) throw Exception("No page found")
        val thisPage = pageInfo[key]!!
        title = getPageTitle(thisPage::class.java)
        thisName.add(key)
        val frame = MIUIFragment(key)

        if (key != "__main__" && fragmentManager.backStackEntryCount != 0) {
            fragmentManager.beginTransaction().let {
                if (key != "__menu__") {
                    if (isRtl(activity)) it.setCustomAnimations(
                        R.animator.slide_left_in,
                        R.animator.slide_right_out,
                        R.animator.slide_right_in,
                        R.animator.slide_left_out
                    )
                    else it.setCustomAnimations(
                        R.animator.slide_right_in,
                        R.animator.slide_left_out,
                        R.animator.slide_left_in,
                        R.animator.slide_right_out
                    )
                } else {
                    if (isRtl(activity)) it.setCustomAnimations(
                        R.animator.slide_right_in,
                        R.animator.slide_left_out,
                        R.animator.slide_left_in,
                        R.animator.slide_right_out
                    )
                    else it.setCustomAnimations(
                        R.animator.slide_left_in,
                        R.animator.slide_right_out,
                        R.animator.slide_right_in,
                        R.animator.slide_left_out
                    )
                }
            }.replace(frameLayoutId, frame).addToBackStack(key).commit()

            setBackupShow(true)
            if (key !in arrayOf("__main__", "__menu__")) setMenuShow(!getPageHideMenu(thisPage))
            if (key == "__menu__") setMenuShow(false)
        } else {
            setMenuShow(pageInfo.containsKey("__menu__"))
            setBackupShow(pageInfo["__main__"]!!.javaClass.getAnnotation(BMMainPage::class.java)!!.showBack)
            fragmentManager.beginTransaction().replace(frameLayoutId, frame).addToBackStack(key).commit()
        }
    }

    fun setMenuShow(show: Boolean) {
        if (this::initViewData.isInitialized) {
            if (!dataList.containsKey("Menu")) return
            menuButton.visibility = if (show) View.VISIBLE else View.GONE
            return
        }
        if (pageInfo.containsKey("__menu__")) {
            menuButton.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun setBackupShow(show: Boolean) {
        backButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun getPageHideMenu(basePage: BasePage): Boolean {
        return basePage.javaClass.getAnnotation(BMPage::class.java)?.hideMenu == true
    }

    private fun getPageTitle(basePage: Class<out BasePage>): String {
        return pageList[basePage].toString()
    }

    fun getTopPage(): String = thisName[thisName.lastSize()]

    fun getThisItems(key: String): List<BaseView> {
        if (this::initViewData.isInitialized) {
            return dataList[key]?.itemList ?: arrayListOf()
        }
        val currentPage = pageInfo[key]!!
        if (currentPage.itemList.size == 0) currentPage.onCreate()
        return currentPage.itemList
    }

    fun getThisAsync(key: String): AsyncInit? {
        if (this::initViewData.isInitialized) return dataList[key]?.async

        val currentPage = pageInfo[key]!!
        if (currentPage.itemList.size == 0) currentPage.onCreate()
        return object : AsyncInit {
            override val skipLoadItem: Boolean
                get() = currentPage.skipLoadItem

            override fun onInit(fragment: MIUIFragment) {
                currentPage.asyncInit(fragment)
            }
        }
    }

    fun getAllCallBacks(): (() -> Unit)? = callbacks
    fun setAllCallBacks(callbacks: () -> Unit) { this.callbacks = callbacks }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount <= 1) {
            if (isExit) finishAndRemoveTask() else finish()
        } else {
            thisName.removeAt(thisName.lastSize())
            val name = fragmentManager.getBackStackEntryAt(fragmentManager.backStackEntryCount - 2).name
            when (name) {
                "Main" -> {
                    if (!viewData.mainShowBack) backButton.visibility = View.GONE
                    if (viewData.isMenu) menuButton.visibility = View.VISIBLE
                }
                "__main__" -> {
                    if (!pageInfo[name]!!.javaClass.getAnnotation(BMMainPage::class.java)!!.showBack)
                        backButton.visibility = View.GONE
                    setMenuShow(pageInfo.containsKey("__menu__"))
                }
                else -> {
                    if (this::initViewData.isInitialized) {
                        setMenuShow(dataList[name]?.hideMenu == false)
                    } else {
                        setMenuShow(!getPageHideMenu(pageInfo[name]!!))
                    }
                }
            }
            title = if (this::initViewData.isInitialized) {
                dataList[name]?.title
            } else {
                getPageTitle(pageInfo[name]!!::class.java)
            }
            fragmentManager.popBackStack()
        }
    }

    private fun ArrayList<*>.lastSize(): Int = this.size - 1

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList("this", thisName)
    }
}
