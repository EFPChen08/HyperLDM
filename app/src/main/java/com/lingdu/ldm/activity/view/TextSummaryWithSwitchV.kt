/*
 * BlockMIUI
 * Copyright (C) 2022 fkj@fkj233.cn
 * https://github.com/577fkj/BlockMIUI
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License v2.1
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version and our eula as published
 * by 577fkj.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * GNU Lesser General Public License v2.1 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License v2.1
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 * <https://github.com/577fkj/BlockMIUI/blob/main/LICENSE>.
 */

package com.lingdu.ldm.activity.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.lingdu.ldm.R
import com.lingdu.ldm.activity.data.DataBinding
import com.lingdu.ldm.activity.data.LayoutPair
import com.lingdu.ldm.activity.dp2px
import com.lingdu.ldm.activity.fragment.MIUIFragment

class TextSummaryWithSwitchV(private val textSummaryV: TextSummaryV, private val switchV: SwitchV, private val dataBindingRecv: DataBinding.Binding.Recv? = null): BaseView {

    override fun getType(): BaseView = this

    override fun create(context: Context, callBacks: (() -> Unit)?): View {
        textSummaryV.notShowMargins(true)
        return LinearContainerV(
            LinearContainerV.HORIZONTAL,
            arrayOf(
                LayoutPair(textSummaryV.create(context, callBacks), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)),
                LayoutPair(switchV.create(context, callBacks), LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.gravity = Gravity.CENTER_VERTICAL })
            ),
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT).also {
                it.setMargins(0, dp2px(context, 17.75f),0, dp2px(context, 17.75f))
            }
        ).create(context, callBacks).also {
            dataBindingRecv?.setView(it)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDraw(thiz: MIUIFragment, group: LinearLayout, view: View) {
        // 现在背景/按压/整行点击都由 MIUIFragment 统一处理
        group.addView(view)
        group.setOnTouchListener(null)
        group.setOnClickListener(null)
        group.background = null
    }

}