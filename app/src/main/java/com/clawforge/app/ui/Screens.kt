package com.clawforge.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clawforge.app.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

// ─── Root ────────────────────────────────────────────────────────────────────

@Composable
fun MainScreen() {
    var tab by remember { mutableIntStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(BgDeep)
    ) {
        Box(Modifier.fillMaxSize().padding(bottom = 76.dp)) {
            when (tab) {
                0 -> ChatScreen()
                1 -> ProjectsScreen()
                2 -> RulesScreen()
                else -> SettingsScreen()
            }
        }
        // Floating pill nav
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(BgCard)
                .border(0.7.dp, BorderColor, RoundedCornerShape(28.dp))
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(Icons.Filled.Chat, "Chat", tab == 0) { tab = 0 }
                NavItem(Icons.Filled.FolderOpen, "Projekte", tab == 1) { tab = 1 }
                NavItem(Icons.Filled.Description, "Regeln", tab == 2) { tab = 2 }
                NavItem(Icons.Filled.Tune, "Setup", tab == 3) { tab = 3 }
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, sel: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (sel) BrandIndigo.copy(alpha = 0.18f) else Color.Transparent, label = "nbg"
    )
    val fg by animateColorAsState(
        if (sel) BrandCyan else TextSecondary, label = "nfg"
    )
    Box(
        Modifier.clip(RoundedCornerShape(22.dp)).background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = if (sel) 14.dp else 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(19.dp))
            if (sel) Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Chat ────────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen() {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(ChatState.messages.size) {
        if (ChatState.messages.isNotEmpty()) listState.animateScrollToItem(ChatState.messages.size - 1)
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Column(
            Modifier.fillMaxWidth().background(BgDeep)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                        .background(GradientBrand2).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) { Text("⚡", fontSize = 20.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "ClawForge",
                        style = TextStyle(brush = GradientBrand, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(StatusGreen))
                        Text(
                            "${Store.provider} · ${Store.model(Store.provider).take(22)}",
                            color = TextSecondary, fontSize = 11.sp
                        )
                    }
                }
            }
        }
        Divider(BgElevated)

        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 14.dp),
            state = listState,
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            items(ChatState.messages) { m -> MessageBubble(m); Spacer(Modifier.height(10.dp)) }
            if (ChatState.busy) item { TypingBubble() }
        }

        ChatInput(
            value = input,
            onChange = { input = it },
            onSend = {
                val t = input.trim()
                if (t.isBlank() || ChatState.busy) return@ChatInput
                input = ""
                ChatState.messages.add(ChatMsg("user", t))
                ChatState.busy = true
                scope.launch {
                    val reply = try { AgentEngine.run(ChatState.messages.toList()) }
                    catch (e: Exception) { "❌ ${e.message}" }
                    ChatState.messages.add(ChatMsg("assistant", reply))
                    ChatState.busy = false
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(GradientBrand2),
                contentAlignment = Alignment.Center
            ) { Text("⚡", fontSize = 13.sp) }
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.widthIn(max = 290.dp)) {
            if (isUser) {
                Box(
                    Modifier
                        .background(GradientUser, RoundedCornerShape(20.dp, 5.dp, 20.dp, 20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(msg.content, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                }
            } else {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp))
                        .background(BgCard)
                        .border(0.7.dp, BorderColor, RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp))
                        .drawBehind {
                            drawLine(
                                color = BrandCyan.copy(alpha = 0.7f),
                                start = Offset(2.dp.toPx(), 0f),
                                end = Offset(2.dp.toPx(), size.height),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(msg.content, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(GradientBrand2),
            contentAlignment = Alignment.Center
        ) { Text("⚡", fontSize = 13.sp) }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp))
                .background(BgCard)
                .border(0.7.dp, BorderColor, RoundedCornerShape(5.dp, 20.dp, 20.dp, 20.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) { TypingDots() }
    }
}

@Composable
private fun TypingDots() {
    var active by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(280); active = (active + 1) % 3 }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier
                    .size(if (i == active) 9.dp else 6.dp)
                    .clip(CircleShape)
                    .background(BrandCyan.copy(alpha = if (i == active) 1f else 0.25f))
            )
        }
    }
}

