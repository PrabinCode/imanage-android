package com.imanage.fileexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanage.fileexplorer.data.model.FileItem
import com.imanage.fileexplorer.data.model.OperationProgress

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (folderName: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    isError = false
                },
                label = { Text("Folder Name") },
                singleLine = true,
                isError = isError,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.trim().isNotEmpty()) {
                        onConfirm(text.trim())
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RenameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("New Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.trim().isNotEmpty() && text.trim() != currentName) {
                        onConfirm(text.trim())
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    itemCount: Int,
    itemName: String,
    onDismiss: () -> Unit,
    onMoveToTrash: () -> Unit,
    onSecureShred: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (itemCount > 1) "Delete $itemCount items?" else "Delete '$itemName'?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose how you would like to remove these files:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "• Move to Trash: Soft delete. Can be restored anytime within 30 days.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Secure Shred: DoD multi-pass overwrite. Permanently unrecoverable.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMoveToTrash,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Move to Trash")
                }
                Button(
                    onClick = onSecureShred,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Shred")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FileInfoDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoRow(label = "Name", value = item.name)
                InfoRow(label = "Path", value = item.path)
                InfoRow(label = "Type", value = item.fileType.displayName)
                InfoRow(label = "Size", value = "${item.formattedSize} (${item.size} bytes)")
                InfoRow(label = "Last Modified", value = item.formattedDate)
                InfoRow(label = "Readable", value = if (item.file.canRead()) "Yes" else "No")
                InfoRow(label = "Writable", value = if (item.file.canWrite()) "Yes" else "No")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OperationProgressDialog(
    progress: OperationProgress,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismiss while running */ },
        title = { Text("Processing ${progress.type.name}...", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = progress.currentFileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${(progress.progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${FileItem.formatBytes(progress.bytesProcessed)} / ${FileItem.formatBytes(progress.totalBytes)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Composable
fun PasswordZipDialog(
    defaultZipName: String,
    onDismiss: () -> Unit,
    onConfirm: (zipName: String, password: String?) -> Unit
) {
    var zipName by remember { mutableStateOf(defaultZipName) }
    var enablePassword by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress to ZIP", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = zipName,
                    onValueChange = { zipName = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = enablePassword,
                        onCheckedChange = { enablePassword = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Password Protect (Encryption)", style = MaterialTheme.typography.bodyMedium)
                }

                if (enablePassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Enter Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (zipName.endsWith(".zip", ignoreCase = true)) zipName else "$zipName.zip"
                    val finalPass = if (enablePassword && password.isNotEmpty()) password else null
                    onConfirm(finalName, finalPass)
                }
            ) {
                Text("Compress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExtractArchiveDialog(
    archiveName: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String?) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var hasPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extract $archiveName", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Extract contents into current directory?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = hasPassword,
                        onCheckedChange = { hasPassword = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Archive is Password Protected", style = MaterialTheme.typography.bodyMedium)
                }

                if (hasPassword) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Archive Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPass = if (hasPassword && password.isNotEmpty()) password else null
                    onConfirm(finalPass)
                }
            ) {
                Text("Extract Here")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AssignTagDialog(
    fileName: String,
    currentTagHex: String?,
    onDismiss: () -> Unit,
    onSelectTag: (colorHex: String, label: String) -> Unit,
    onRemoveTag: () -> Unit
) {
    val tags = listOf(
        Pair("#EF4444", "Important (Red)"),
        Pair("#10B981", "Personal (Green)"),
        Pair("#3B82F6", "Work (Blue)"),
        Pair("#F97316", "Finance (Orange)"),
        Pair("#8B5CF6", "Archive (Purple)")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Color Tag", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Tag: $fileName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                tags.forEach { (hex, label) ->
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Red }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentTagHex == hex) color.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            TextButton(
                                onClick = { onSelectTag(hex, label.substringBefore(" (")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontWeight = if (currentTagHex == hex) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentTagHex != null) {
                TextButton(
                    onClick = onRemoveTag,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Tag")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
