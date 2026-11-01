package de.fabiobaensch.glitchy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import de.fabiobaensch.glitchy.data.Repo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val USER_RE = Regex("^[A-Za-z0-9_]{3,20}$")

@Composable
fun UsernameStatus(user: String, free: Boolean?) {
    val txt = when {
        user.isEmpty() -> ""
        !USER_RE.matches(user) -> tr("3–20 Zeichen: Buchstaben, Zahlen, _", "3–20 chars: letters, digits, _")
        free == null -> tr("Prüfe …", "Checking …")
        free -> tr("✓ Username ist frei", "✓ Username is available")
        else -> tr("✗ Username ist schon vergeben", "✗ Username is already taken")
    }
    Text(txt, fontSize = 13.sp, color = if (free == true) Neon else if (free == false) Danger else Lilac)
}

@Composable
fun AuthScreen(onAuthed: () -> Unit, onLegal: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var pw by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var accept by remember { mutableStateOf(false) }
    var userFree by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user, register) {
        userFree = null
        if (register && USER_RE.matches(user)) {
            delay(450)
            userFree = runCatching { Repo.usernameFree(user) }.getOrNull()
        }
    }

    val canSubmit = !busy && email.contains("@") && pw.length >= 8 &&
        (!register || (USER_RE.matches(user) && userFree == true && accept))

    fun submit() {
        scope.launch {
            busy = true; error = null; info = null
            try {
                if (register) Repo.signUp(email.trim(), pw, user) else Repo.signIn(email.trim(), pw)
                if (Repo.uid == null) {
                    // E-Mail-Bestätigung ist in Supabase aktiv -> noch keine Session
                    info = tr("Wir haben dir eine Bestätigungs-Mail geschickt. Bestätige sie und logge dich dann ein.",
                        "We sent you a confirmation email. Confirm it, then log in.")
                    register = false
                } else {
                    Haptics.success(ctx); onAuthed()
                }
            } catch (e: Exception) {
                Haptics.error(ctx); error = e.message?.take(180) ?: "Error"
            } finally { busy = false }
        }
    }

    fun google() {
        scope.launch {
            busy = true; error = null
            try {
                Repo.google(ctx)
                Haptics.success(ctx); onAuthed()
            } catch (_: GetCredentialCancellationException) {
            } catch (e: Exception) {
                Haptics.error(ctx); error = e.message?.take(180) ?: "Google error"
            } finally { busy = false }
        }
    }

    Column(
        Modifier.fillMaxSize().glitchBackground().statusBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelMascot(100.dp)
        Text("Glitchy", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Pink)
        Spacer(Modifier.height(16.dp))
        TabRow(selectedTabIndex = if (register) 1 else 0, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            Tab(!register, { register = false; Haptics.tick(ctx) }, text = { Text(tr("Anmelden", "Log in")) })
            Tab(register, { register = true; Haptics.tick(ctx) }, text = { Text(tr("Registrieren", "Sign up")) })
        }
        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(register) {
            Column {
                OutlinedTextField(
                    user, { user = it.take(20) }, label = { Text("Username") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                UsernameStatus(user, userFree)
                Spacer(Modifier.height(8.dp))
            }
        }
        OutlinedTextField(
            email, { email = it }, label = { Text("E-Mail") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            pw, { pw = it }, label = { Text(tr("Passwort (min. 8 Zeichen)", "Password (min. 8 chars)")) }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(register) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(accept, { accept = it })
                Text(tr("Ich habe die ", "I have read the "), fontSize = 13.sp)
                TextButton(onClick = onLegal) { Text(tr("Datenschutzerklärung", "Privacy Policy"), fontSize = 13.sp) }
                Text(tr(" gelesen und bin mind. 16.", " and I am 16+."), fontSize = 13.sp)
            }
        }
        error?.let { Text(it, color = Danger, fontSize = 13.sp, textAlign = TextAlign.Center) }
        info?.let { Text(it, color = Neon, fontSize = 13.sp, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { submit() }, enabled = canSubmit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp)
            else Text(if (register) tr("Konto erstellen", "Create account") else tr("Anmelden", "Log in"))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { google() }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(tr("Weiter mit Google", "Continue with Google"))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Mit Google akzeptierst du die ", "By using Google you accept the "), fontSize = 12.sp)
            TextButton(onClick = onLegal) { Text(tr("Datenschutzerklärung", "Privacy Policy"), fontSize = 12.sp) }
        }
    }
}

/** Nach Google-Login ohne Username: einmaligen Username wählen. */
@Composable
fun UsernameScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf("") }
    var free by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(user) {
        free = null
        if (USER_RE.matches(user)) { delay(450); free = runCatching { Repo.usernameFree(user) }.getOrNull() }
    }

    Column(
        Modifier.fillMaxSize().glitchBackground().statusBarsPadding().imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        PixelMascot(110.dp)
        Text(tr("Wähle deinen Username", "Choose your username"), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(tr("Er ist einmalig und für andere in den Leaderboards sichtbar.",
            "It is unique and visible to others on the leaderboards."), color = Lilac, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(user, { user = it.take(20) }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        UsernameStatus(user, free)
        error?.let { Text(it, color = Danger, fontSize = 13.sp) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    busy = true; error = null
                    try { Repo.setUsername(user); Haptics.success(ctx); onDone() }
                    catch (e: Exception) { Haptics.error(ctx); error = tr("Username konnte nicht gespeichert werden (evtl. vergeben).", "Could not save username (maybe taken).") }
                    finally { busy = false }
                }
            },
            enabled = free == true && !busy, modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(tr("Speichern", "Save")) }
    }
}
