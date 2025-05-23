package com.tutozz.blespam

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class BugReportActivity : AppCompatActivity() {

    private val botToken = "TOKEN"
    private val chatId = "CHATID"

    private var lastSendTime = 0L
    private val SPAM_INTERVAL = 30_000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug)

        val reportEditText = findViewById<EditText>(R.id.editText)
        val sendButton = findViewById<Button>(R.id.sendButton)

        sendButton.setOnClickListener {
            val description = reportEditText.text.toString().trim()
            if (description.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_description), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val now = System.currentTimeMillis()
            if (now - lastSendTime < SPAM_INTERVAL) {
                Toast.makeText(this, getString(R.string.please_wait), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lastSendTime = now

            CoroutineScope(Dispatchers.Main).launch {
                val publicIp = getPublicIpAddress() ?: "Unknown public IP"
                val message = buildMessage(this@BugReportActivity, description, publicIp)
                val success = sendBugReportToTelegram(message)
                if (success) {
                    Toast.makeText(this@BugReportActivity, getString(R.string.message_sent), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildMessage(context: Context, description: String, publicIp: String): String {
        val model = "${Build.MANUFACTURER} ${Build.MODEL}"
        val version = getAppVersion(context)
        val timestamp = getCurrentTimestamp()
        val language = Locale.getDefault().language

        return """
            🛠️ Bug report

            📱 Device: $model
            🌐 Public IP: $publicIp
            🗣 Language: $language
            📦 App version: $version
            🛑 Description: $description
            🕒 Time: $timestamp
        """.trimIndent()
    }

    private fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private suspend fun getPublicIpAddress(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.ipify.org")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.inputStream.bufferedReader().use {
                    it.readLine()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun sendBugReportToTelegram(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val url = URL("https://api.telegram.org/bot$botToken/sendMessage?chat_id=$chatId&text=$encodedMessage")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                withContext(Dispatchers.Main) {
                    if (responseCode != 200) {
                        Toast.makeText(this@BugReportActivity, getString(R.string.send_error, responseCode), Toast.LENGTH_LONG).show()
                    }
                }
                responseCode == 200
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BugReportActivity, getString(R.string.exception_error, e.message ?: "Unknown error"), Toast.LENGTH_LONG).show()
                }
                false
            }
        }
    }
}
