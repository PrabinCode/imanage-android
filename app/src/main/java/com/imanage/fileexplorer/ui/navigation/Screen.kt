package com.imanage.fileexplorer.ui.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    object Home : Screen("home")
    
    object Explorer : Screen("explorer?path={path}&title={title}&category={category}") {
        fun createRoute(path: String? = null, title: String? = null, category: String? = null): String {
            val encodedPath = path?.let { Uri.encode(it) } ?: ""
            val encodedTitle = title?.let { Uri.encode(it) } ?: ""
            val encodedCategory = category?.let { Uri.encode(it) } ?: ""
            return "explorer?path=$encodedPath&title=$encodedTitle&category=$encodedCategory"
        }
    }

    object Vault : Screen("vault")
    object Analyzer : Screen("analyzer")
    object Trash : Screen("trash")
    object Search : Screen("search")
    object Settings : Screen("settings")

    object TextEditor : Screen("text_editor?path={path}") {
        fun createRoute(path: String): String = "text_editor?path=${Uri.encode(path)}"
    }
}
