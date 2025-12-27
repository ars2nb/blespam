package com.tutozz.blespam

import android.bluetooth.BluetoothAdapter

object BluetoothHelper {
    val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
}