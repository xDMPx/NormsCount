package com.xdmpx.normscount

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

abstract class CurrentActivity {

    companion object {
        @Volatile
        private var INSTANCE: ComponentActivity? = null

        fun setInstance(instance: ComponentActivity) {
            INSTANCE = instance
        }

        fun getInstance(): ComponentActivity? {
            synchronized(this) {
                return INSTANCE
            }
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.getStringExtra("action")
        val activity = CurrentActivity.getInstance()
        if (action == "increase" && activity != null) {
            val mainActivity = activity as MainActivity
            mainActivity.getCounterViewModel().incrementCounter()
        }
    }
}