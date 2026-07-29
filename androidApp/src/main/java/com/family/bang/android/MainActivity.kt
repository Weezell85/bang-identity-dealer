package com.family.bang.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Rust = Color(0xFF8F2D1E)
private val Cream = Color(0xFFFFF4D8)
private val Ink = Color(0xFF2B1712)
private val Gold = Color(0xFFD99B35)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BangTheme { BangApp() } }
    }
}

@Composable
private fun BangTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Rust,
            onPrimary = Color.White,
            secondary = Gold,
            background = Cream,
            surface = Color(0xFFFFFAEE),
            onBackground = Ink,
            onSurface = Ink,
            error = Color(0xFFB3261E),
        ),
        typography = Typography(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        ),
        content = content,
    )
}

@Composable
private fun BangApp(vm: GameViewModel = viewModel()) {
    val ui by vm.state
    Scaffold(
        containerColor = Cream,
        snackbarHost = {
            ui.error?.let { error ->
                Snackbar(modifier = Modifier.padding(16.dp), action = {
                    TextButton(onClick = vm::dismissError) { Text("DISMISS") }
                }) { Text(error) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (ui.screen) {
                Screen.HOME -> HomeScreen(ui.loading, vm::createAndJoin, vm::join)
                Screen.LOBBY -> LobbyScreen(ui, vm::refresh, vm::deal, vm::revealRole, vm::hideRole, vm::leave)
            }
            if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun HomeScreen(
    loading: Boolean,
    create: (String) -> Unit,
    join: (String, String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(.7f))
        Text("BANG!", fontSize = 58.sp, fontWeight = FontWeight.Black, color = Rust, letterSpacing = 2.sp)
        Text("IDENTITY DEALER", fontWeight = FontWeight.Bold, letterSpacing = 4.sp, color = Ink.copy(alpha = .72f))
        Spacer(Modifier.height(12.dp))
        Text(
            "Gather your posse. Keep your identity secret.",
            textAlign = TextAlign.Center,
            color = Ink.copy(alpha = .72f),
        )
        Spacer(Modifier.height(36.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(5.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Your name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    placeholder = { Text("e.g. Calamity Jane") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { focus.clearFocus(); create(name) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("CREATE A GAME", fontWeight = FontWeight.Bold) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("  OR JOIN  ", color = Ink.copy(alpha = .55f), fontSize = 12.sp)
                    HorizontalDivider(Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.filter(Char::isLetterOrDigit).take(6).uppercase() },
                    placeholder = { Text("6-character game code") },
                    label = { Text("Game code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); join(code, name) }),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { focus.clearFocus(); join(code, name) },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("JOIN GAME", fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("4–7 players", color = Ink.copy(alpha = .6f), modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun LobbyScreen(
    ui: GameUiState,
    refresh: () -> Unit,
    deal: () -> Unit,
    reveal: () -> Unit,
    hideRole: () -> Unit,
    leave: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 22.dp)) {
            Column {
                Text("GAME CODE", color = Cream.copy(alpha = .65f), fontSize = 12.sp, letterSpacing = 2.sp)
                Text(ui.gameCode, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
            }
            IconButton(onClick = refresh, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Outlined.Refresh, "Refresh lobby", tint = Cream)
            }
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, null, tint = Rust)
                Spacer(Modifier.width(10.dp))
                Text("Lobby", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("${ui.players.size}/7", color = Ink.copy(alpha = .6f))
            }
            Text(
                if (ui.players.size < 4) "Waiting for ${4 - ui.players.size} more player${if (4 - ui.players.size == 1) "" else "s"}…"
                else if (ui.isHost) "Your posse is ready. Deal when everyone has joined."
                else "The posse is ready. Waiting for the host to deal.",
                color = Ink.copy(alpha = .68f), modifier = Modifier.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(10.dp))
            ui.players.forEachIndexed { index, player ->
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).background(Gold.copy(alpha = .22f), RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center,
                        ) { Text("${index + 1}", fontWeight = FontWeight.Bold, color = Rust) }
                        Spacer(Modifier.width(12.dp))
                        Text(player, fontWeight = FontWeight.SemiBold)
                        if (player == ui.playerName) {
                            Spacer(Modifier.weight(1f)); Text("YOU", color = Rust, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(ui.isHost) {
                Button(
                    onClick = deal,
                    enabled = ui.players.size in 4..7 && !ui.loading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("DEAL ROLES", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = reveal, enabled = !ui.loading, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Outlined.Visibility, null); Spacer(Modifier.width(8.dp)); Text("PRIVATELY REVEAL MY ROLE", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = leave, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("LEAVE LOBBY") }
        }
    }

    ui.role?.let { role -> RoleDialog(role, hideRole) }
}

@Composable
private fun RoleDialog(role: String, hide: () -> Unit) {
    AlertDialog(
        onDismissRequest = hide,
        title = { Text("Your secret identity", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Make sure no one is looking.", color = Ink.copy(alpha = .65f))
                Spacer(Modifier.height(24.dp))
                Surface(color = Rust, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        role.replace('_', ' '), color = Color.White, fontSize = 30.sp,
                        fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 34.dp, horizontal = 12.dp),
                    )
                }
            }
        },
        confirmButton = { Button(onClick = hide, modifier = Modifier.fillMaxWidth()) { Text("HIDE MY ROLE") } },
    )
}