@Composable
private fun ChatInput(value: String, onChange: (String) -> Unit, onSend: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().background(BgDeep)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)).background(BgCard)
                .border(0.7.dp, BorderColor, RoundedCornerShape(28.dp))
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value, onValueChange = onChange,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, lineHeight = 21.sp),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text("Schreib etwas …", color = TextSecondary, fontSize = 15.sp)
                    inner()
                },
                maxLines = 5,
                cursorBrush = SolidColor(BrandCyan),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )
            Spacer(Modifier.width(6.dp))
            val active = value.isNotBlank() && !ChatState.busy
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (active) GradientBrand2 else SolidColor(BgElevated))
                    .clickable(enabled = active, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, null,
                    tint = if (active) Color.White else TextSecondary,
                    modifier = Modifier.size(19.dp))
            }
        }
    }
}

// ─── Projects ────────────────────────────────────────────────────────────────

@Composable
fun ProjectsScreen() {
    var refresh by remember { mutableIntStateOf(0) }
    val projects = remember(refresh) { ProjectsManager.list() }
    var viewProject by remember { mutableStateOf<String?>(null) }
    var viewFile by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showNew by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Projekte", Icons.Filled.FolderOpen) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(GradientBrand2).clickable { showNew = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
        if (projects.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Noch keine Projekte", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sag der KI im Chat:\nz.B. Erstelle eine Website\noder: Baue mir eine App",
                        color = TextSecondary, fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(projects) { p ->
                    val fileCount = ProjectsManager.fileList(p.name).size
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(BgCard).border(0.7.dp, BorderColor, RoundedCornerShape(16.dp))
                            .clickable { viewProject = p.name }
                            .padding(16.dp)
                    ) {
                        Column {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(GradientBrand),
                                contentAlignment = Alignment.Center
                            ) { Text("📁", fontSize = 20.sp) }
                            Spacer(Modifier.height(10.dp))
                            Text(p.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp, maxLines = 2)
                            Spacer(Modifier.height(4.dp))
                            Text("$fileCount Datei${if (fileCount == 1) "" else "en"}",
                                color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    viewProject?.let { p ->
        val files = remember(p) { ProjectsManager.fileList(p) }
        AlertDialog(
            onDismissRequest = { viewProject = null },
            containerColor = BgCard,
            tonalElevation = 0.dp,
            title = { Text(p, color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (files.isEmpty()) Text("Leer", color = TextSecondary)
                else LazyColumn(Modifier.heightIn(max = 350.dp)) {
                    items(files) { f ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewFile = p to f }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(fileEmoji(f), fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(f, color = BrandCyan, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                        if (f != files.last()) Divider(BorderColor)
                    }
                }
            },
            confirmButton = { TextButton({ viewProject = null }) { Text("Schließen", color = BrandCyan) } }
        )
    }

    viewFile?.let { (p, path) ->
        val content = remember(p, path) { ProjectsManager.readFile(p, path) }
        AlertDialog(
            onDismissRequest = { viewFile = null },
            containerColor = BgCard,
            tonalElevation = 0.dp,
            title = { Text(path, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    Text(content, color = TextPrimary, fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
                }
            },
            confirmButton = { TextButton({ viewFile = null }) { Text("Schließen", color = BrandCyan) } }
        )
    }

    if (showNew) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            containerColor = BgCard,
            tonalElevation = 0.dp,
            title = { Text("Neues Projekt", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                StyledTextField(value = name, onValueChange = { name = it }, label = "Projektname")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) { ProjectsManager.create(name); refresh++ }
                    showNew = false
                }) { Text("Anlegen", color = BrandCyan) }
            },
            dismissButton = { TextButton({ showNew = false }) { Text("Abbrechen", color = TextSecondary) } }
        )
    }
}

private fun fileEmoji(path: String): String {
    val ext = path.substringAfterLast(".", "")
    return when (ext.lowercase()) {
        "html", "htm" -> "🌐"
        "css" -> "🎨"
        "js", "ts" -> "⚡"
        "kt", "java" -> "☕"
        "py" -> "🐍"
        "md" -> "📝"
        "json" -> "📦"
        "xml" -> "🔧"
        "png", "jpg", "jpeg", "gif", "svg" -> "🖼️"
        "apk" -> "📱"
        else -> "📄"
    }
}

// ─── Rules ───────────────────────────────────────────────────────────────────

@Composable
fun RulesScreen() {
    var refresh by remember { mutableIntStateOf(0) }
    val rules = remember(refresh) { RulesManager.list() }
    var editing by remember { mutableStateOf<File?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Regeln", Icons.Filled.Description) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(GradientBrand2).clickable { creating = true },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp)).background(BgElevated)
                .border(0.7.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("💡", fontSize = 14.sp)
            Text("Kleinere Zahl im Dateinamen = höhere Priorität", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            items(rules) { f ->
                val priority = f.name.takeWhile { it.isDigit() }.take(3).ifBlank { "?" }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp)).background(BgCard)
                        .border(0.7.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { editing = f }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(BrandViolet.copy(0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(priority, color = BrandViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            f.readText().lines().firstOrNull { it.isNotBlank() }?.take(55) ?: "",
                            color = TextSecondary, fontSize = 11.sp, maxLines = 1
                        )
                    }
                    IconButton(onClick = { f.delete(); refresh++ }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    editing?.let { file ->
        var text by remember(file) { mutableStateOf(file.readText()) }
        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = BgCard, tonalElevation = 0.dp,
            title = { Text(file.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
            text = {
                OutlinedTextField(
                    text, { text = it }, modifier = Modifier.fillMaxWidth().height(340.dp),
                    colors = fieldColors(), textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                )
            },
            confirmButton = {
                TextButton(onClick = { file.writeText(text); editing = null; refresh++ }) {
                    Text("Speichern", color = BrandCyan)
                }
            },
            dismissButton = { TextButton({ editing = null }) { Text("Abbrechen", color = TextSecondary) } }
        )
    }

    if (creating) {
        var name by remember { mutableStateOf("020_neue_regel") }
        var content by remember { mutableStateOf("# Verhalten\n\n…\n\n---\n\n# Regeln\n\n1. …") }
        AlertDialog(
            onDismissRequest = { creating = false },
            containerColor = BgCard, tonalElevation = 0.dp,
            title = { Text("Neue Regel", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    StyledTextField(name, { name = it }, "Dateiname (Nummer = Priorität)")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        content, { content = it }, modifier = Modifier.fillMaxWidth().height(200.dp),
                        colors = fieldColors(), textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) RulesManager.create(name, content)
                    creating = false; refresh++
                }) { Text("Anlegen", color = BrandCyan) }
            },
            dismissButton = { TextButton({ creating = false }) { Text("Abbrechen", color = TextSecondary) } }
        )
    }
}

// ─── Settings ────────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen() {
    val scope = rememberCoroutineScope()

    var provider by remember { mutableStateOf(Store.provider) }
    var apiKey by remember(provider) { mutableStateOf(Store.apiKey(provider)) }
    var model by remember(provider) { mutableStateOf(Store.model(provider)) }
    var keyVisible by remember { mutableStateOf(false) }
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
    var statusMsg by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 0.dp)
    ) {
        ScreenHeader("Setup", Icons.Filled.Tune)

        // AI Provider
        SettingsCard {
            SectionLabel("KI-ANBIETER")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AiClient.PROVIDERS.forEach { p ->
                    val sel = provider == p
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BrandIndigo else BgElevated)
                            .border(0.7.dp, if (sel) BrandIndigo else BorderColor, RoundedCornerShape(20.dp))
                            .clickable { provider = p; Store.provider = p }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(p, color = if (sel) Color.White else TextSecondary, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it; Store.setApiKey(provider, it) },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                label = { Text("API-Schlüssel ($provider)") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(if (keyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            null, tint = TextSecondary)
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            Text("Modell", color = TextSecondary, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AiClient.defaultModels(provider).forEach { m ->
                    val sel = model == m
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (sel) BrandViolet.copy(0.25f) else BgElevated)
                            .border(0.7.dp, if (sel) BrandViolet else BorderColor, RoundedCornerShape(20.dp))
                            .clickable { model = m; Store.setModel(provider, m) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) { Text(m, color = if (sel) BrandCyan else TextSecondary, fontSize = 11.sp) }
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it; Store.setModel(provider, it) },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                label = { Text("Eigenes Modell eintragen") }, singleLine = true
            )
        }

        Spacer(Modifier.height(10.dp))

        // Permissions
        SettingsCard {
            SectionLabel("BERECHTIGUNGEN")
            ToggleRow("🌐  Internet-Zugriff", "KI darf Webseiten und APIs abrufen", internet) {
                internet = it; Store.internetEnabled = it
            }
            ToggleRow("📡  Netzwerk-Scan", "KI darf Geräte im WLAN finden", netAccess) {
                netAccess = it; Store.networkAccessEnabled = it
            }
            ToggleRow("⚡  Agent immer aktiv", "KI läuft im Hintergrund, auch nach Neustart", alwaysOn) {
                alwaysOn = it; Store.alwaysOn = it
                if (it) com.clawforge.app.core.AgentService.start(com.clawforge.app.ClawForgeApp.ctx)
                else com.clawforge.app.core.AgentService.stop(com.clawforge.app.ClawForgeApp.ctx)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Telegram
        SettingsCard {
            SectionLabel("TELEGRAM")
            ToggleRow("Bot aktiv", "Antwortet auf Nachrichten an deinen Telegram-Bot", tgEnabled) {
                tgEnabled = it; Store.telegramEnabled = it
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tgToken, onValueChange = { tgToken = it; Store.telegramToken = it },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                label = { Text("Bot-Token (von @BotFather)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(Modifier.height(10.dp))

        // WhatsApp
        SettingsCard {
            SectionLabel("WHATSAPP (Meta Cloud API)")
            ToggleRow("WhatsApp aktiv", "KI kann dir per WhatsApp schreiben", waEnabled) {
                waEnabled = it; Store.whatsappEnabled = it
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(waToken, { waToken = it; Store.whatsappToken = it },
                Modifier.fillMaxWidth(), colors = fieldColors(), label = { Text("Access-Token") },
                singleLine = true, visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(waPhoneId, { waPhoneId = it; Store.whatsappPhoneId = it },
                Modifier.fillMaxWidth(), colors = fieldColors(), label = { Text("Telefonnummer-ID") }, singleLine = true)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(waTo, { waTo = it; Store.whatsappTo = it },
                Modifier.fillMaxWidth(), colors = fieldColors(), label = { Text("Empfänger (z. B. 4915112345678)") }, singleLine = true)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GradientBrand2)
                    .clickable {
                        scope.launch {
                            statusMsg = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                Messengers.sendWhatsApp("Test von ClawForge ⚡")
                            }
                        }
                    }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("Testnachricht senden", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.height(10.dp))

        // GitHub / APK Build
        SettingsCard {
            SectionLabel("APK-BUILDS (GITHUB ACTIONS)")
            Text("Mit einem GitHub-Token kann die KI im Chat APK-Builds starten.",
                color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(ghToken, { ghToken = it; Store.githubToken = it },
                Modifier.fillMaxWidth(), colors = fieldColors(), label = { Text("GitHub-Token") },
                singleLine = true, visualTransformation = PasswordVisualTransformation())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(ghRepo, { ghRepo = it; Store.githubRepo = it },
                Modifier.fillMaxWidth(), colors = fieldColors(), label = { Text("Repository (benutzer/repo)") }, singleLine = true)
        }

        if (statusMsg.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(BgElevated).border(0.7.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) { Text(statusMsg, color = BrandCyan, fontSize = 13.sp) }
        }

        Spacer(Modifier.height(28.dp))
    }
}

// ─── Shared components ───────────────────────────────────────────────────────

@Composable
private fun ScreenHeader(
    title: String,
    icon: ImageVector,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().background(BgDeep).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = BrandCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = TextStyle(brush = GradientBrand, fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
    Divider(BorderColor)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BgCard)
            .border(0.7.dp, BorderColor, RoundedCornerShape(16.dp)).padding(16.dp),
        content = content
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandIndigo,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BgElevated,
                uncheckedBorderColor = BorderColor
            )
        )
    }
}

@Composable
private fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(), label = { Text(label) }, singleLine = true)
}

@Composable
private fun Divider(color: Color) {
    Box(Modifier.fillMaxWidth().height(0.7.dp).background(color))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BrandCyan,
    unfocusedBorderColor = BorderColor,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = BrandCyan,
    focusedLabelColor = BrandCyan,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = BgElevated,
    unfocusedContainerColor = BgElevated
)
