package com.clawforge.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clawforge.app.core.AgentEngine
import com.clawforge.app.core.AgentService
import com.clawforge.app.core.AiClient
import com.clawforge.app.core.ChatMsg
import com.clawforge.app.core.ChatState
import com.clawforge.app.core.Messengers
import com.clawforge.app.core.ProjectsManager
import com.clawforge.app.core.RulesManager
import com.clawforge.app.core.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MainScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = DeepBg,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.SmartToy, contentDescription = null) },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    label = { Text("Projekte") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Rule, contentDescription = null) },
                    label = { Text("Regeln") }
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Setup") }
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> ChatScreen()
                1 -> ProjectsScreen()
                2 -> RulesScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun Header(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = NeonCyan,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

// ---------------------------------------------------------------- Chat

@Composable
fun ChatScreen() {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(ChatState.messages.size) {
        if (ChatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(ChatState.messages.size - 1)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Header("⚡ ClawForge")
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            state = listState
        ) {
            items(ChatState.messages) { msg -> Bubble(msg) }
            if (ChatState.busy) {
                item {
                    Text(
                        "⚡ ClawForge denkt nach …",
                        color = NeonPurple,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Frag ClawForge …") },
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = {
                val text = input.trim()
                if (text.isBlank() || ChatState.busy) return@FilledIconButton
                input = ""
                ChatState.messages.add(ChatMsg("user", text))
                ChatState.busy = true
                scope.launch {
                    val reply = try {
                        AgentEngine.run(ChatState.messages.toList())
                    } catch (e: Exception) {
                        "❌ Fehler: ${e.message}"
                    }
                    ChatState.messages.add(ChatMsg("assistant", reply))
                    ChatState.busy = false
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Senden")
            }
        }
    }
}

@Composable
private fun Bubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) NeonPurple.copy(alpha = 0.25f) else SurfaceDark,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Text(msg.content, Modifier.padding(12.dp), color = TextLight)
        }
    }
}

// ---------------------------------------------------------------- Projekte

@Composable
fun ProjectsScreen() {
    var refresh by remember { mutableIntStateOf(0) }
    val projects = remember(refresh) { ProjectsManager.list() }
    var openProject by remember { mutableStateOf<String?>(null) }
    var openFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Header("📁 Projekte")
        if (projects.isEmpty()) {
            Text(
                "Noch keine Projekte.\nBitte die KI im Chat, eines zu erstellen – z. B. „Erstelle mir eine Website“.",
                Modifier.padding(16.dp), color = TextDim
            )
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            items(projects) { p ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { openProject = p.name }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.name, fontWeight = FontWeight.Bold, color = NeonCyan)
                        Text(
                            "${ProjectsManager.fileList(p.name).size} Datei(en)",
                            style = MaterialTheme.typography.bodySmall, color = TextDim
                        )
                    }
                }
            }
        }
        Button(onClick = { showNew = true }, Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Neues Projekt")
        }
    }

    openProject?.let { project ->
        AlertDialog(
            onDismissRequest = { openProject = null },
            title = { Text(project) },
            text = {
                val files = ProjectsManager.fileList(project)
                if (files.isEmpty()) Text("Projekt ist leer.")
                else LazyColumn(Modifier.height(300.dp)) {
                    items(files) { f ->
                        Text(
                            f, Modifier.fillMaxWidth().clickable { openFile = project to f }
                                .padding(vertical = 8.dp), color = NeonCyan
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { openProject = null }) { Text("Schließen") }
            }
        )
    }

    openFile?.let { (project, path) ->
        AlertDialog(
            onDismissRequest = { openFile = null },
            title = { Text(path) },
            text = {
                Column(Modifier.height(350.dp).verticalScroll(rememberScrollState())) {
                    Text(ProjectsManager.readFile(project, path))
                }
            },
            confirmButton = {
                TextButton(onClick = { openFile = null }) { Text("Schließen") }
            }
        )
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("Neues Projekt") },
            text = {
                OutlinedTextField(name, { name = it }, label = { Text("Name") })
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) ProjectsManager.create(name)
                    showNew = false
                    refresh++
                }) { Text("Anlegen") }
            },
            dismissButton = {
                TextButton(onClick = { showNew = false }) { Text("Abbrechen") }
            }
        )
    }
}

// ---------------------------------------------------------------- Regeln

@Composable
fun RulesScreen() {
    var refresh by remember { mutableIntStateOf(0) }
    val rules = remember(refresh) { RulesManager.list() }
    var editing by remember { mutableStateOf<File?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Header("📜 Regeln")
        Text(
            "Regeln steuern das Verhalten der KI. Kleinere Nummer im Dateinamen = höhere Priorität.",
            Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall, color = TextDim
        )
        LazyColumn(Modifier.weight(1f).padding(12.dp)) {
            items(rules) { f ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { editing = f }
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(f.name, Modifier.weight(1f), color = NeonCyan, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { f.delete(); refresh++ }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = TextDim)
                        }
                    }
                }
            }
        }
        Button(onClick = { creating = true }, Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Neue Regel")
        }
    }

    editing?.let { file ->
        var text by remember(file) { mutableStateOf(file.readText()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(file.name) },
            text = {
                OutlinedTextField(
                    text, { text = it },
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { file.writeText(text); editing = null; refresh++ }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("Abbrechen") }
            }
        )
    }

    if (creating) {
        var name by remember { mutableStateOf("020_neue_regel") }
        var content by remember { mutableStateOf("# Verhalten\n\n…\n\n---\n\n# Regeln\n\n1. …") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("Neue Regel") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Dateiname (Nummer = Priorität)") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        content, { content = it },
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) RulesManager.create(name, content)
                    creating = false
                    refresh++
                }) { Text("Anlegen") }
            },
            dismissButton = {
                TextButton(onClick = { creating = false }) { Text("Abbrechen") }
            }
        )
    }
}

