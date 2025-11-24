package com.lingdu.ldm

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.view.WindowCompat
import com.lingdu.ldm.activity.MIUIActivity


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

    init {
        activity = this
        registerPage(MainPage::class.java , "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        setSP(getPreferences(0))
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)

    }
}