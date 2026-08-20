package com.imanage.fileexplorer.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.imanage.fileexplorer.IManageApp
import com.imanage.fileexplorer.data.model.FileType
import com.imanage.fileexplorer.data.security.AutoLockManager
import com.imanage.fileexplorer.ui.screens.analyzer.StorageAnalyzerScreen
import com.imanage.fileexplorer.ui.screens.analyzer.StorageAnalyzerViewModel
import com.imanage.fileexplorer.ui.screens.explorer.ExplorerScreen
import com.imanage.fileexplorer.ui.screens.explorer.ExplorerViewModel
import com.imanage.fileexplorer.ui.screens.home.HomeScreen
import com.imanage.fileexplorer.ui.screens.home.HomeViewModel
import com.imanage.fileexplorer.ui.screens.search.SearchScreen
import com.imanage.fileexplorer.ui.screens.search.SearchViewModel
import com.imanage.fileexplorer.ui.screens.security.PinLockScreen
import com.imanage.fileexplorer.ui.screens.security.PinMode
import com.imanage.fileexplorer.ui.screens.server.WifiShareScreen
import com.imanage.fileexplorer.ui.screens.settings.SettingsScreen
import com.imanage.fileexplorer.ui.screens.trash.TrashScreen
import com.imanage.fileexplorer.ui.screens.trash.TrashViewModel
import com.imanage.fileexplorer.ui.screens.vault.VaultScreen
import com.imanage.fileexplorer.ui.screens.vault.VaultViewModel
import com.imanage.fileexplorer.ui.screens.viewer.ImageViewerScreen
import com.imanage.fileexplorer.ui.screens.viewer.PdfViewerScreen
import com.imanage.fileexplorer.ui.screens.viewer.TextEditorScreen
import java.io.File

@Composable
fun AppNavGraph(
    navController: NavHostController,
    app: IManageApp
) {
    val context = LocalContext.current
    val isAppLocked by AutoLockManager.isLocked.collectAsState()

    fun openFile(filePath: String) {
        val file = File(filePath)
        val ext = file.extension.lowercase()

        // 1. PDF Viewer
        if (ext == "pdf") {
            navController.navigate(Screen.PdfViewer.createRoute(filePath))
            return
        }

        // 2. Image Viewer
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
        if (imageExtensions.contains(ext)) {
            navController.navigate(Screen.ImageViewer.createRoute(filePath))
            return
        }

        // 3. Text / Code Editor
        val codeExtensions = FileType.CODE.extensions + setOf("txt", "log", "json", "xml", "md", "csv", "conf", "prop", "ini", "rc", "gradle", "kts", "yaml", "yml")
        if (codeExtensions.contains(ext)) {
            navController.navigate(Screen.TextEditor.createRoute(filePath))
            return
        }

        // 4. Fallback Intent
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            navController.navigate(Screen.TextEditor.createRoute(filePath))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                val homeVm: HomeViewModel = viewModel(
                    factory = ViewModelFactory {
                        HomeViewModel(
                            app.fileSystemRepository,
                            app.trashRepository,
                            app.vaultRepository
                        )
                    }
                )
                HomeScreen(
                    viewModel = homeVm,
                    onNavigateToExplorer = { path, title ->
                        navController.navigate(Screen.Explorer.createRoute(path = path, title = title))
                    },
                    onNavigateToCategory = { type ->
                        navController.navigate(Screen.Explorer.createRoute(category = type.name))
                    },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToAnalyzer = { navController.navigate(Screen.Analyzer.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToWifiShare = { navController.navigate(Screen.WifiShare.route) },
                    onOpenFile = { openFile(it) }
                )
            }

            composable(
                route = Screen.Explorer.route,
                arguments = listOf(
                    navArgument("path") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("title") {
                        type = NavType.StringType
                        defaultValue = "Storage"
                    },
                    navArgument("category") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: "Storage"
                val category = backStackEntry.arguments?.getString("category") ?: ""
                val explorerVm: ExplorerViewModel = viewModel(
                    factory = ViewModelFactory {
                        ExplorerViewModel(
                            app.fileSystemRepository,
                            app.trashRepository,
                            app.vaultRepository
                        )
                    }
                )
                ExplorerScreen(
                    initialPath = path,
                    title = title,
                    categoryName = category.ifEmpty { null },
                    viewModel = explorerVm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFile = { openFile(it) }
                )
            }

            composable(Screen.Vault.route) {
                val vaultVm: VaultViewModel = viewModel(
                    factory = ViewModelFactory { VaultViewModel(app.vaultRepository) }
                )
                VaultScreen(
                    viewModel = vaultVm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFile = { openFile(it) }
                )
            }

            composable(Screen.Analyzer.route) {
                val analyzerVm: StorageAnalyzerViewModel = viewModel(
                    factory = ViewModelFactory { StorageAnalyzerViewModel(app.storageAnalyzerRepository) }
                )
                StorageAnalyzerScreen(
                    viewModel = analyzerVm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFile = { openFile(it) }
                )
            }

            composable(Screen.Trash.route) {
                val trashVm: TrashViewModel = viewModel(
                    factory = ViewModelFactory { TrashViewModel(app.trashRepository) }
                )
                TrashScreen(
                    viewModel = trashVm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Search.route) {
                val searchVm: SearchViewModel = viewModel(
                    factory = ViewModelFactory {
                        SearchViewModel(
                            app.fileSystemRepository,
                            app.trashRepository,
                            app.vaultRepository
                        )
                    }
                )
                SearchScreen(
                    viewModel = searchVm,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenFile = { openFile(it) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPinSetup = { navController.navigate(Screen.PinSetup.route) },
                    onNavigateToWifiShare = { navController.navigate(Screen.WifiShare.route) }
                )
            }

            composable(Screen.WifiShare.route) {
                WifiShareScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PinSetup.route) {
                PinLockScreen(
                    mode = PinMode.SETUP,
                    onSuccess = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.TextEditor.route,
                arguments = listOf(
                    navArgument("path") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                TextEditorScreen(
                    filePath = path,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.PdfViewer.route,
                arguments = listOf(
                    navArgument("path") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                PdfViewerScreen(
                    filePath = path,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ImageViewer.route,
                arguments = listOf(
                    navArgument("path") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                ImageViewerScreen(
                    filePath = path,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Overlay PIN Lock Screen when AutoLockManager triggers
        if (isAppLocked) {
            PinLockScreen(
                mode = PinMode.VERIFY,
                onSuccess = { AutoLockManager.unlock() }
            )
        }
    }
}

class ViewModelFactory<T>(val creator: () -> T) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : androidx.lifecycle.ViewModel> create(modelClass: Class<VM>): VM {
        return creator() as VM
    }
}
