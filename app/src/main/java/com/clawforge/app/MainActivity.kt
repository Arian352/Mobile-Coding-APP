package com.clawforge.app

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

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* OS handles the dialog results */ }

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
            val missing = allRuntimePermissions().filter { perm ->
                ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
            if (missing.isNotEmpty()) {
                permLauncher.launch(missing)
            }
        }

        MainScreen()
    }
}
