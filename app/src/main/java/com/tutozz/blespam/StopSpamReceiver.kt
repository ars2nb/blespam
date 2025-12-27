package com.tutozz.blespam

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopSpamReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP) {
            SpamService.stopAllSpammers(context)
            val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_UI_STOPPED
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(mainActivityIntent)
        }
    }

    companion object {
        const val ACTION_STOP = SpamService.ACTION_STOP
        const val ACTION_UI_STOPPED = "com.tutozz.blespam.ACTION_UI_STOPPED"
    }
}
