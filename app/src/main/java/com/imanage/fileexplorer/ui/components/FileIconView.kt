package com.imanage.fileexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.imanage.fileexplorer.data.model.FileItem
import com.imanage.fileexplorer.data.model.FileType

@Composable
fun FileIconView(
    item: FileItem,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isMedia = (item.fileType == FileType.IMAGE || item.fileType == FileType.VIDEO) && !item.isDirectory

    if (isMedia) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.file)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        val (icon, color) = when (item.fileType) {
            FileType.FOLDER -> Icons.Default.Folder to item.fileType.color
            FileType.IMAGE -> Icons.Default.Image to item.fileType.color
            FileType.VIDEO -> Icons.Default.VideoFile to item.fileType.color
            FileType.AUDIO -> Icons.Default.AudioFile to item.fileType.color
            FileType.DOCUMENT -> Icons.Default.Description to item.fileType.color
            FileType.ARCHIVE -> Icons.Default.FolderZip to item.fileType.color
            FileType.APK -> Icons.Default.Android to item.fileType.color
            FileType.CODE -> Icons.Default.Code to item.fileType.color
            FileType.VAULT_ENCRYPTED -> Icons.Default.Lock to item.fileType.color
            FileType.OTHER -> Icons.Default.InsertDriveFile to item.fileType.color
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
