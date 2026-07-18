package com.smart.clipboard.data

import kotlinx.coroutines.flow.Flow

class ClipboardRepository(private val clipboardDao: ClipboardDao) {

    val allItems: Flow<List<ClipboardItem>> = clipboardDao.getAllItems()

    fun searchItems(query: String): Flow<List<ClipboardItem>> {
        return clipboardDao.searchItems(query)
    }

    suspend fun insert(item: ClipboardItem) {
        val latest = clipboardDao.getLatestItem()
        // Deduplication: ignore if identical to the latest unpinned item
        if (latest != null && latest.text == item.text && !latest.isPinned) {
            return
        }
        clipboardDao.insertItem(item)
        // Trim history to keep max 100 items (excluding pinned items preference handling)
        clipboardDao.trimOldItems(100)
    }

    suspend fun update(item: ClipboardItem) {
        clipboardDao.updateItem(item)
    }

    suspend fun delete(item: ClipboardItem) {
        clipboardDao.deleteItem(item)
    }

    suspend fun deleteAllUnpinned() {
        clipboardDao.deleteAllUnpinned()
    }
}
