package com.imanage.fileexplorer.ui.screens.viewer

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val mediaController = MediaController(ctx)
                        mediaController.setAnchorView(this)
                        setMediaController(mediaController)
                        setVideoURI(Uri.fromFile(file))
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
