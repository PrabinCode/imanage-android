package com.imanage.fileexplorer.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.imanage.fileexplorer.IManageApp
import com.imanage.fileexplorer.data.model.FileItem
import com.imanage.fileexplorer.data.model.FileType
import com.imanage.fileexplorer.data.security.AutoLockManager
import com.imanage.fileexplorer.ui.screens.analyzer.StorageAnalyzerScreen
import com.imanage.fileexplorer.ui.screens.analyzer.StorageAnalyzerViewModel
import com.imanage.fileexplorer.ui.screens.explorer.ExplorerScreen
import com.imanage.fileexplorer.ui.screens.explorer.ExplorerViewModel
import com.imanage.fileexplorer.ui.screens.home.HomeScreen
import com.imanage.fileexplorer.ui.screens.home.HomeViewModel
import com.imanage.fileexplorer.ui.screens.onboarding.OnboardingScreen
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
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import com.imanage.fileexplorer.data.archive.ArchiveEngine
import com.imanage.fileexplorer.ui.components.ExtractArchiveDialog
import android.widget.Toast
import com.imanage.fileexplorer.util.ExternalFileResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AppNavGraph(
    navController: NavHostController,
    app: IManageApp,
    incomingIntent: Intent? = null,
    onIntentHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("imanage_prefs", Context.MODE_PRIVATE) }
    val isOnboardingCompleted = remember { prefs.getBoolean("onboarding_completed", false) }
    val startDestination = remember { if (isOnboardingCompleted) Screen.Home.route else Screen.Onboarding.route }
    val isAppLocked by AutoLockManager.isLocked.collectAsState()
    val scope = rememberCoroutineScope()
    var archiveToExtract by remember { mutableStateOf<File?>(null) }
    var unsupportedFileToOpen by remember { mutableStateOf<File?>(null) }

    fun openFile(filePath: String, explicitMime: String? = null, isFromExternal: Boolean = false) {
        val file = File(filePath)
        val ext = file.extension.lowercase()
        val mime = explicitMime ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)

        // 1. Audio Files -> In-App Audio Player
        val audioExtensions = setOf("mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "wma")
        if (audioExtensions.contains(ext) || mime?.startsWith("audio/") == true) {
            com.imanage.fileexplorer.data.media.AudioPlaybackManager.playTrack(FileItem.fromFile(file))
            return
        }

        // 2. Video Files -> In-App Video Player
        val videoExtensions = setOf("mp4", "mkv", "webm", "mov", "3gp", "avi", "flv", "ts")
        if (videoExtensions.contains(ext) || mime?.startsWith("video/") == true) {
            navController.navigate(Screen.VideoPlayer.createRoute(filePath))
            return
        }

        // 3. PDF Viewer
        if (ext == "pdf" || mime == "application/pdf") {
            navController.navigate(Screen.PdfViewer.createRoute(filePath))
            return
        }

        // 4. Image Viewer
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "svg")
        if (imageExtensions.contains(ext) || mime?.startsWith("image/") == true) {
            navController.navigate(Screen.ImageViewer.createRoute(filePath))
            return
        }

        // 5. Text / Code Editor
        val codeExtensions = FileType.CODE.extensions + setOf("txt", "log", "json", "xml", "md", "csv", "conf", "prop", "ini", "rc", "gradle", "kts", "yaml", "yml")
        if (codeExtensions.contains(ext) || mime?.startsWith("text/") == true || mime in setOf("application/json", "application/xml", "application/javascript")) {
            navController.navigate(Screen.TextEditor.createRoute(filePath))
            return
        }

        // 6. APK Files -> Package Installer
        val apkExtensions = setOf("apk", "apks", "xapk")
        if (apkExtensions.contains(ext) || mime == "application/vnd.android.package-archive") {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                    try {
                        val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(manageIntent)
                        Toast.makeText(context, "Please allow IManage to install unknown apps, then tap the file again", Toast.LENGTH_LONG).show()
                        return
                    } catch (_: Exception) {}
                }

                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(installIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot install APK: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // 7. Archive Files -> Extraction
        val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "iso")
        if (archiveExtensions.contains(ext) || mime in setOf("application/zip", "application/x-zip-compressed", "application/x-rar-compressed", "application/x-7z-compressed", "application/x-tar", "application/gzip")) {
            archiveToExtract = file
            return
        }

        // 8. Other / Unsupported Files (Office docs, unknown binaries)
        if (isFromExternal) {
            unsupportedFileToOpen = file
        } else {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val mimeType = mime ?: "*/*"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open with"))
            } catch (e: Exception) {
                unsupportedFileToOpen = file
            }
        }
    }

    LaunchedEffect(incomingIntent) {
        val currentIntent = incomingIntent ?: return@LaunchedEffect
        val navigateTo = currentIntent.getStringExtra("navigate_to")
        if (navigateTo == "wifi_share") {
            navController.navigate(Screen.WifiShare.route)
        }

        val action = currentIntent.action
        if (action == Intent.ACTION_VIEW || action == Intent.ACTION_EDIT) {
            val uri = currentIntent.data ?: currentIntent.clipData?.let { clip ->
                if (clip.itemCount > 0) clip.getItemAt(0).uri else null
            }
            val mimeType = currentIntent.type

            if (uri != null) {
                val resolvedFile = withContext(Dispatchers.IO) {
                    ExternalFileResolver.resolveFile(context, uri, mimeType)
                }
                if (resolvedFile != null && resolvedFile.exists()) {
                    openFile(resolvedFile.absolutePath, explicitMime = mimeType, isFromExternal = true)
                } else {
                    Toast.makeText(context, "Unable to open file", Toast.LENGTH_SHORT).show()
                }
            }
        }
        onIntentHandled()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        prefs.edit().putBoolean("onboarding_completed", true).apply()
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    }
                )
            }

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
                    onOpenFile = { openFile(it) },
                    onNavigateToCategory = { type ->
                        navController.navigate(Screen.Explorer.createRoute(category = type.name))
                    },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToAnalyzer = { navController.navigate(Screen.Analyzer.route) },
                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                    onNavigateToWifiShare = { navController.navigate(Screen.WifiShare.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
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
                    onOpenFile = { openFile(it) },
                    onLaunchDuplicateCleaner = { navController.navigate(Screen.DuplicateCleaner.route) }
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
                    onNavigateToWifiShare = { navController.navigate(Screen.WifiShare.route) },
                    onNavigateToOnboarding = { navController.navigate(Screen.Onboarding.route) }
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

            composable(
                route = Screen.VideoPlayer.route,
                arguments = listOf(
                    navArgument("path") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString("path") ?: ""
                com.imanage.fileexplorer.ui.screens.viewer.VideoPlayerScreen(
                    filePath = path,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.DuplicateCleaner.route) {
                com.imanage.fileexplorer.ui.screens.analyzer.DuplicateCleanerScreen(
                    trashRepository = app.trashRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // Persistent Mini Audio Player Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            com.imanage.fileexplorer.ui.components.AudioPlayerBar()
        }

        // Archive extraction dialog when opened
        archiveToExtract?.let { archiveFile ->
            ExtractArchiveDialog(
                archiveName = archiveFile.name,
                onDismiss = {
                    val parent = archiveFile.parentFile
                    archiveToExtract = null
                    if (parent != null && parent.exists()) {
                        navController.navigate(Screen.Explorer.createRoute(path = parent.absolutePath, title = parent.name))
                    }
                },
                onConfirm = { password ->
                    val targetArchive = archiveFile
                    archiveToExtract = null
                    scope.launch {
                        Toast.makeText(context, "Extracting ${targetArchive.name}...", Toast.LENGTH_SHORT).show()
                        val parentDir = targetArchive.parentFile ?: Environment.getExternalStorageDirectory()
                        val destDir = File(parentDir, targetArchive.nameWithoutExtension)
                        val result = ArchiveEngine.extractArchive(targetArchive, destDir, password, context)
                        result.onSuccess {
                            Toast.makeText(context, "Extracted to ${destDir.name}", Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Explorer.createRoute(path = destDir.absolutePath, title = destDir.name))
                        }.onFailure { err ->
                            Toast.makeText(context, "Extraction failed: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }

        // Unsupported / binary file prompt
        unsupportedFileToOpen?.let { file ->
            AlertDialog(
                onDismissRequest = { unsupportedFileToOpen = null },
                title = { Text("Open ${file.name}", fontWeight = FontWeight.Bold) },
                text = {
                    val ext = file.extension.uppercase()
                    Text(
                        if (ext.isNotEmpty()) {
                            "IManage does not have a built-in viewer for .$ext files. Would you like to open it with another application or view it as text?"
                        } else {
                            "IManage does not have a built-in viewer for this file type. Would you like to open it with another application or view it as text?"
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val target = file
                        unsupportedFileToOpen = null
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                target
                            )
                            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(target.extension.lowercase()) ?: "*/*"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open with"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Open with Other App")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val target = file
                        unsupportedFileToOpen = null
                        navController.navigate(Screen.TextEditor.createRoute(target.absolutePath))
                    }) {
                        Text("View as Text")
                    }
                }
            )
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
