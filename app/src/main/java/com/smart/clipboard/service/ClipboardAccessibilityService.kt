package com.smart.clipboard.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.smart.clipboard.data.AppDatabase
import com.smart.clipboard.data.ClipboardItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClipboardAccessibilityService : AccessibilityService() {

    private lateinit var clipboardManager: ClipboardManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSeenText: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d("ClipboardDebug", "Listener fired!")
        // تأخير 200ms لأن أندرويد 10+ لا يسمح بالقراءة فوراً
        mainHandler.postDelayed({
            readAndSaveClipboard()
        }, 200)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ClipboardDebug", "=== Accessibility Service CONNECTED ===")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)

        // حفظ النص الحالي كمرجع
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                lastSeenText = clip.getItemAt(0).text?.toString()
                Log.d("ClipboardDebug", "Initial text: $lastSeenText")
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Init error", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // نتحقق من الحافظة عند أي حدث نصي
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            checkClipboardQuietly()
        }
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
            Log.d("ClipboardDebug", "Reading clipboard: $clipData")
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                Log.d("ClipboardDebug", "Text: $text")
                if (!text.isNullOrBlank() && text != lastSeenText) {
                    lastSeenText = text
                    saveToDatabase(text)
                } else {
                    Log.d("ClipboardDebug", "Skipped (duplicate or empty)")
                }
            } else {
                Log.d("ClipboardDebug", "clipData is null - trying delayed read")
                // محاولة أخرى بعد 500ms
                mainHandler.postDelayed({
                    trySecondRead()
                }, 500)
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Read error", e)
        }
    }

    private fun trySecondRead() {
        try {
            val clipData = clipboardManager.primaryClip
            Log.d("ClipboardDebug", "Second attempt: $clipData")
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                if (!text.isNullOrBlank() && text != lastSeenText) {
                    lastSeenText = text
                    saveToDatabase(text)
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Second read error", e)
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
                    Log.d("ClipboardDebug", "=== SAVED: $text ===")
                }
            } catch (e: Exception) {
                Log.e("ClipboardDebug", "DB error", e)
            }
        }
    }

    override fun onInterrupt() {
        Log.d("ClipboardDebug", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            clipboardManager.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}
        serviceJob.cancel()
        mainHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        var isRunning = false

        fun getServiceClassName(context: Context): String {
            return "${context.packageName}/.service.ClipboardAccessibilityService"
        }
    }

    init {
        isRunning = true
    }
}
