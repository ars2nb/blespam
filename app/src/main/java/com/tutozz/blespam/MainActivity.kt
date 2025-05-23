package com.tutozz.blespam

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tutozz.blespam.databinding.ActivityMainBinding
import java.io.IOException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Paint
import android.widget.TextView

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    fun openSocialLink(view: View) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/social.html"))
        view.context.startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAllSpammers()
    }

    private val spammerList = mutableListOf<Spammer>()

    private fun stopAllSpammers() {
        spammerList.forEach { spammer ->
            if (spammer.isSpamming) {
                spammer.stop()
            }
        }
    }

    private lateinit var binding: ActivityMainBinding

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0"
        }
    }

    private fun checkForNewVersion() {
        Thread {
            try {
                val url = URL("https://example.com/index.json")
                val currentVersion = getAppVersion()

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val inputStream = connection.inputStream
                val jsonResponse = inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(jsonResponse)

                val latestVersion = jsonObject.getString("version")
                val releaseNotes = jsonObject.getString("release_notes")
                val downloadUrl = jsonObject.getString("download_url")
                val minSupportedVersion = jsonObject.optString("min_supported_version", "0.0")

                runOnUiThread {
                    when {
                        isVersionNewer(minSupportedVersion, currentVersion) -> {
                            // Forced update
                            AlertDialog.Builder(this)
                                .setTitle(R.string.update_required_title)
                                .setMessage(getString(R.string.update_message, latestVersion, releaseNotes))
                                .setCancelable(false)
                                .setPositiveButton(R.string.update_button) { _, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        }
                        isVersionNewer(latestVersion, currentVersion) -> {
                            // Regular update
                            AlertDialog.Builder(this)
                                .setTitle(getString(R.string.update_available_title))
                                .setMessage(getString(R.string.update_available_message, latestVersion, releaseNotes))
                                .setPositiveButton(R.string.update_button) { _, _ ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    startActivity(intent)
                                }
                                .setNegativeButton(R.string.later_button) { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun isVersionNewer(newVersion: String, currentVersion: String): Boolean {
        fun normalize(v: String) = v.replace("[^\\d.]".toRegex(), "")
            .split(".")
            .mapNotNull { it.toIntOrNull() }

        val newParts = normalize(newVersion)
        val currParts = normalize(currentVersion)

        val maxLength = maxOf(newParts.size, currParts.size)
        val paddedNew = newParts + List(maxLength - newParts.size) { 0 }
        val paddedCurr = currParts + List(maxLength - currParts.size) { 0 }

        for (i in 0 until maxLength) {
            if (paddedNew[i] > paddedCurr[i]) return true
            if (paddedNew[i] < paddedCurr[i]) return false
        }
        return false
    }

    private fun checkBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    private fun promptToEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API 31) and higher, request BLUETOOTH_CONNECT
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    2
                )
                return
            }
        } else {
            // For older Android versions, request BLUETOOTH permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH),
                    2
                )
                return
            }
        }

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, 1)
    }


    private fun onClickSpamButton(spammer: Spammer, button: Button, circle: ImageView) {
        button.setOnClickListener {
            if (!checkBluetoothEnabled()) {
                promptToEnableBluetooth()
                return@setOnClickListener
            }

            if (!spammer.isSpamming) {
                spammer.start()
                circle.setImageResource(R.drawable.active_circle)
                if (spammer.blinkRunnable == null) {
                    spammer.blinkRunnable = startBlinking(circle, spammer, button)
                }
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.orange)
                button.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                spammer.stop()
                circle.setImageResource(R.drawable.grey_circle)
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.empty)
                button.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }

    private fun startBlinking(imageView: ImageView, spammer: Spammer, button: Button): Runnable {
        val handler = Handler(Looper.getMainLooper())
        val blinkRunnable: Runnable = object : Runnable {
            override fun run() {
                if (spammer.isSpamming && !checkBluetoothEnabled()) {
                    spammer.stop()
                    imageView.setImageResource(R.drawable.grey_circle)
                    button.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.empty)
                    button.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                    Toast.makeText(this@MainActivity, getString(R.string.bluetoothoff_spammeroff), Toast.LENGTH_SHORT).show()
                    return
                }
                if (spammer.isSpamming) {
                    if (imageView.visibility == View.VISIBLE) {
                        imageView.visibility = View.INVISIBLE
                        handler.postDelayed(this, Helper.delay.toLong())
                    } else {
                        imageView.visibility = View.VISIBLE
                        handler.postDelayed(this, (Helper.delay / 10).coerceAtLeast(20).toLong())
                    }
                } else {
                    imageView.visibility = View.VISIBLE
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.postDelayed(blinkRunnable, 100)
        return blinkRunnable
    }

    @SuppressLint("SetTextI18n", "NewApi", "MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val bugButton: ImageView = findViewById(R.id.bugReportButton)
        bugButton.setOnClickListener {
            val intent = Intent(this, BugReportActivity::class.java)
            startActivity(intent)
        }

        val socialLink = findViewById<TextView>(R.id.socialLink)
        socialLink.paintFlags = socialLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 1)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), 1)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), 1)
            }
        }

        checkForNewVersion()

        if (Helper.isPermissionGranted(this)) {
            initializeSpamButtons()
            setupDelayButtons()
        }
    }


    private fun initializeSpamButtons() {
        try {
            onClickSpamButton(ContinuitySpam(ContinuityDevice.type.ACTION, true), binding.ios17CrashButton, binding.ios17CrashCircle)
            onClickSpamButton(ContinuitySpam(ContinuityDevice.type.ACTION, false), binding.appleActionModalButton, binding.appleActionModalCircle)
            onClickSpamButton(ContinuitySpam(ContinuityDevice.type.DEVICE, false), binding.appleDevicePopupButton, binding.appleDevicePopupCircle)
            onClickSpamButton(FastPairSpam(), binding.androidFastPairButton, binding.androidFastPairCircle)
            onClickSpamButton(EasySetupSpam(EasySetupDevice.type.BUDS), binding.samsungEasyPairBudsButton, binding.samsungEasyPairBudsCircle)
            onClickSpamButton(EasySetupSpam(EasySetupDevice.type.WATCH), binding.samsungEasyPairWatchButton, binding.samsungEasyPairWatchCircle)

            val swiftPairSpam = SwiftPairSpam()
            onClickSpamButton(swiftPairSpam, binding.windowsSwiftPairButton, binding.windowsSwiftPairCircle)
        } catch (e: IOException) {
            Toast.makeText(this, getString(R.string.swiftpair), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupDelayButtons() {
        binding.minusDelayButton.setOnClickListener {
            val i = Helper.delays.indexOf(Helper.delay)
            if (i > 0) {
                Helper.delay = Helper.delays[i - 1]
                binding.delayText.text = "${Helper.delay}ms"
            }
        }

        binding.plusDelayButton.setOnClickListener {
            val i = Helper.delays.indexOf(Helper.delay)
            if (i < Helper.delays.size - 1) {
                Helper.delay = Helper.delays[i + 1]
                binding.delayText.text = "${Helper.delay}ms"
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, getString(R.string.bluetoothon), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.bluetootherror), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
