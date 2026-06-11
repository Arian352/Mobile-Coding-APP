package com.clawforge.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.clawforge.app.core.AgentService
import com.clawforge.app.core.Store
import com.clawforge.app.ui.ClawForgeTheme
import com.clawforge.app.ui.MainScreen
import com.clawforge.app.ui.allRuntimePermissions

class MainActivity : ComponentActivity() {

    // Step 2: background location (must be separate on Android 11+)
    private val bgLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* user responded to background location dialog */ }

    // Step 1: all other permissions
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val bgMissing = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        if (fineGranted && bgMissing) {
            bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(this)
        if (Store.alwaysOn) AgentService.start(this)
        setContent {
            ClawForgeTheme {
                AppRoot()
            }
        }
    }

    @Composable
    private fun AppRoot() {
        val ctx = LocalContext.current

        LaunchedEffect(Unit) {
            // All permissions except background location (handled separately)
            val missing = allRuntimePermissions()
                .filter { it != Manifest.permission.ACCESS_BACKGROUND_LOCATION }
                .filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
                .toTypedArray()

            if (missing.isNotEmpty()) {
                // This triggers the real Android system dialogs for each permission
                permLauncher.launch(missing)
            } else {
                // Main perms already granted — check background location directly
                val bgMissing = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                val fineGranted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (fineGranted && bgMissing) {
                    bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
        }

        MainScreen()
    }
}
