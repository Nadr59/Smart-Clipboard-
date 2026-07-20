package com.smart.clipboard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smart.clipboard.MainActivity
import com.smart.clipboard.data.AppDatabase
import com.smart.clipboard.data.ClipboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClipboardService : Service() {

    private lateinit var clipboardManager: ClipboardManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSeenText: String? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // مهمة تكرارية: فحص الحافظة كل 15 ثانية كطبقة حماية
    private val periodicCheck = object : Runnable {
        override fun run() {
            checkClipboardQuietly()
            mainHandler.postDelayed(this, 15_000)
        }
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d("ClipboardDebug", "Service listener fired!")
        mainHandler.postDelayed({
            readAndSaveClipboard()
        }, 300)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("ClipboardDebug", "=== ClipboardService CREATED ===")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)

        // حفظ النص الحالي كمرجع
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                lastSeenText = clip.getItemAt(0).text?.toString()
            }
        } catch (_: Exception) {}

        startForegroundServiceWithNotification()
        acquireWakeLock()

        // بدء الفحص الدوري
        mainHandler.postDelayed(periodicCheck, 15_000)
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SmartClipboard::ServiceWakeLock"
            )
            wakeLock?.acquire(24 * 60 * 60 * 1000L) // 24 ساعة
            Log.d("ClipboardDebug", "WakeLock acquired")
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "WakeLock error", e)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "clipboard_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "خدمة الحافظة الذكية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "مراقبة عمليات النسخ في الخلفية"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("الحافظة الذكية")
            .setContentText("تتم مراقبة النسخ في الخلفية")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun checkClipboardQuietly() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text != lastSeenText) {
                    lastSeenText = text
                    saveToDatabase(text)
                }
            }
        } catch (_: Exception) {}
    }

    private fun readAndSaveClipboard() {
        try {
            val clipData = clipboardManager.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text != lastSeenText) {
                    lastSeenText = text
                    saveToDatabase(text)
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Service read error", e)
        }
    }

    private fun saveToDatabase(text: String) {
        serviceScope.launch {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).clipboardDao()
                val latest = dao.getLatestItem()
                if (latest == null || latest.text != text) {
                    dao.insertItem(
                        ClipboardItem(text = text, timestamp = System.currentTimeMillis())
                    )
                    dao.trimOldItems(100)
                    Log.d("ClipboardDebug", "Service SAVED: $text")
                }
            } catch (e: Exception) {
                Log.e("ClipboardDebug", "Service DB error", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ClipboardDebug", "onStartCommand called")
        return START_STICKY
    }

    override fun onBind(intent: IBinder?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ClipboardDebug", "=== ClipboardService DESTROYED ===")

        mainHandler.removeCallbacksAndMessages(null)

        try {
            clipboardManager.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}

        serviceJob.cancel()

        // إعادة تشغيل الخدمة تلقائياً
        restartService()
    }

    private fun restartService() {
        try {
            val restartIntent = Intent(applicationContext, ClipboardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
            Log.d("ClipboardDebug", "Service restart scheduled")
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Restart error", e)
        }
    }
}
