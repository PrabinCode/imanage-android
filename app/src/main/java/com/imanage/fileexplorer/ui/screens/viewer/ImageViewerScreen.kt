package com.imanage.fileexplorer.ui.screens.viewer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.imanage.fileexplorer.data.model.FileItem
import com.imanage.fileexplorer.ui.components.FileInfoDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val initialFile = remember(filePath) { File(filePath) }

    val imageExtensions = remember { setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif") }

    val imageFiles = remember(filePath) {
        val parent = initialFile.parentFile
        val list = parent?.listFiles()?.filter { f ->
            f.isFile && imageExtensions.contains(f.extension.lowercase())
        }?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }) ?: listOf(initialFile)
        if (list.isNotEmpty()) list else listOf(initialFile)
    }

    val initialIndex = remember(imageFiles, initialFile) {
        val idx = imageFiles.indexOfFirst { it.absolutePath == initialFile.absolutePath }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { imageFiles.size }
    )

    val currentFile = imageFiles.getOrNull(pagerState.currentPage) ?: initialFile
    val currentItem = remember(currentFile) { FileItem.fromFile(currentFile) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentFile.name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp)
                        Text(
                            text = "${pagerState.currentPage + 1} of ${imageFiles.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                    IconButton(onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                currentFile
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        } catch (e: Exception) { }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) { page ->
            val fileForPage = imageFiles[page]
            ZoomableImage(file = fileForPage)
        }
    }

    if (showInfoDialog) {
        FileInfoDialog(
            item = currentItem,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun ZoomableImage(file: File) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .crossfade(true)
                .build(),
            contentDescription = file.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}
