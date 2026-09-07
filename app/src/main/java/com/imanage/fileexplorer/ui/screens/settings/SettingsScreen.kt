package com.imanage.fileexplorer.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imanage.fileexplorer.data.crypto.PinCryptoHelper
import com.imanage.fileexplorer.data.root.RootShellProvider
import com.imanage.fileexplorer.data.update.AppUpdateInfo
import com.imanage.fileexplorer.data.update.UpdateManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPinSetup: () -> Unit,
    onNavigateToWifiShare: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("imanage_prefs", Context.MODE_PRIVATE) }

    var flagSecureEnabled by remember {
        mutableStateOf(prefs.getBoolean("flag_secure", true))
    }
    var shredPasses by remember {
        mutableIntStateOf(prefs.getInt("shred_passes", 3))
    }
    var pinEnabled by remember {
        mutableStateOf(PinCryptoHelper.isPinEnabled(context))
    }
    var autoLockTimeout by remember {
        mutableIntStateOf(PinCryptoHelper.getAutoLockTimeoutSeconds(context))
    }
    var rootAccessEnabled by remember {
        mutableStateOf(prefs.getBoolean("root_access_enabled", false))
    }
    var showHiddenFiles by remember {
        mutableStateOf(prefs.getBoolean("show_hidden_files", false))
    }
    var showNomediaFiles by remember {
        mutableStateOf(prefs.getBoolean("show_nomedia_files", true))
    }
    var showTimeoutMenu by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfoDialog by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val versionInfo = remember { UpdateManager.getCurrentVersion(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Wireless PC Transfer
            item {
                Text(
                    text = "Connectivity & Sharing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToWifiShare() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF006874))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PC / Wi-Fi Transfer Server", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Transfer files to/from your PC browser over local Wi-Fi without internet or cables.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Display & File Preferences
            item {
                Text(
                    text = "Display & File Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Show / Hide Hidden Files
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Hidden Files & Folders", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Display files and folders starting with a dot (e.g. .thumbnails, .system).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showHiddenFiles,
                            onCheckedChange = {
                                showHiddenFiles = it
                                prefs.edit().putBoolean("show_hidden_files", it).apply()
                            }
                        )
                    }
                }
            }

            // Show / Hide .nomedia Files
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.HideImage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show .nomedia Files", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Display .nomedia hidden marker files inside media directories.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showNomediaFiles,
                            onCheckedChange = {
                                showNomediaFiles = it
                                prefs.edit().putBoolean("show_nomedia_files", it).apply()
                            }
                        )
                    }
                }
            }

            // Security & Privacy
            item {
                Text(
                    text = "Security & Master Lock",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Master PIN Setup
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Master PIN / Password", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (pinEnabled) "Master PIN is active (PBKDF2 encrypted)" else "Set a 4-digit PIN for app & vault unlock fallback",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (pinEnabled) {
                            TextButton(onClick = {
                                PinCryptoHelper.disablePin(context)
                                pinEnabled = false
                                Toast.makeText(context, "PIN disabled", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            Button(onClick = onNavigateToPinSetup) {
                                Text("Setup")
                            }
                        }
                    }
                }
            }

            // Auto-Lock Timeout
            if (pinEnabled) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Lock Inactivity Timeout", fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(2.dp))
                                val timeoutLabel = when (autoLockTimeout) {
                                    0 -> "Immediately on background"
                                    30 -> "30 seconds"
                                    60 -> "1 minute"
                                    300 -> "5 minutes"
                                    900 -> "15 minutes"
                                    else -> "$autoLockTimeout seconds"
                                }
                                Text("Lock after: $timeoutLabel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                TextButton(onClick = { showTimeoutMenu = true }) {
                                    Text("Change")
                                }
                                DropdownMenu(
                                    expanded = showTimeoutMenu,
                                    onDismissRequest = { showTimeoutMenu = false }
                                ) {
                                    val options = listOf(
                                        0 to "Immediately",
                                        30 to "30 seconds",
                                        60 to "1 minute",
                                        300 to "5 minutes",
                                        900 to "15 minutes"
                                    )
                                    options.forEach { (secs, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                autoLockTimeout = secs
                                                PinCryptoHelper.setAutoLockTimeoutSeconds(context, secs)
                                                showTimeoutMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Anti-Snooping FLAG_SECURE
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Screen Obfuscation (FLAG_SECURE)", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Blocks screenshots, screen recordings, and obscures previews in the recent apps switcher.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = flagSecureEnabled,
                            onCheckedChange = {
                                flagSecureEnabled = it
                                prefs.edit().putBoolean("flag_secure", it).apply()
                                val activity = context as? Activity
                                if (it) {
                                    activity?.window?.setFlags(
                                        WindowManager.LayoutParams.FLAG_SECURE,
                                        WindowManager.LayoutParams.FLAG_SECURE
                                    )
                                } else {
                                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                                }
                            }
                        )
                    }
                }
            }

            // Secure Shredding Passes
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Shredder Overwrite Passes", fontWeight = FontWeight.SemiBold)
                            Text("$shredPasses Passes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Number of multi-pass random byte overwrites before inode deletion.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = shredPasses.toFloat(),
                            onValueChange = {
                                shredPasses = it.toInt()
                                prefs.edit().putInt("shred_passes", it.toInt()).apply()
                            },
                            valueRange = 1f..7f,
                            steps = 5
                        )
                    }
                }
            }

            // Power User / Root Access
            item {
                Text(
                    text = "Advanced & Power Users",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Elevated Root & Shizuku Access", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Enables root shell commands and system partition access if device is rooted.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rootAccessEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    scope.launch {
                                        val hasRoot = RootShellProvider.isRootAvailable()
                                        if (hasRoot) {
                                            rootAccessEnabled = true
                                            prefs.edit().putBoolean("root_access_enabled", true).apply()
                                            Toast.makeText(context, "Root access enabled", Toast.LENGTH_SHORT).show()
                                        } else {
                                            rootAccessEnabled = false
                                            Toast.makeText(context, "No root / su binary detected on this device", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    rootAccessEnabled = false
                                    prefs.edit().putBoolean("root_access_enabled", false).apply()
                                }
                            }
                        )
                    }
                }
            }

            // About & Help
            item {
                Text(
                    text = "About & Help",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Quick Tour / App Introduction
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToOnboarding() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Feature Tour & Introduction", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Revisit the first-time welcome slider and explore all app features.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Check for Updates
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isCheckingUpdate) {
                            isCheckingUpdate = true
                            scope.launch {
                                val result = UpdateManager.checkForUpdates(context)
                                isCheckingUpdate = false
                                result.fold(
                                    onSuccess = { info ->
                                        if (info.isUpdateAvailable) {
                                            updateInfoDialog = info
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "You're using the latest version (v${versionInfo.first})",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            context,
                                            "Could not check for updates: ${error.localizedMessage ?: "Offline"}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Installed: v${versionInfo.first} (Build ${versionInfo.second})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Zero Telemetry Guarantee Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline & Open-Source (GPL-3.0)", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "This build contains zero analytics SDKs, zero crash trackers, and zero third-party telemetry. Your files stay strictly on your device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // In-App Update Dialog
        if (updateInfoDialog != null) {
            val info = updateInfoDialog!!
            AlertDialog(
                onDismissRequest = {
                    if (!info.isMandatory) updateInfoDialog = null
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text("Update Available: v${info.versionName}")
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (info.releaseDate.isNotBlank()) {
                            Text(
                                text = "Released: ${info.releaseDate}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (info.releaseNotes.isNotEmpty()) {
                            Text(
                                text = "What's New:",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            info.releaseNotes.forEach { note ->
                                Row(modifier = Modifier.padding(start = 4.dp)) {
                                    Text("• ", fontWeight = FontWeight.Bold)
                                    Text(note, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            UpdateManager.startDownload(context, info)
                            updateInfoDialog = null
                            Toast.makeText(context, "Initiating update...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Update Now")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                UpdateManager.openInBrowser(
                                    context,
                                    info.fallbackUrl.ifBlank { "https://pcshrestha.com.np/imanage" }
                                )
                            }
                        ) {
                            Text("Website")
                        }
                        if (!info.isMandatory) {
                            TextButton(onClick = { updateInfoDialog = null }) {
                                Text("Later")
                            }
                        }
                    }
                }
            )
        }
    }
}
