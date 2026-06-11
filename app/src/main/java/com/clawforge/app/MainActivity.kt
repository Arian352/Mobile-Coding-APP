package com.clawforge.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.clawforge.app.core.AgentService
import com.clawforge.app.core.Store
import com.clawforge.app.ui.ClawForgeTheme
import com.clawforge.app.ui.MainScreen
import com.clawforge.app.ui.allRuntimePermissions

class MainActivity : ComponentActivity() {

    private var onPermResult: ((Map<String, Boolean>) -> Unit)? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        onPermResult?.invoke(result)
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
        val prefs = remember { ctx.getSharedPreferences("clawforge", Context.MODE_PRIVATE) }
        var permsDone by remember { mutableStateOf(prefs.getBoolean("perms_done", false)) }

        if (!permsDone) {
            LaunchedEffect(Unit) {
                onPermResult = {
                    prefs.edit().putBoolean("perms_done", true).apply()
                    permsDone = true
                }
                permLauncher.launch(allRuntimePermissions())
            }
        }

        MainScreen()
    }
}
