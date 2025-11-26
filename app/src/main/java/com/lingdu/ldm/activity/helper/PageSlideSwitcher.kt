package com.lingdu.ldm.activity.helper

import com.lingdu.ldm.activity.MIUIActivity

/**
 * 底部 tab 切换专用：
 *  - 不再用 onBackPressed / popBackStack
 *  - 统一走 MIUIActivity.showFragmentWithSlide(key, direction)
 *  - direction > 0 视为“往右切”（0 -> 1），direction < 0 视为“往左切”（1 -> 0）
 */
class PageSlideSwitcher(
    private val activity: MIUIActivity
) {

    private var currentIndex: Int = 0

    fun setCurrentIndex(index: Int) {
        currentIndex = index
    }

    /**
     * @param key      页面 key（"__main__", "TestPage"...）
     * @param newIndex 底部 tab 的下标
     */
    fun switchTo(key: String, newIndex: Int) {
        if (newIndex == currentIndex) return

        val direction = if (newIndex > currentIndex) 1 else -1

        // ✅ 走我们自己写的带方向动画的 showFragmentWithSlide
        activity.showFragmentWithSlide(key, direction)

        currentIndex = newIndex
    }
}
