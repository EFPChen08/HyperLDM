package com.lingdu.ldm.activity.annotation

import androidx.annotation.Keep

@Keep
annotation class BMMainPage(
    val title: String = "",
    val titleId: Int = 0,
    val showBack: Boolean = false
)