// ---------------------------------------------------------------- Einstellungen

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf(Store.provider) }
    var apiKey by remember(provider) { mutableStateOf(Store.apiKey(provider)) }
    var model by remember(provider) { mutableStateOf(Store.model(provider)) }
    var internet by remember { mutableStateOf(Store.internetEnabled) }
    var netAccess by remember { mutableStateOf(Store.networkAccessEnabled) }
    var alwaysOn by remember { mutableStateOf(Store.alwaysOn) }
    var tgEnabled by remember { mutableStateOf(Store.telegramEnabled) }
    var tgToken by remember { mutableStateOf(Store.telegramToken) }
    var waEnabled by remember { mutableStateOf(Store.whatsappEnabled) }
    var waToken by remember { mutableStateOf(Store.whatsappToken) }
    var waPhoneId by remember { mutableStateOf(Store.whatsappPhoneId) }
    var waTo by remember { mutableStateOf(Store.whatsappTo) }
    var ghToken by remember { mutableStateOf(Store.githubToken) }
    var ghRepo by remember { mutableStateOf(Store.githubRepo) }
    var waTestResult by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("⚙️ Einstellungen", style = MaterialTheme.typography.headlineSmall,
            color = NeonCyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SectionTitle("KI-Anbieter")
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            AiClient.PROVIDERS.forEach { p ->
                FilterChip(
                    selected = provider == p,
                    onClick = { provider = p; Store.provider = p },
                    label = { Text(p) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; Store.setApiKey(provider, it) },
            label = { Text("API-Schlüssel für $provider") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text("Modell", color = TextDim, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            AiClient.defaultModels(provider).forEach { m ->
                FilterChip(
                    selected = model == m,
                    onClick = { model = m; Store.setModel(provider, m) },
                    label = { Text(m) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it; Store.setModel(provider, it) },
            label = { Text("Eigenes Modell (frei eintragen)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SectionDivider()
        SectionTitle("Berechtigungen")
        ToggleRow("Internet-Zugriff", "Die KI darf Webseiten und APIs abrufen", internet) {
            internet = it; Store.internetEnabled = it
        }
        ToggleRow("Netzwerk-Zugriff", "Die KI darf Geräte im WLAN finden und ansprechen", netAccess) {
            netAccess = it; Store.networkAccessEnabled = it
        }
        ToggleRow("Agent immer aktiv", "KI läuft im Hintergrund, solange das Handy an ist", alwaysOn) {
            alwaysOn = it; Store.alwaysOn = it
            if (it) AgentService.start(ctx) else AgentService.stop(ctx)
        }

        SectionDivider()
        SectionTitle("Telegram")
        ToggleRow("Telegram-Bot", "Antwortet auf Nachrichten an deinen Bot (Token von @BotFather)", tgEnabled) {
            tgEnabled = it; Store.telegramEnabled = it
            if (Store.alwaysOn) AgentService.start(ctx)
        }
        OutlinedTextField(
            value = tgToken,
            onValueChange = { tgToken = it; Store.telegramToken = it },
            label = { Text("Bot-Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SectionDivider()
        SectionTitle("WhatsApp (Meta Cloud API)")
        ToggleRow("WhatsApp", "Die KI kann dir Nachrichten per WhatsApp senden", waEnabled) {
            waEnabled = it; Store.whatsappEnabled = it
        }
        OutlinedTextField(
            value = waToken,
            onValueChange = { waToken = it; Store.whatsappToken = it },
            label = { Text("Access-Token") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = waPhoneId,
            onValueChange = { waPhoneId = it; Store.whatsappPhoneId = it },
            label = { Text("Telefonnummer-ID") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = waTo,
            onValueChange = { waTo = it; Store.whatsappTo = it },
            label = { Text("Empfänger (z. B. 4915112345678)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                waTestResult = withContext(Dispatchers.IO) {
                    Messengers.sendWhatsApp("Testnachricht von ClawForge ⚡")
                }
            }
        }) { Text("Testnachricht senden") }
        if (waTestResult.isNotBlank()) {
            Text(waTestResult, color = TextDim, style = MaterialTheme.typography.bodySmall)
        }

        SectionDivider()
        SectionTitle("APK-Builds (GitHub Actions)")
        Text(
            "Mit einem GitHub-Token kann die KI APK-Builds in deinem Repository starten.",
            color = TextDim, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ghToken,
            onValueChange = { ghToken = it; Store.githubToken = it },
            label = { Text("GitHub-Token") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = ghRepo,
            onValueChange = { ghRepo = it; Store.githubRepo = it },
            label = { Text("Repository (benutzer/repo)") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = NeonPurple, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(vertical = 16.dp), color = SurfaceDark)
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextLight)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextDim)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
