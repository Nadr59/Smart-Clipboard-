package com.smart.clipboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smart.clipboard.data.AppDatabase
import com.smart.clipboard.data.ClipboardItem
import com.smart.clipboard.data.ClipboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ClipboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClipboardRepository
    
    private val searchQuery = MutableStateFlow("")

    val allItems: LiveData<List<ClipboardItem>>

    init {
        val clipboardDao = AppDatabase.getDatabase(application).clipboardDao()
        repository = ClipboardRepository(clipboardDao)
        
        allItems = searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allItems
            } else {
                repository.searchItems(query)
            }
        }.asLiveData()
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun insert(text: String) = viewModelScope.launch(Dispatchers.IO) {
        val item = ClipboardItem(text = text, timestamp = System.currentTimeMillis())
        repository.insert(item)
    }

    fun update(item: ClipboardItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(item)
    }

    fun delete(item: ClipboardItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(item)
    }

    fun deleteAllUnpinned() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAllUnpinned()
    }
}
