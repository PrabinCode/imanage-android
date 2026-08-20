package com.imanage.fileexplorer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.imanage.fileexplorer.data.local.dao.BookmarkDao
import com.imanage.fileexplorer.data.local.dao.SearchHistoryDao
import com.imanage.fileexplorer.data.local.dao.TrashDao
import com.imanage.fileexplorer.data.local.dao.VaultDao
import com.imanage.fileexplorer.data.local.entity.BookmarkEntity
import com.imanage.fileexplorer.data.local.entity.SearchHistoryEntity
import com.imanage.fileexplorer.data.local.entity.TrashEntity
import com.imanage.fileexplorer.data.local.entity.VaultEntity

@Database(
    entities = [
        BookmarkEntity::class,
        TrashEntity::class,
        VaultEntity::class,
        SearchHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookmarkDao(): BookmarkDao
    abstract fun trashDao(): TrashDao
    abstract fun vaultDao(): VaultDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "imanage_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
