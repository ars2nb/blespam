package com.tutozz.blespam

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.tutozz.blespam.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var hasShownUpdateDialog = false
    private lateinit var binding: ActivityMainBinding
    private val spammerList = mutableListOf<Spammer>()
    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var downloadProgressBar: ProgressBar
    private val progressHandler = Handler(Looper.getMainLooper())

    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, getString(R.string.bluetoothon), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.bluetootherror), Toast.LENGTH_SHORT).show()
        }
    }

    private val installPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
            val uriString = sharedPref.getString("pending_apk_uri", null)
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                installApk(uri)
                sharedPref.edit().remove("pending_apk_uri").apply()
            } else {
                Toast.makeText(this, getString(R.string.invalid_file_uri), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.install_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    fun openSocialLink(view: View) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SOCIAL_LINK))
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAllSpammers() {
        spammerList.forEach { spammer ->
            if (spammer.isSpamming) {
                spammer.stop()
            }
        }
        if (!isFinishing && !isDestroyed) {
            updateLogoAnimation()
        }
    }

    private fun getAppVersion(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0"
        }
    }

    private fun checkForNewVersion() {
        Thread {
            var connection: HttpsURLConnection? = null
            try {
                val url = URL(VERSION_CHECK_API)
                val currentVersion = getAppVersion()
                connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()

                if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val jsonResponse = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(jsonResponse)

                    val latestVersion = jsonObject.getString("version")
                    val releaseNotes = jsonObject.getString("release_notes")
                    val downloadUrl = jsonObject.getString("download_url")
                    val minSupportedVersion = jsonObject.optString("min_supported_version", "0.0")
                    val blockedVersions = jsonObject.optJSONArray("blocked_versions")?.let { jsonArray ->
                        (0 until jsonArray.length()).map { jsonArray.getString(it) }
                    } ?: emptyList()

                    runOnUiThread {
                        when {
                            blockedVersions.contains(currentVersion) -> {
                                showUpdateDialog(
                                    title = getString(R.string.update_required_title),
                                    message = getString(R.string.blocked_version_message, currentVersion, releaseNotes),
                                    isForced = true,
                                    downloadUrl = downloadUrl
                                )
                            }
                            isVersionNewer(minSupportedVersion, currentVersion) -> {
                                showUpdateDialog(
                                    title = getString(R.string.update_required_title),
                                    message = getString(R.string.update_message, latestVersion, releaseNotes),
                                    isForced = true,
                                    downloadUrl = downloadUrl
                                )
                            }
                            isVersionNewer(latestVersion, currentVersion) -> {
                                showUpdateDialog(
                                    title = getString(R.string.update_available_title),
                                    message = getString(R.string.update_available_message, latestVersion, releaseNotes),
                                    isForced = false,
                                    downloadUrl = downloadUrl
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.update_check_failed, Toast.LENGTH_SHORT).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.activeNetwork != null
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    

    private fun hasEnoughStorage(): Boolean {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val freeSpace = downloadsDir.freeSpace
        return freeSpace > 100 * 1024 * 1024 // 100MB
    }

    private fun isValidUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            url.protocol == "https"
        } catch (e: Exception) {
            false
        }
    }

    private fun downloadApk(downloadUrl: String, dialog: AlertDialog) {
        if (!isNetworkAvailable()) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.no_network_connection), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            return
        }

        if (!hasEnoughStorage()) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.insufficient_storage), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            return
        }

        if (!isValidUrl(downloadUrl)) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.invalid_download_url), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 3)
            return
        }

        // Initialize progress UI
        runOnUiThread {
            val progressContainer = dialog.findViewById<LinearLayout>(R.id.progress_container)
            if (progressContainer != null) {
                progressContainer.visibility = View.VISIBLE
                downloadProgressBar.visibility = View.VISIBLE
                downloadProgressBar.isIndeterminate = false
            } else {
                Toast.makeText(this, getString(R.string.error_progress_bar), Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }

        // Start download in a background thread
        Thread {
            var connection: HttpsURLConnection? = null
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null
            try {
                val fileName = "BLESpam-${System.currentTimeMillis()}.apk"
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val apkFile = File(downloadsDir, fileName)

                connection = URL(downloadUrl).openConnection() as HttpsURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode != HttpsURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: ${connection.responseCode}")
                }

                val fileLength = connection.contentLengthLong
                inputStream = connection.inputStream
                outputStream = FileOutputStream(apkFile)

                val buffer = ByteArray(4096)
                var totalBytesRead = 0L
                var lastUpdateTime = System.currentTimeMillis()
                var lastBytesRead = 0L

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= 200) {
                        val progress = if (fileLength > 0) ((totalBytesRead * 100) / fileLength).toInt() else 0
                        val speed = if (currentTime > lastUpdateTime) {
                            ((totalBytesRead - lastBytesRead) * 1000 / (currentTime - lastUpdateTime) / 1024).toInt()
                        } else 0

                        runOnUiThread {
                            downloadProgressBar.progress = progress
                            dialog.findViewById<TextView>(R.id.progress_text)?.text =
                                getString(R.string.progress_text, progress, speed)
                        }
                        lastBytesRead = totalBytesRead
                        lastUpdateTime = currentTime
                    }
                }

                outputStream.flush()

                runOnUiThread {
                    dialog.findViewById<LinearLayout>(R.id.progress_container)?.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.download_completed), Toast.LENGTH_SHORT).show()

                    val contentUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        apkFile
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        !packageManager.canRequestPackageInstalls()
                    ) {
                        val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:$packageName"))
                        installPermissionLauncher.launch(permissionIntent)
                        sharedPref.edit().putString("pending_apk_uri", contentUri.toString()).apply()
                    } else {
                        installApk(contentUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("BLESpam", "Download failed: ${e.message}", e)
                runOnUiThread {
                    dialog.findViewById<LinearLayout>(R.id.progress_container)?.visibility = View.GONE
                    Toast.makeText(
                        this,
                        getString(R.string.download_failed, e.message ?: getString(R.string.unknown_error)),
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
            } finally {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
                progressHandler.removeCallbacksAndMessages(null)
            }
        }.start()
    }

    private fun showUpdateDialog(title: String, message: String, isForced: Boolean, downloadUrl: String) {
        val dialogView = layoutInflater.inflate(R.layout.activity_update, null)
        downloadProgressBar = dialogView.findViewById<ProgressBar>(R.id.download_progress)
            ?: throw IllegalStateException(getString(R.string.progress_bar_not_found))
        val progressContainer = dialogView.findViewById<LinearLayout>(R.id.progress_container)
            ?: throw IllegalStateException(getString(R.string.progress_container_not_found))
        val progressText = dialogView.findViewById<TextView>(R.id.progress_text)
            ?: throw IllegalStateException(getString(R.string.progress_text_not_found))

        dialogView.findViewById<TextView>(R.id.update_title).text = title
        dialogView.findViewById<TextView>(R.id.update_notes).text = message

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(!isForced)
            .create()

        dialogView.findViewById<Button>(R.id.btn_update).setOnClickListener {
            downloadApk(downloadUrl, dialog)
        }

        dialogView.findViewById<Button>(R.id.btn_later).setOnClickListener {
            if (!isForced) {
                dialog.dismiss()
            }
        }

        if (isForced) {
            dialogView.findViewById<Button>(R.id.btn_later).visibility = View.GONE
        }

        dialog.setOnDismissListener {
            progressHandler.removeCallbacksAndMessages(null)
        }

        try {
            dialog.show()
            dialog.window?.apply {
                setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_dialog_background))
                val params = attributes
                params.width = WindowManager.LayoutParams.MATCH_PARENT
                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.CENTER
                attributes = params
                decorView.setPadding(0, 0, 0, 0)
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, getString(R.string.update_dialog_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.with(this).clear(binding.logo)
        stopAllSpammers()
        progressHandler.removeCallbacksAndMessages(null)
    }

    private fun installApk(uri: Uri) {
        Log.d("BLESpam", "Installing APK with URI: $uri, Scheme: ${uri.scheme}")
        try {
            if (uri.scheme != "content") {
                throw IllegalArgumentException("Expected content URI, got: ${uri.scheme}")
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val externalApkFile = File(downloadsDir, "temp_installer_${System.currentTimeMillis()}.apk")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(externalApkFile).use { output ->
                    val bytesCopied = input.copyTo(output)
                    Log.d("BLESpam", "Bytes copied to external file: $bytesCopied")
                }
            } ?: throw IOException("Failed to open input stream for URI: $uri")

            if (!externalApkFile.exists() || externalApkFile.length() == 0L) {
                throw IllegalStateException("Invalid APK file: exists=${externalApkFile.exists()}, size=${externalApkFile.length()}")
            }

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageArchiveInfo(externalApkFile.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageArchiveInfo(externalApkFile.absolutePath, 0)
            } ?: throw IllegalStateException("Invalid APK file: Unable to read package info")

            Log.d("BLESpam", "APK package: ${packageInfo.packageName}, version: ${packageInfo.versionName}")

            val apkUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                externalApkFile
            )
            Log.d("BLESpam", "FileProvider URI: $apkUri, File path: ${externalApkFile.absolutePath}")

            val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                startActivity(installIntent)
                Log.d("BLESpam", "Install intent started successfully")
            } catch (e: ActivityNotFoundException) {
                Log.e("BLESpam", "No app to handle install intent: ${e.message}")
                showInstallError(getString(R.string.no_install_app))
            } catch (e: SecurityException) {
                Log.e("BLESpam", "Security exception during install: ${e.message}")
                showInstallError(getString(R.string.install_permission_denied))
            }
        } catch (e: Exception) {
            Log.e("BLESpam", "Error preparing APK file: ${e.message}", e)
            showInstallError(getString(R.string.installation_error, e.message ?: getString(R.string.unknown_error)))
        }
    }

    private fun showInstallError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
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
        return bluetoothAdapter?.isEnabled == true
    }

    private fun promptToEnableBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                2
            )
            return
        }
        if (!hasPermission(Manifest.permission.BLUETOOTH)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH),
                2
            )
            return
        }

        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateLogoAnimation() {
        if (isFinishing || isDestroyed) return

        val isAnimationEnabled = sharedPref.getBoolean("logo_animation", false)
        val isAnySpammerActive = spammerList.any { it.isSpamming }

        if (isAnimationEnabled && isAnySpammerActive) {
            Glide.with(this)
                .load(R.drawable.anim)
                .into(binding.logo)
        } else {
            Glide.with(this)
                .load(R.drawable.logo)
                .into(binding.logo)
        }
    }

    private fun onClickSpamButton(spammer: Spammer, button: Button, circle: ImageView) {
        if (!spammerList.contains(spammer)) {
            spammerList.add(spammer)
        }
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
            updateLogoAnimation()
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
                    updateLogoAnimation()
                    return
                }
                if (spammer.isSpamming) {
                    imageView.visibility = if (imageView.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                    handler.postDelayed(
                        this,
                        if (imageView.visibility == View.VISIBLE) (Helper.delay / 10).coerceAtLeast(20).toLong()
                        else Helper.delay.toLong()
                    )
                } else {
                    imageView.visibility = View.VISIBLE
                    handler.postDelayed(this, 200)
                }
            }
        }
        handler.postDelayed(blinkRunnable, 100)
        return blinkRunnable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = getSharedPreferences("AppSettings", MODE_PRIVATE)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bugButton: ImageView = findViewById(R.id.settingsButton)
        bugButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val socialLink = findViewById<TextView>(R.id.socialLink)
        socialLink.paintFlags = socialLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        if (!hasPermission(Manifest.permission.BLUETOOTH) ||
            !hasPermission(Manifest.permission.BLUETOOTH_ADMIN) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE))
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    *if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_ADVERTISE) else arrayOf()
                ),
                1
            )
        } else {
            checkForNewVersion()
            if (Helper.isPermissionGranted(this)) {
                initializeSpamButtons()
                setupDelayButtons()
            }
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
            onClickSpamButton(SwiftPairSpam(), binding.windowsSwiftPairButton, binding.windowsSwiftPairCircle)
        } catch (e: IOException) {
            Toast.makeText(this, getString(R.string.swiftpair), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDelayButtons() {
        binding.minusDelayButton.setOnClickListener {
            val i = Helper.delays.indexOf(Helper.delay)
            if (i > 0) {
                Helper.delay = Helper.delays[i - 1]
                binding.delayText.text = getString(R.string.delay_text, Helper.delay)
            }
        }

        binding.plusDelayButton.setOnClickListener {
            val i = Helper.delays.indexOf(Helper.delay)
            if (i < Helper.delays.size - 1) {
                Helper.delay = Helper.delays[i + 1]
                binding.delayText.text = getString(R.string.delay_text, Helper.delay)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    checkForNewVersion()
                    if (Helper.isPermissionGranted(this)) {
                        initializeSpamButtons()
                        setupDelayButtons()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.permissions_denied), Toast.LENGTH_LONG).show()
                }
            }
            2 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    promptToEnableBluetooth()
                } else {
                    Toast.makeText(this, getString(R.string.bluetooth_permission_denied), Toast.LENGTH_LONG).show()
                }
            }
            3 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkForNewVersion()
                } else {
                    Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        private const val INSTALL_PERMISSION_REQUEST_CODE = 1001
    }
}