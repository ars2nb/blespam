package com.tutozz.blespam

import android.bluetooth.le.AdvertiseCallback

interface Spammer {
    fun start()
    fun stop()
    fun isSpamming(): Boolean

    fun setBlinkRunnable(blinkRunnable: Runnable?)
    fun getBlinkRunnable(): Runnable?
}
