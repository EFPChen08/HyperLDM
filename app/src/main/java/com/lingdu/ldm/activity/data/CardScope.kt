@file:Suppress("FunctionName")

package com.lingdu.ldm.activity.data

import com.lingdu.ldm.activity.view.BaseView

/**
 * WhiteCard { ... } 内部作用域：
 * 只负责收集 BaseView（用 add 或 + 都行）
 */
class CardScope {
    internal val items: ArrayList<BaseView> = arrayListOf()

    fun add(item: BaseView) {
        items.add(item)
    }

    operator fun BaseView.unaryPlus() {
        items.add(this)
    }
}
