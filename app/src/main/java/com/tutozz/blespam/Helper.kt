package com.tutozz.blespam

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*

object Helper {
    private val hexDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
    val random = Random()
    var delay = 20
    val delays = intArrayOf(10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 6000, 8000, 10000, 12000, 15000, 18000, 20000)
    const val MAX_LOOP = 50_000_000

    @RequiresApi(Build.VERSION_CODES.S)
    fun isPermissionGranted(c: Context): Boolean {
        return (ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) &&
                (ActivityCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
    }

    fun convertHexToByteArray(hex: String): ByteArray {
        val cleanHex = hex.lowercase().replace("0x", "").replace(" ", "")
        if (cleanHex.isEmpty() || cleanHex.length % 2 != 0) {
            throw IllegalArgumentException("Invalid hex string: $cleanHex")
        }
        val data = ByteArray(cleanHex.length / 2)
        for (i in 0 until cleanHex.length step 2) {
            val high = Character.digit(cleanHex[i], 16)
            val low = Character.digit(cleanHex[i + 1], 16)
            if (high == -1 || low == -1) {
                throw IllegalArgumentException("Invalid hex character in: $cleanHex")
            }
            data[i / 2] = ((high shl 4) or low).toByte()
        }
        return data
    }

    fun randomHexFiller(size: Int): String {
        return buildString {
            repeat(size) { append(hexDigits[random.nextInt(hexDigits.size)]) }
        }
    }

    fun log(msg: String) {
        android.util.Log.d("BLESpam", msg)
    }
}