package com.xdmpx.normscount

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xdmpx.normscount.counter.CurrentCounter

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.getStringExtra("action")
        val counter = CurrentCounter.getInstance()
        if (action == "increment" && counter != null) {
            counter.incrementCounter()
        }
        if (action == "decrement" && counter != null) {
            counter.decrementCounter()
        }
    }
}