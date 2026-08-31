package com.imanage.fileexplorer.data.repository

import com.imanage.fileexplorer.data.local.dao.BookmarkDao
import com.imanage.fileexplorer.data.local.entity.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun addBookmark(path: String, title: String, isDirectory: Boolean = true) = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(
            BookmarkEntity(
                path = path,
                title = title,
                isDirectory = isDirectory,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeBookmark(path: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteByPath(path)
    }

    suspend fun isBookmarked(path: String): Boolean = withContext(Dispatchers.IO) {
        bookmarkDao.isBookmarked(path)
    }
}
