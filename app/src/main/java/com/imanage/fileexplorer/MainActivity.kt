package com.imanage.fileexplorer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.imanage.fileexplorer.data.security.AutoLockManager
import com.imanage.fileexplorer.ui.navigation.AppNavGraph
import com.imanage.fileexplorer.ui.theme.IManageTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("imanage_prefs", Context.MODE_PRIVATE)
        val flagSecureEnabled = prefs.getBoolean("flag_secure", true)
        if (flagSecureEnabled) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        val app = application as IManageApp

        setContent {
            IManageTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController, app = app)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AutoLockManager.onAppForegrounded(this)
    }

    override fun onPause() {
        super.onPause()
        AutoLockManager.onAppBackgrounded()
    }

    private fun checkAndRequestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}
