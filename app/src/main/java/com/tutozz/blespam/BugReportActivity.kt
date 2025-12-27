package com.tutozz.blespam

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tutozz.blespam.AppConfig.BUG_REPORT_API
import com.tutozz.blespam.AppConfig.TOKEN_API
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.tutozz.blespam.R
import android.util.Log
import android.provider.Settings
import java.security.MessageDigest

class BugReportActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BugReportActivity"
        private const val MAX_DESCRIPTION_LENGTH = 500
        private const val SPAM_COOLDOWN_MS = 60_000L
        private const val PREFS_NAME = "BugReportPrefs"
        private const val PREF_LAST_REPORT_TIME = "lastReportTime"
        private const val PREF_DEVICE_ID = "deviceId"
    }

    private lateinit var sharedPref: SharedPreferences
    private lateinit var sendButton: Button
    private lateinit var errorText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val useMaterialDesign: Boolean
        get() = sharedPref.getBoolean("use_material", defaultUseMaterial())

    private fun defaultUseMaterial(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val layoutRes = if (useMaterialDesign) {
            R.layout.activity_bug_material
        } else {
            R.layout.activity_bug_legacy
        }

        super.onCreate(savedInstanceState)
        setContentView(layoutRes)

        title = getString(R.string.bug_report_title)

        val reportEditText = findViewById<EditText>(R.id.editText)
        sendButton = findViewById<Button>(R.id.sendButton)
        val charCountText = findViewById<TextView>(R.id.charCountText)
        errorText = findViewById<TextView>(R.id.errorText)

        charCountText.text = getString(R.string.char_counter_format, 0, MAX_DESCRIPTION_LENGTH)

        updateButtonState()
        startCooldownTimer()

        reportEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                charCountText.text = getString(R.string.char_counter_format, currentLength, MAX_DESCRIPTION_LENGTH)

                when {
                    currentLength > MAX_DESCRIPTION_LENGTH -> {
                        charCountText.setTextColor(Color.RED)
                        sendButton.isEnabled = false
                        sendButton.alpha = 0.5f
                    }
                    currentLength > MAX_DESCRIPTION_LENGTH * 0.9 -> {
                        charCountText.setTextColor(Color.parseColor("#FF6600"))
                        updateButtonState()
                    }
                    else -> {
                        charCountText.setTextColor(Color.parseColor("#666666"))
                        updateButtonState()
                    }
                }
                clearError()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sendButton.setOnClickListener {
            val rawDescription = reportEditText.text.toString().trim()

            if (rawDescription.isEmpty()) {
                showError(getString(R.string.error_empty_description))
                return@setOnClickListener
            }

            val description = sanitizeInput(rawDescription)
            if (description.length > MAX_DESCRIPTION_LENGTH) {
                showError(getString(R.string.char_limit_exceeded, MAX_DESCRIPTION_LENGTH))
                return@setOnClickListener
            }

            if (isCooldownActive()) {
                val remainingSeconds = getRemainingCooldownSeconds()
                showError(getString(R.string.cooldown_error, remainingSeconds))
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    if (isFinishing || isDestroyed) return@launch

                    sendButton.isEnabled = false
                    sendButton.text = getString(R.string.sending_button_text)
                    clearError()

                    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".take(100)
                    val language = getAppLanguage()
                    val appVersion = getAppVersion()
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                    val deviceId = getOrCreateDeviceId()

                    val token = getTokenFromServer(deviceModel, language, appVersion, timestamp, deviceId)
                    if (token == null) return@launch

                    val success = sendReportToServerWithToken(description, token)
                    if (success) {
                        saveLastReportTime()
                        if (!isFinishing && !isDestroyed) {
                            errorText.text = getString(R.string.report_sent_success)
                            errorText.setTextColor(Color.parseColor("#008000"))
                            errorText.visibility = View.VISIBLE
                            reportEditText.text.clear()

                            handler.postDelayed({
                                if (!isFinishing && !isDestroyed) finish()
                            }, 2000)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send bug report", e)
                    if (!isFinishing && !isDestroyed) {
                        showError(getString(R.string.connection_error_generic))
                    }
                } finally {
                    if (!isFinishing && !isDestroyed) {
                        sendButton.text = getString(R.string.send_button_text)
                        updateButtonState()
                    }
                }
            }
        }
    }

    private fun sanitizeInput(input: String): String {
        return input
            .replace("<", "<")
            .replace(">", ">")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("\n", " ")
            .replace("\r", " ")
    }

    private fun getAppLanguage(): String {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        return if (language.matches(Regex("^[a-z]{2,3}\$"))) language else "en"
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(PREF_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(PREF_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    private fun saveLastReportTime() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(PREF_LAST_REPORT_TIME, System.currentTimeMillis()).apply()
    }

    private fun getLastReportTime(): Long {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(PREF_LAST_REPORT_TIME, 0L)
    }

    private fun isCooldownActive(): Boolean {
        val lastReportTime = getLastReportTime()
        return (System.currentTimeMillis() - lastReportTime) < SPAM_COOLDOWN_MS
    }

    private fun getRemainingCooldownSeconds(): Long {
        val lastReportTime = getLastReportTime()
        val elapsed = System.currentTimeMillis() - lastReportTime
        return if (elapsed < SPAM_COOLDOWN_MS) ((SPAM_COOLDOWN_MS - elapsed) / 1000).coerceAtLeast(1) else 0
    }

    private fun updateButtonState() {
        if (isFinishing || isDestroyed) return
        val descriptionLength = findViewById<EditText>(R.id.editText).text.length
        val isValidLength = descriptionLength in 1..MAX_DESCRIPTION_LENGTH
        val isCooldownOver = !isCooldownActive()
        sendButton.isEnabled = isValidLength && isCooldownOver
        sendButton.alpha = if (isValidLength && isCooldownOver) 1f else 0.5f
    }

    private fun startCooldownTimer() {
        if (isCooldownActive()) {
            val remainingTime = SPAM_COOLDOWN_MS - (System.currentTimeMillis() - getLastReportTime())
            handler.postDelayed({
                if (!isFinishing && !isDestroyed) updateButtonState()
            }, remainingTime)
        }
    }

    private suspend fun getTokenFromServer(
        model: String,
        language: String,
        version: String,
        timestamp: String,
        deviceId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"

            val deviceFingerprint = Build.FINGERPRINT.take(100)
            val board = Build.BOARD.take(50)
            val brand = Build.BRAND.take(30)
            val device = Build.DEVICE.take(30)

            val nonce = UUID.randomUUID().toString()

            val combined = "$androidId|$model|$language|$version|$timestamp|$nonce|$deviceFingerprint"
            val hash = sha256(combined)

            val json = JSONObject().apply {
                put("model", model.take(100))
                put("language", language.take(10))
                put("version", version.take(50))
                put("timestamp", timestamp)
                put("android_id", androidId.take(16))
                put("fingerprint", deviceFingerprint)
                put("board", board)
                put("brand", brand)
                put("device", device)
                put("nonce", nonce)
                put("hash", hash)
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(TOKEN_API)
                .post(requestBody)
                .addHeader("User-Agent", "BleSpamApp/${version} (${Build.MODEL})")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("status", "error")
                        put("message", "Invalid JSON")
                    }
                }

                if (response.isSuccessful && jsonResponse.optString("status") == "success") {
                    jsonResponse.optString("token", null)
                } else {
                    val msg = jsonResponse.optString("message", "Unknown error")
                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) {
                            showError("Token error: $msg")
                        }
                    }
                    null
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (!isFinishing && !isDestroyed) {
                    showError(getString(R.string.connection_error, e.message ?: getString(R.string.unknown_error)))
                }
            }
            null
        }
    }

    private fun sha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "invalid_hash"
        }
    }

    private suspend fun sendReportToServerWithToken(description: String, token: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("description", description)
                }

                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(BUG_REPORT_API)
                    .post(requestBody)
                    .header("Authorization", "Bearer $token")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        true
                    } else {
                        withContext(Dispatchers.Main) {
                            if (!isFinishing && !isDestroyed) showError(getString(R.string.server_error_generic))
                        }
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Report sending failed", e)
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) showError(getString(R.string.connection_error_generic))
                }
                false
            }
        }

    private fun showError(message: String) {
        if (isFinishing || isDestroyed) return
        errorText.text = message
        errorText.setTextColor(Color.RED)
        errorText.visibility = View.VISIBLE
        handler.postDelayed({ if (!isFinishing && !isDestroyed) clearError() }, 5000)
    }

    private fun clearError() {
        if (isFinishing || isDestroyed) return
        errorText.text = ""
        errorText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}