package com.tutozz.blespam

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.pm.ServiceInfo
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import com.tutozz.blespam.R

class SpamService : Service() {
    private val controllers = ConcurrentHashMap<String, SpammerController>()

    companion object {
        const val CHANNEL_ID = "SpamChannel"
        const val NOTIFICATION_ID = 1

        const val ACTION_STOP = "com.tutozz.blespam.ACTION_STOP"
        const val ACTION_ADD_SPAMMER = "com.tutozz.blespam.ACTION_ADD"
        const val ACTION_REMOVE_SPAMMER = "com.tutozz.blespam.ACTION_REMOVE"

        private val activeSpammers = ConcurrentHashMap<String, Boolean>()

        private const val EXTRA_SPAMMER_TYPE = "spammer_type"

        fun startSpammer(context: Context, spammerType: String) {
            activeSpammers[spammerType] = true

            val intent = Intent(context, SpamService::class.java).apply {
                action = ACTION_ADD_SPAMMER
                putExtra(EXTRA_SPAMMER_TYPE, spammerType)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun isSpammerRunning(spammerType: String): Boolean {
            return activeSpammers.containsKey(spammerType)
        }

        fun getActiveSpammers(): Set<String> {
            return activeSpammers.keys.toSet()
        }

        fun stopSpammer(context: Context, spammerType: String) {
            activeSpammers.remove(spammerType)

            val intent = Intent(context, SpamService::class.java).apply {
                action = ACTION_REMOVE_SPAMMER
                putExtra(EXTRA_SPAMMER_TYPE, spammerType)
            }

            context.startService(intent)
        }

        fun stopAllSpammers(context: Context) {
            activeSpammers.clear()

            val intent = Intent(context, SpamService::class.java).apply {
                action = ACTION_STOP
            }

            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ADD_SPAMMER -> {
                val type = intent.getStringExtra(EXTRA_SPAMMER_TYPE)
                if (type != null) {
                    activeSpammers[type] = true
                    startControllerForType(type)
                }
            }

            ACTION_REMOVE_SPAMMER -> {
                val type = intent.getStringExtra(EXTRA_SPAMMER_TYPE)
                if (type != null) {
                    activeSpammers.remove(type)
                    stopControllerForType(type)
                }

                if (activeSpammers.isEmpty()) {
                    stopSelf()
                    return START_NOT_STICKY
                }
            }

            ACTION_STOP -> {
                activeSpammers.clear()
                stopAllControllers()
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
            }
        }

        updateNotification()
        return START_STICKY
    }

    private fun startControllerForType(type: String) {
        if (controllers.containsKey(type)) return

        val controller = createControllerForType(type)
        if (controller != null) {
            controllers[type] = controller
            controller.start()
        } else {
            Log.w("SpamService", "No controller available for $type")
        }
    }

    private fun stopControllerForType(type: String) {
        controllers.remove(type)?.let { ctrl ->
            try { ctrl.stop() } catch (e: Exception) { Log.e("SpamService", "Error stopping controller $type", e) }
        }
    }

    private fun stopAllControllers() {
        for ((_, ctrl) in controllers) {
            try { ctrl.stop() } catch (e: Exception) { Log.e("SpamService", "Error stopping controller", e) }
        }
        controllers.clear()
    }

    private fun createControllerForType(type: String): SpammerController? {
        return when (type) {
            "iOS Crash" ->
                SpammerWrapper(ContinuitySpam(ContinuityType.ACTION, true))
            "Apple Action Modal" ->
                SpammerWrapper(ContinuitySpam(ContinuityType.ACTION, false))
            "Apple Device Popup" ->
                SpammerWrapper(ContinuitySpam(ContinuityType.DEVICE, false))
            "Apple 'Not Your Device'" ->
                SpammerWrapper(ContinuitySpam(ContinuityType.NOTYOURDEVICE, false))
            "Vzhuh Spam" ->
                SpammerWrapper(VzhuhSpam())
            "Android Fast Pair" ->
                SpammerWrapper(FastPairSpam())
            "Xiaomi Quick Connect" ->
                SpammerWrapper(XiaomiQuickConnect())
            "Samsung Buds" ->
                SpammerWrapper(EasySetupSpam(EasySetupDevice.type.BUDS))
            "Samsung Watch" ->
                SpammerWrapper(EasySetupSpam(EasySetupDevice.type.WATCH))
            "Windows Swift Pair" ->
                SpammerWrapper(SwiftPairSpam())
            else -> null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.ble_spam_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.ble_spam_channel_description)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun getMainActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getStopIntent(): PendingIntent {
        val intent = Intent(this, StopSpamReceiver::class.java).apply {
            action = StopSpamReceiver.ACTION_STOP
        }
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("ForegroundServiceType")
    private fun updateNotification() {
        val spammerList = activeSpammers.keys.toList()

        val iosSpammers = listOf(
            "iOS Crash",
            "Apple Action Modal",
            "Apple Device Popup",
            "Apple 'Not Your Device'",
            "Vzhuh Spam"
        )
        val androidSpammers = listOf("Android Fast Pair", "Xiaomi Quick Connect", "Samsung Buds", "Samsung Watch")
        val windowsSpammers = listOf("Windows Swift Pair")

        val activeIos = spammerList.filter { it in iosSpammers }
        val activeAndroid = spammerList.filter { it in androidSpammers }
        val activeWindows = spammerList.filter { it in windowsSpammers }

        val displayList = mutableListOf<String>()

        if (activeIos.size == iosSpammers.size) {
            displayList.add("All iOS")
        } else {
            displayList.addAll(activeIos)
        }

        if (activeAndroid.size == androidSpammers.size) {
            displayList.add("All Android")
        } else {
            displayList.addAll(activeAndroid)
        }

        if (activeWindows.isNotEmpty()) {
            displayList.add("Windows")
        }

        val finalDisplay = if (activeIos.size == iosSpammers.size &&
            activeAndroid.size == androidSpammers.size &&
            activeWindows.isNotEmpty()) {
            listOf("All Devices")
        } else {
            displayList.sorted()
        }

        val contentText = when {
            finalDisplay.isEmpty() -> getString(R.string.ble_spam_notification_no_spammers)
            finalDisplay.size == 1 -> getString(R.string.ble_spam_notification_active_one, finalDisplay[0])
            else -> finalDisplay.joinToString(", ")
        }

        val contentTitle = when {
            finalDisplay.isEmpty() -> getString(R.string.ble_spam_notification_title_idle)
            finalDisplay.size == 1 -> getString(R.string.ble_spam_notification_title_one)
            else -> resources.getQuantityString(
                R.plurals.ble_spam_notification_title_many,
                finalDisplay.size,
                finalDisplay.size
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(getMainActivityIntent())
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .addAction(
                R.drawable.ic_stop,
                getString(R.string.ble_spam_notification_action_stop),
                getStopIntent()
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    interface SpammerController {
        fun start()
        fun stop()
    }

    class SpammerWrapper(private val spammer: Spammer) : SpammerController {
        override fun start() = spammer.start()
        override fun stop() = spammer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeSpammers.clear()
    }
}
