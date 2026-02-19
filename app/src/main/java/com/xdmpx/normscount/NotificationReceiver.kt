package com.xdmpx.normscount

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xdmpx.normscount.counter.CountersViewModelInstance

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.getStringExtra("action")
        val counter = CountersViewModelInstance.getInstance()
        if (action == "increment" && counter != null) {
            counter.incrementCurrentCounter()
            counter.synchronizeCountersWithCurrentCounter()
        }
        if (action == "decrement" && counter != null) {
            counter.decrementCurrentCounter()
            counter.synchronizeCountersWithCurrentCounter()
        }
    }
}