package com.tutozz.blespam

import android.content.Context
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
import com.tutozz.blespam.AppConfig.BUG_REPORT_API
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class BugReportActivity : AppCompatActivity() {

    private val MAX_DESCRIPTION_LENGTH = 1000
    private val SPAM_COOLDOWN_MS = 60_000L
    private val PREFS_NAME = "BugReportPrefs"
    private val PREF_LAST_REPORT_TIME = "lastReportTime"
    private lateinit var sendButton: Button
    private lateinit var errorText: TextView
    private val handler = Handler(Looper.getMainLooper())

    private fun getAppLanguage(): String {
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "en") ?: "en"
        // Validate language code (ensure it’s a 2- or 3-letter ISO 639-1 code)
        return if (language.matches(Regex("[a-z]{2,3}"))) language else "en"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug)
        title = getString(R.string.bug_report_title)

        val reportEditText = findViewById<EditText>(R.id.editText)
        sendButton = findViewById<Button>(R.id.sendButton)
        val charCountText = findViewById<TextView>(R.id.charCountText)
        errorText = findViewById<TextView>(R.id.errorText)

        charCountText.text = getString(R.string.char_counter_format, 0, MAX_DESCRIPTION_LENGTH)

        // Check cooldown and start auto-enable timer
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
                        charCountText.setTextColor(Color.parseColor("#FF6600")) // Orange
                        updateButtonState()
                    }
                    else -> {
                        charCountText.setTextColor(Color.parseColor("#666666"))
                        updateButtonState()
                    }
                }
                // Clear error text when user types
                clearError()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        sendButton.setOnClickListener {
            val description = reportEditText.text.toString().trim()

            if (description.isEmpty()) {
                showError(getString(R.string.error_empty_description))
                return@setOnClickListener
            }

            if (description.length > MAX_DESCRIPTION_LENGTH) {
                showError(getString(R.string.char_limit_exceeded, MAX_DESCRIPTION_LENGTH))
                return@setOnClickListener
            }

            // Check cooldown before sending
            if (isCooldownActive()) {
                val remainingSeconds = getRemainingCooldownSeconds()
                showError(getString(R.string.cooldown_error, remainingSeconds))
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                    val language = getAppLanguage()
                    val appVersion = getAppVersion()
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

                    sendButton.isEnabled = false
                    sendButton.text = getString(R.string.sending_button_text)
                    clearError()

                    val success = sendReportToServer(
                        description = description,
                        model = deviceModel,
                        language = language,
                        version = appVersion,
                        timestamp = timestamp
                    )

                    if (success) {
                        // Save the timestamp of the successful report
                        saveLastReportTime()
                        // Show success message
                        errorText.text = getString(R.string.report_sent_success)
                        errorText.setTextColor(Color.parseColor("#008000")) // Green for success
                        errorText.visibility = View.VISIBLE
                        reportEditText.text.clear()
                        // Navigate back to previous screen after a short delay
                        handler.postDelayed({ finish() }, 2000)
                    }
                } catch (e: Exception) {
                    showError(getString(R.string.connection_error, e.message ?: getString(R.string.unknown_error)))
                } finally {
                    sendButton.text = getString(R.string.send_button_text)
                    updateButtonState()
                }
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
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
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastReportTime) < SPAM_COOLDOWN_MS
    }

    private fun getRemainingCooldownSeconds(): Long {
        val lastReportTime = getLastReportTime()
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastReportTime
        return if (elapsed < SPAM_COOLDOWN_MS) {
            ((SPAM_COOLDOWN_MS - elapsed) / 1000).coerceAtLeast(1)
        } else {
            0
        }
    }

    private fun updateButtonState() {
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
                updateButtonState()
            }, remainingTime)
        }
    }

    private suspend fun sendReportToServer(
        description: String,
        model: String,
        language: String,
        version: String,
        timestamp: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // Create JSON object with data
                val jsonData = JSONObject().apply {
                    put("description", description.take(MAX_DESCRIPTION_LENGTH))
                    put("model", model.take(100))
                    put("language", language.take(10))
                    put("version", version.take(50))
                    put("timestamp", timestamp)
                }

                // Set up connection
                connection = (URL(BUG_REPORT_API).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                // Send data
                connection.outputStream.use { output ->
                    output.bufferedWriter(Charsets.UTF_8).use {
                        it.write(jsonData.toString())
                        it.flush()
                    }
                }

                // Check response
                val responseCode = connection.responseCode
                val responseBody = when {
                    responseCode in 200..299 -> {
                        connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    }
                    else -> {
                        connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: connection.inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: "No response details"
                    }
                }

                // Parse response body as JSON
                val jsonResponse = try {
                    JSONObject(responseBody)
                } catch (e: Exception) {
                    JSONObject().apply {
                        put("status", "error")
                        put("message", "Invalid server response: ${e.message}")
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_OK && jsonResponse.optString("status") == "success") {
                    true
                } else {
                    val errorMessage = jsonResponse.optString("message", "Server error: $responseCode")
                    withContext(Dispatchers.Main) {
                        showError(getString(R.string.server_error, errorMessage))
                    }
                    false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError(getString(R.string.connection_error, e.message ?: getString(R.string.unknown_error)))
                }
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.setTextColor(Color.RED)
        errorText.visibility = View.VISIBLE
        // Optionally clear the error after a delay
        handler.postDelayed({ clearError() }, 5000)
    }

    private fun clearError() {
        errorText.text = ""
        errorText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handler callbacks to prevent memory leaks
        handler.removeCallbacksAndMessages(null)
    }
}