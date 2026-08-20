package com.imanage.fileexplorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val path: String,
    val title: String,
    val isDirectory: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalPath: String,
    val originalName: String,
    val trashedPath: String,
    val trashedTime: Long = System.currentTimeMillis(),
    val size: Long,
    val isDirectory: Boolean = false
)

@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val encryptedFileName: String,
    val originalFileName: String,
    val originalPath: String,
    val fileSize: Long,
    val encryptedAt: Long = System.currentTimeMillis(),
    val mimeType: String = "*/*",
    val isDirectory: Boolean = false
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
