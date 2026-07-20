package com.smart.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.smart.clipboard.adapter.ClipboardAdapter
import com.smart.clipboard.data.ClipboardItem
import com.smart.clipboard.service.ClipboardService
import com.smart.clipboard.viewmodel.ClipboardViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: ClipboardViewModel
    private lateinit var adapter: ClipboardAdapter
    private lateinit var tvEmpty: TextView
    private var setupDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvEmpty = findViewById(R.id.tvEmpty)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val fabClear = findViewById<FloatingActionButton>(R.id.fabClear)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ClipboardAdapter(
            onCopyClick = { text -> copyToClipboard(text) },
            onPinClick = { item -> togglePin(item) },
            onDeleteClick = { item -> viewModel.delete(item) }
        )
        recyclerView.adapter = adapter

        viewModel = ViewModelProvider(this)[ClipboardViewModel::class.java]
        viewModel.allItems.observe(this) { items ->
            adapter.submitList(items)
            if (items.isEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        fabClear.setOnClickListener {
            viewModel.deleteAllUnpinned()
            Toast.makeText(this, "تم مسح السجل غير المثبت", Toast.LENGTH_SHORT).show()
        }

        startClipboardService()
    }

    override fun onResume() {
        super.onResume()
        checkAllRequirements()
    }

    private fun checkAllRequirements() {
        // 1. التحقق من خدمة إمكانية الوصول
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityDialog()
            return
        }

        // 2. التحقق من تجاهل تحسين البطارية
        if (!isBatteryOptimizationIgnored()) {
            showBatteryOptimizationDialog()
            return
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val serviceName = "${packageName}/.service.ClipboardAccessibilityService"
            val settingValue = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return settingValue.split(":").any {
                it.equals(serviceName, ignoreCase = true)
            }
        } catch (e: Exception) {
            return false
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } catch (e: Exception) {
            false
        }
    }

    private fun showAccessibilityDialog() {
        if (setupDialogShown) return
        setupDialogShown = true

        AlertDialog.Builder(this)
            .setTitle("الخطوة 1: تفعيل خدمة المراقبة")
            .setMessage(
                "لمراقبة عمليات النسخ، يجب تفعيل خدمة إمكانية الوصول.\n\n" +
                "الخطوات:\n" +
                "1. اضغط 'فتح الإعدادات'\n" +
                "2. ابحث عن 'الحافظة الذكية'\n" +
                "3. فعّل الخدمة\n" +
                "4. ارجع للتطبيق"
            )
            .setPositiveButton("فتح الإعدادات") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "لا يمكن فتح الإعدادات", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("إلغاء") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { setupDialogShown = false }
            .setCancelable(true)
            .show()
    }

    private fun showBatteryOptimizationDialog() {
        if (setupDialogShown) return
        setupDialogShown = true

        AlertDialog.Builder(this)
            .setTitle("الخطوة 2: السماح بالعمل في الخلفية")
            .setMessage(
                "لضمان استمرار العمل في الخلفية، يجب السماح للتطبيق بتجاهل تحسين البطارية.\n\n" +
                "بدون هذا الإذن، سيتوقف التطبيق عن العمل بعد فترة."
            )
            .setPositiveButton("فتح الإعدادات") { _, _ ->
                try {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e2: Exception) {
                        Toast.makeText(this, "لا يمكن فتح الإعدادات", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("إلغاء") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { setupDialogShown = false }
            .setCancelable(true)
            .show()
    }

    private fun startClipboardService() {
        val serviceIntent = Intent(this, ClipboardService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Smart Clipboard", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "تم نسخ النص إلى الحافظة", Toast.LENGTH_SHORT).show()
    }

    private fun togglePin(item: ClipboardItem) {
        val updatedItem = item.copy(isPinned = !item.isPinned)
        viewModel.update(updatedItem)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Toast.makeText(this, "الإعدادات قيد التطوير", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
