package com.smart.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

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
