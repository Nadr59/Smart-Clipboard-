package com.smart.clipboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clipboard_items")
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val timestamp: Long,
    val isPinned: Boolean = false
)

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC")
    fun getAllItems(): Flow<List<ClipboardItem>>

    @Query("SELECT * FROM clipboard_items WHERE text LIKE '%' || :query || '%' ORDER BY isPinned DESC, timestamp DESC")
    fun searchItems(query: String): Flow<List<ClipboardItem>>

    @Insert
    suspend fun insertItem(item: ClipboardItem): Long

    @Update
    suspend fun updateItem(item: ClipboardItem)

    @Delete
    suspend fun deleteItem(item: ClipboardItem)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun deleteAllUnpinned()

    @Query("SELECT * FROM clipboard_items ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestItem(): ClipboardItem?

    @Query("DELETE FROM clipboard_items WHERE id NOT IN (SELECT id FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC LIMIT :maxLimit)")
    suspend fun trimOldItems(maxLimit: Int)
}

@Database(entities = [ClipboardItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clipboard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
