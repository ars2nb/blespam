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



@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private fun checkForNewVersion(manualVersion: String) {
        Thread {
            try {
                val url = URL("https://example.com/main.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val inputStream = connection.inputStream
                val jsonResponse = inputStream.bufferedReader().readText()
                val jsonObject = JSONObject(jsonResponse)

                val latestVersion = jsonObject.getString("version")
                val releaseNotes = jsonObject.getString("release_notes")
                val downloadUrl = jsonObject.getString("download_url")

                // Use only the manually entered version
                val currentVersion = manualVersion

                if (latestVersion != currentVersion) {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("Update Available")
                            .setMessage("A new version is available: $latestVersion\n\nChangeLog:\n$releaseNotes")
                            .setPositiveButton("Update") { _, _ ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                startActivity(intent)
                            }
                            .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
                            .show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }



    private fun checkBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    private fun promptToEnableBluetooth() {
        Toast.makeText(this, "Bluetooth is off. Please turn it on.", Toast.LENGTH_SHORT).show()
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        startActivityForResult(enableBtIntent, 1) // Request user to enable Bluetooth
    }

    private fun onClickSpamButton(spammer: Spammer, button: Button, circle: ImageView) {
        button.setOnClickListener {
            if (!checkBluetoothEnabled()) {
                promptToEnableBluetooth() // If Bluetooth is off, prompt user to turn it on
                return@setOnClickListener
            }

            if (!spammer.isSpamming) {
                spammer.start()
                // blink animation
                circle.setImageResource(R.drawable.active_circle)
                if (spammer.blinkRunnable == null) {
                    spammer.blinkRunnable = startBlinking(circle, spammer, button)
                }
                // button style
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.orange)
                button.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                spammer.stop()
                // blink animation
                circle.setImageResource(R.drawable.grey_circle)
                // button style restore
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.empty)
                button.setTextColor(ContextCompat.getColor(this, R.color.black))
            }
        }
    }

    // blink animation with additional Bluetooth check
    private fun startBlinking(imageView: ImageView, spammer: Spammer, button: Button): Runnable {
        val handler = Handler(Looper.getMainLooper())
        val blinkRunnable: Runnable = object : Runnable {
            override fun run() {
                // If spamming is on, but Bluetooth is off, stop the spammer and update UI
                if (spammer.isSpamming && !checkBluetoothEnabled()) {
                    spammer.stop()
                    imageView.setImageResource(R.drawable.grey_circle)
                    // Restore button style
                    button.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.empty)
                    button.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
                    Toast.makeText(this@MainActivity, "Bluetooth is off. Spammer stopped.", Toast.LENGTH_SHORT).show()
                    return // Exit the loop
                }
                // Standard blinking logic
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // Ask missing permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 1)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), 1)
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE), 1)
            }
        }

        if (Helper.isPermissionGranted(this)) {
            // Setup click listeners
            onClickSpamButton(ContinuitySpam(ContinuityDevice.type.ACTION, true), binding.ios17CrashButton, binding.ios17CrashCircle)
            onClickSpamButton(ContinuitySpam(ContinuityDevice.type.ACTION, false), binding.appleActionModalButton, binding.appleActionModalCircle)
            onClickSpamButton(ContinuitySpam(ContinuityDevice.type.DEVICE, false), binding.appleDevicePopupButton, binding.appleDevicePopupCircle)
            onClickSpamButton(FastPairSpam(), binding.androidFastPairButton, binding.androidFastPairCircle)
            onClickSpamButton(EasySetupSpam(EasySetupDevice.type.BUDS), binding.samsungEasyPairBudsButton, binding.samsungEasyPairBudsCircle)
            onClickSpamButton(EasySetupSpam(EasySetupDevice.type.WATCH), binding.samsungEasyPairWatchButton, binding.samsungEasyPairWatchCircle)
            try {
                val swiftPairSpam = SwiftPairSpam()
                onClickSpamButton(swiftPairSpam, binding.windowsSwiftPairButton, binding.windowsSwiftPairCircle)
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to initialize SwiftPairSpam", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

            // Delay Buttons onClick
            binding.minusDelayButton.setOnClickListener {
                val i = Helper.delays.indexOf(Helper.delay)
                if (i > 0) {
                    Helper.delay = Helper.delays[i - 1]
                    binding.delayText.text = Helper.delay.toString() + "ms"
                }
            }
            binding.plusDelayButton.setOnClickListener {
                val i = Helper.delays.indexOf(Helper.delay)
                if (i < Helper.delays.size - 1) {
                    Helper.delay = Helper.delays[i + 1]
                    binding.delayText.text = Helper.delay.toString() + "ms"
                }
            }
        }
        checkForNewVersion("2.1")
    }

    // Handle Bluetooth enable result
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // Bluetooth is enabled
                Toast.makeText(this, "Bluetooth is now enabled.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth enabling failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
