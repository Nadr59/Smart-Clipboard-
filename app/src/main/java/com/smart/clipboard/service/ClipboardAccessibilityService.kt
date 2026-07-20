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
    private var isListenerRegistered = false

    // مهمة تكرارية: إعادة تسجيل listener كل 30 ثانية
    private val reRegisterRunnable = object : Runnable {
        override fun run() {
            ensureListenerActive()
            mainHandler.postDelayed(this, 30_000) // كل 30 ثانية
        }
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d("ClipboardDebug", "Listener fired!")
        mainHandler.postDelayed({
            readAndSaveClipboard()
        }, 200)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ClipboardDebug", "=== Service CONNECTED ===")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        registerListener()

        // حفظ النص الحالي كمرجع
        readCurrentSilently()

        // بدء المهمة التكرارية
        mainHandler.postDelayed(reRegisterRunnable, 30_000)
    }

    private fun registerListener() {
        try {
            if (isListenerRegistered) {
                clipboardManager.removePrimaryClipChangedListener(clipListener)
            }
            clipboardManager.addPrimaryClipChangedListener(clipListener)
            isListenerRegistered = true
            Log.d("ClipboardDebug", "Listener registered")
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Register error", e)
        }
    }

    private fun unregisterListener() {
        try {
            if (isListenerRegistered) {
                clipboardManager.removePrimaryClipChangedListener(clipListener)
                isListenerRegistered = false
                Log.d("ClipboardDebug", "Listener unregistered")
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Unregister error", e)
        }
    }

    /**
     * التأكد أن listener لا يزال نشطاً
     */
    private fun ensureListenerActive() {
        try {
            Log.d("ClipboardDebug", "Re-registering listener (periodic check)")
            unregisterListener()
            registerListener()
            // تحقق من الحافظة أيضاً
            checkClipboardQuietly()
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Re-register error", e)
        }
    }

    private fun readCurrentSilently() {
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
        // نفحص الحافظة عند أي تغيير في النص
        checkClipboardQuietly()
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
            Log.d("ClipboardDebug", "Reading clipboard...")
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                Log.d("ClipboardDebug", "Text: $text")
                if (!text.isNullOrBlank() && text != lastSeenText) {
                    lastSeenText = text
                    saveToDatabase(text)
                }
            } else {
                Log.d("ClipboardDebug", "clipData null, retrying...")
                mainHandler.postDelayed({ trySecondRead() }, 500)
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Read error", e)
        }
    }

    private fun trySecondRead() {
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
        mainHandler.removeCallbacksAndMessages(null)
        unregisterListener()
        serviceJob.cancel()
        Log.d("ClipboardDebug", "=== Service DESTROYED ===")
    }
}
