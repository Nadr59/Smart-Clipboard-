package com.smart.clipboard.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
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
    private var lastSeenText: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        Log.d("ClipboardDebug", "Accessibility listener fired!")
        readAndSaveClipboard()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ClipboardDebug", "Accessibility service connected!")

        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipListener)

        // حفظ النص الحالي كمرجع لتجنب تكراره
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                lastSeenText = clip.getItemAt(0).text?.toString()
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Init read error", e)
        }

        // إشعار المستخدم أن الخدمة تعمل
        android.os.Handler(mainLooper).post {
            Toast.makeText(applicationContext, "خدمة مراقبة الحافظة تعمل الآن ✓", Toast.LENGTH_LONG).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // لا نحتاج حدث محدد - الـ listener يتكفل بكل شيء
    }

    override fun onInterrupt() {
        Log.d("ClipboardDebug", "Accessibility service interrupted")
    }

    private fun readAndSaveClipboard() {
        try {
            val clipData = clipboardManager.primaryClip
            Log.d("ClipboardDebug", "clipData: $clipData")
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString()
                Log.d("ClipboardDebug", "Text: $text, last: $lastSeenText")
                if (!text.isNullOrBlank() && text != lastSeenText) {
                    lastSeenText = text
                    saveToDatabase(text)
                }
            }
        } catch (e: Exception) {
            Log.e("ClipboardDebug", "Read error", e)
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
                    Log.d("ClipboardDebug", "SAVED: $text")
                }
            } catch (e: Exception) {
                Log.e("ClipboardDebug", "DB error", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            clipboardManager.removePrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {}
        serviceJob.cancel()
    }
}
