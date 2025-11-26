@file:Suppress("DEPRECATION")

package com.lingdu.ldm.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.FragmentManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.Keep
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.annotation.BMMainPage
import com.lingdu.ldm.activity.annotation.BMMenuPage
import com.lingdu.ldm.activity.annotation.BMPage
import com.lingdu.ldm.activity.data.AsyncInit
import com.lingdu.ldm.activity.data.BasePage
import com.lingdu.ldm.activity.data.InitView
import com.lingdu.ldm.activity.data.SafeSharedPreferences
import com.lingdu.ldm.activity.fragment.MIUIFragment
import com.lingdu.ldm.activity.view.BaseView
import com.lingdu.ldm.activity.view.GlobalTitle
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur

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

    // 🔥 全局大标题
    private var globalTitleView: GlobalTitle? = null

    companion object {
        var safeSP: SafeSharedPreferences = SafeSharedPreferences()

        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        @SuppressLint("StaticFieldLeak")
        lateinit var activity: MIUIActivity
    }

    override fun onResume() {
        super.onResume()
        // 回到任何一个继承 MIUIActivity 的界面，都把静态指针重新指向当前这个
        context = this
        activity = this
    }


    // 供 BasePage 调用更新标题
    // 供 BasePage 调用更新标题
    fun setPageTitle(text: String) {
        runOnUiThread {
            // 每次切换页面时先把大标题的位置归零，再更新文字
            globalTitleView?.apply {
                translationY = 0f
                updateTitle(text)
            }
        }
    }


    fun setTopBarProgress(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        topBarContainer.alpha = 1f
        blurView.alpha = p * 2
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val overlayAlpha = (p * 0.9f * 255).toInt()
        val overlayColor = if (isNight) (overlayAlpha shl 24) or 0x00F000000 else (overlayAlpha shl 24) or 0x00FF7F7F7
        blurView.setOverlayColor(overlayColor)
        topBarContent.alpha = p
        topBarContainer.isClickable = p > 0.02f
    }

    fun onPageScroll(scrollY: Int) {
        val offset = scrollY.coerceAtLeast(0)
        // ScrollView 内容往上滚多少，大标题就往上挪多少
        globalTitleView?.translationY = -offset.toFloat()
    }

    // ===== 各种 View 的懒加载 =====
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
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.gravity = Gravity.CENTER_VERTICAL
            }
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(getColor(R.color.whiteText))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            paint.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
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

        // 1. 创建总根布局 FrameLayout
        val root = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            background = getDrawable(R.color.foreground)
        }

        // 2. 添加内容容器 (最底层)
        root.addView(frameLayout)

        // 3. ✅ 添加全局标题 (中间层，浮在内容之上)
        globalTitleView = GlobalTitle(this)
        root.addView(globalTitleView)

        // 4. 添加顶部栏 (最上层)
        topBarContainer = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            )
            background = null
            alpha = 0f
            isClickable = false

            // 4.1 模糊背景
            blurView = BlurView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
                val windowBg = window.decorView.background ?: ColorDrawable(Color.TRANSPARENT)
                setupWith(rootView)
                    .setFrameClearDrawable(windowBg)
                    .setBlurAlgorithm(RenderScriptBlur(activity))
                    .setBlurRadius(20f)
                    .setBlurAutoUpdate(true)
                    .setHasFixedTransformationMatrix(true)
                setOverlayColor(0x00FFFFFF)
            }
            addView(blurView)

            // 4.2 顶部栏内容 (返回键、小标题、菜单)
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
                alpha = 0f
            }
            addView(topBarContent)
        }

        // 将 TopBar 加入根布局
        root.addView(topBarContainer)

        topBarContainer.bringToFront()

        setContentView(root)

        // ===== 状态恢复逻辑 =====
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

    // ✅ 恢复了原始 showFragment 的逻辑，并加入了 setPageTitle
    fun showFragment(key: String) {
        if (this::initViewData.isInitialized) {
            title = dataList[key]?.title
            thisName.add(key)
            val frame = MIUIFragment(key)
            if (key != "Main" && fragmentManager.backStackEntryCount != 0) {
                fragmentManager.beginTransaction().let {
                    if (key != "Menu") {
                        if (isRtl(activity)) it.setCustomAnimations(R.animator.slide_left_in, R.animator.slide_right_out, R.animator.slide_right_in, R.animator.slide_left_out)
                        else it.setCustomAnimations(R.animator.slide_right_in, R.animator.slide_left_out, R.animator.slide_left_in, R.animator.slide_right_out)
                    } else {
                        if (isRtl(activity)) it.setCustomAnimations(R.animator.slide_right_in, R.animator.slide_left_out, R.animator.slide_left_in, R.animator.slide_right_out)
                        else it.setCustomAnimations(R.animator.slide_left_in, R.animator.slide_right_out, R.animator.slide_right_in, R.animator.slide_left_out)
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

        // 🔥 刷新大标题
        setPageTitle(thisPage.getPageTitle())

        title = getPageTitle(thisPage::class.java)
        thisName.add(key)
        val frame = MIUIFragment(key)

        if (key != "__main__" && fragmentManager.backStackEntryCount != 0) {
            fragmentManager.beginTransaction().let {
                if (key != "__menu__") {
                    if (isRtl(activity)) it.setCustomAnimations(R.animator.slide_left_in, R.animator.slide_right_out, R.animator.slide_right_in, R.animator.slide_left_out)
                    else it.setCustomAnimations(R.animator.slide_right_in, R.animator.slide_left_out, R.animator.slide_left_in, R.animator.slide_right_out)
                } else {
                    if (isRtl(activity)) it.setCustomAnimations(R.animator.slide_right_in, R.animator.slide_left_out, R.animator.slide_left_in, R.animator.slide_right_out)
                    else it.setCustomAnimations(R.animator.slide_left_in, R.animator.slide_right_out, R.animator.slide_right_in, R.animator.slide_left_out)
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



    /**
     * ✅ 恢复了底部菜单专用动画逻辑：showFragmentWithSlide
     * 确保 direction 逻辑保留，并且在里面调用 setPageTitle
     */
    /**
     * 底部菜单专用：带方向动画的页面切换
     *
     * @param key       页面 key（"__main__", "TestPage" 等）
     * @param direction 1  = 向右切过去（index 变大，例如 0 -> 1）
     *                 -1 = 向左切回来（index 变小，例如 1 -> 0）
     */
    fun showFragmentWithSlide(key: String, direction: Int) {
        // 兼容 initViewData 模式，你现在基本用不到，简单处理即可
        if (this::initViewData.isInitialized) {
            showFragment(key)
            return
        }

        if (!pageInfo.containsKey(key)) throw Exception("No page found")

        val thisPage = pageInfo[key]!!
        title = getPageTitle(thisPage::class.java)
        thisName.add(key)
        val frame = MIUIFragment(key)

        val ft = fragmentManager.beginTransaction()

        // === 水平方向动画（只做位移动画，一切透明度效果都交给你自己的 animator xml）===
        val rtl = isRtl(activity)

        if (direction >= 0) {
            // 👉 往右切：当前页往左出去，新页从右进
            if (rtl) {
                ft.setCustomAnimations(
                    R.animator.slide_left_in,
                    R.animator.slide_right_out
                )
            } else {
                ft.setCustomAnimations(
                    R.animator.slide_right_in,
                    R.animator.slide_left_out
                )
            }
        } else {
            // 👉 往左切回来：当前页往右出去，新页从左进
            if (rtl) {
                ft.setCustomAnimations(
                    R.animator.slide_right_in,
                    R.animator.slide_left_out
                )
            } else {
                ft.setCustomAnimations(
                    R.animator.slide_left_in,
                    R.animator.slide_right_out
                )
            }
        }

        // === 下面这块，完全照你原来的 showFragment 非 initView 模式抄过来 ===
        if (key != "__main__" && fragmentManager.backStackEntryCount != 0) {
            ft.replace(frameLayoutId, frame).addToBackStack(key).commit()

            setBackupShow(true)
            if (key !in arrayOf("__main__", "__menu__")) {
                setMenuShow(!getPageHideMenu(thisPage))
            }
            if (key == "__menu__") {
                setMenuShow(false)
            }
        } else {
            setMenuShow(pageInfo.containsKey("__menu__"))
            setBackupShow(
                pageInfo["__main__"]!!
                    .javaClass
                    .getAnnotation(com.lingdu.ldm.activity.annotation.BMMainPage::class.java)!!
                    .showBack
            )
            ft.replace(frameLayoutId, frame).addToBackStack(key).commit()
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
        if (this::initViewData.isInitialized) {
            return dataList[key]?.async
        }

        // key 找不到就直接返回 null，交给 Fragment 里 async? 去判断
        val currentPage = pageInfo[key] ?: return null

        if (currentPage.itemList.isEmpty()) currentPage.onCreate()

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

            // ✅ 返回时也更新大标题
            if (this::initViewData.isInitialized) {
                title = dataList[name]?.title
            } else {
                val prevPage = pageInfo[name]!!
                setPageTitle(prevPage.getPageTitle())
                title = getPageTitle(prevPage::class.java)
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