package de.fabiobaensch.glitchy.ui

import android.Manifest
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.fabiobaensch.glitchy.AppVm
import de.fabiobaensch.glitchy.Certificate
import de.fabiobaensch.glitchy.Reminder
import de.fabiobaensch.glitchy.data.Block
import de.fabiobaensch.glitchy.data.LbRow
import de.fabiobaensch.glitchy.data.Module
import de.fabiobaensch.glitchy.data.Repo
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.time.LocalDate

// ------------------------------------------------------------------
// Daily Challenge
// ------------------------------------------------------------------
@Composable
fun DailyTab(vm: AppVm) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("glitchy", 0) }
    val today = LocalDate.now().toEpochDay()
    var doneToday by remember { mutableStateOf(prefs.getLong("daily_done_day", -1) == today) }

    // Pool: nur Aufgaben aus bereits ABGESCHLOSSENEN Lektionen -> nie unfair
    val pool: List<Pair<String, Block>> = remember(vm.done) {
        vm.catalog.flatMap { m ->
            m.lessons.filter { it.id in vm.done }.flatMap { l ->
                l.blocks.mapIndexedNotNull { i, b -> if (b.interactive) "${l.id}#$i" to b else null }
            }
        }
    }
    // Die Tagesaufgabe wird beim ersten Öffnen festgelegt und bleibt den ganzen Tag gleich.
    val challenge: Pair<String, Block>? = remember(pool) {
        if (pool.isEmpty()) null else {
            val storedKey = if (prefs.getLong("daily_day", -1) == today) prefs.getString("daily_key", null) else null
            pool.find { it.first == storedKey } ?: pool[((today + (Repo.uid?.hashCode() ?: 0)).mod(pool.size.toLong())).toInt()].also {
                prefs.edit().putLong("daily_day", today).putString("daily_key", it.first).apply()
            }
        }
    }

    var reminderOn by remember { mutableStateOf(prefs.getBoolean("reminder_on", false)) }
    var hour by remember { mutableIntStateOf(prefs.getInt("reminder_h", 8)) }
    var minute by remember { mutableIntStateOf(prefs.getInt("reminder_m", 0)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("🔥 " + tr("Daily Challenge", "Daily Challenge"), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        when {
            challenge == null -> GlowCard {
                Text(tr("Schließe zuerst eine Lektion ab – dann bekommst du hier jeden Tag eine Aufgabe aus deinem bereits gelernten Stoff.",
                    "Complete a lesson first – then you get a daily task from material you already learned."))
            }
            doneToday -> GlowCard {
                Text(tr("✓ Heute erledigt! Komm morgen wieder.", "✓ Done for today! Come back tomorrow."), color = Neon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            else -> androidx.compose.runtime.key(challenge.first) {
                BlockView(challenge.second) { ok ->
                    if (ok) {
                        doneToday = true
                        prefs.edit().putLong("daily_done_day", today).apply()
                        vm.logChallenge(); vm.logSolved()
                    }
                }
            }
        }

        GlowCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(tr("Tägliche Erinnerung", "Daily reminder"), fontWeight = FontWeight.Bold)
                    Text("%02d:%02d".format(hour, minute), color = Pink, fontSize = 22.sp)
                }
                Switch(reminderOn, { on ->
                    reminderOn = on
                    prefs.edit().putBoolean("reminder_on", on).apply()
                    if (on) {
                        if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        Reminder.schedule(ctx, hour, minute)
                    } else Reminder.cancel(ctx)
                })
            }
            TextButton(onClick = {
                TimePickerDialog(ctx, { _, h, m ->
                    hour = h; minute = m
                    prefs.edit().putInt("reminder_h", h).putInt("reminder_m", m).apply()
                    if (reminderOn) Reminder.schedule(ctx, h, m)
                }, hour, minute, true).show()
            }) { Text(tr("Uhrzeit ändern", "Change time")) }
        }
    }
}

// ------------------------------------------------------------------
// Leaderboards
// ------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeaderboardTab(vm: AppVm) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("activity") }
    var days by remember { mutableIntStateOf(7) }
    var rows by remember { mutableStateOf<List<LbRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reportTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(kind, days) {
        loading = true
        rows = runCatching { Repo.leaderboard(kind, days) }.getOrDefault(emptyList())
        loading = false
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
        Text("🏆 Leaderboard", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(kind == "activity", { kind = "activity" }, label = { Text(tr("Täglich aktiv", "Daily active")) })
            FilterChip(kind == "challenge", { kind = "challenge" }, label = { Text("Daily Challenge") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            FilterChip(days == 7, { days = 7 }, label = { Text("7 Day") })
            FilterChip(days == 30, { days = 30 }, label = { Text("30 Day") })
            FilterChip(days == 36500, { days = 36500 }, label = { Text("Lifetime") })
        }
        if (!loading && rows.isEmpty()) Text(tr("Noch keine Einträge – sei der Erste!", "No entries yet – be the first!"), color = Lilac)
        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(rows) { i, r ->
                val me = r.username == vm.profile?.username
                Row(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                        .background14(me)
                        .combinedClickable(onClick = {}, onLongClick = { if (!me) reportTarget = r.username })
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(listOf("🥇", "🥈", "🥉").getOrNull(i) ?: "${i + 1}.", modifier = Modifier.width(36.dp), fontSize = 18.sp)
                    Avatar(r.avatarUrl, 38)
                    Text(r.username, modifier = Modifier.padding(start = 10.dp).weight(1f), fontWeight = if (me) FontWeight.Bold else FontWeight.Normal)
                    Text("${r.score} ${tr("Tage", "days")}", color = Pink, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    reportTarget?.let { u ->
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text(tr("$u melden?", "Report $u?")) },
            text = { Text(tr("Unangemessener Username oder unangemessenes Profilbild.", "Inappropriate username or profile picture.")) },
            confirmButton = { TextButton(onClick = { scope.launch { runCatching { Repo.report(u) } }; reportTarget = null }) { Text(tr("Melden", "Report")) } },
            dismissButton = { TextButton(onClick = { reportTarget = null }) { Text(tr("Abbrechen", "Cancel")) } }
        )
    }
}

private fun Modifier.background14(highlight: Boolean) =
    this.background(if (highlight) Violet.copy(alpha = 0.35f) else CardBg)

@Composable
fun Avatar(url: String?, size: Int) {
    if (url.isNullOrBlank()) {
        Box(Modifier.size(size.dp).clip(CircleShape).background14(true), contentAlignment = Alignment.Center) { Text("👾") }
    } else {
        AsyncImage(url, null, Modifier.size(size.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    }
}

// ------------------------------------------------------------------
// Profil
// ------------------------------------------------------------------
@Composable
fun ProfileTab(vm: AppVm, onLegal: () -> Unit, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("glitchy", 0) }
    var avatar by remember { mutableStateOf(vm.profile?.avatarUrl) }
    var haptics by remember { mutableStateOf(Haptics.enabled) }
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var certModule by remember { mutableStateOf<Module?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching {
                val bmp = ctx.contentResolver.openInputStream(uri)!!.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 })
                }!!
                val s = 512f / maxOf(bmp.width, bmp.height)
                val scaled = if (s < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt(), (bmp.height * s).toInt(), true) else bmp
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                avatar = Repo.uploadAvatar(out.toByteArray())
            }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(avatar, 110)
        Text(vm.profile?.username ?: "", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !busy) {
            Text(tr("Profilbild ändern", "Change profile picture"))
        }
        Text(tr("Username und Profilbild sind für andere sichtbar.", "Username and picture are visible to others."), fontSize = 12.sp, color = Lilac)

        GlowCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Vibrationseffekte", "Vibration effects"))
                Switch(haptics, { haptics = it; Haptics.enabled = it; prefs.edit().putBoolean("haptics", it).apply() })
            }
        }

        GlowCard {
            Text("🎓 " + tr("Zertifikate", "Certificates"), fontWeight = FontWeight.Bold)
            vm.catalog.forEach { m ->
                val ok = vm.certificateEligible(m)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m.titleL(), modifier = Modifier.weight(1f))
                    Button(onClick = { certModule = m }, enabled = ok) { Text(if (ok) "PDF" else "🔒") }
                }
            }
        }

        OutlinedButton(onClick = onLegal, modifier = Modifier.fillMaxWidth()) { Text(tr("Datenschutz & Impressum", "Privacy & Imprint")) }
        OutlinedButton(onClick = { scope.launch { runCatching { Repo.signOut() }; onLogout() } }, modifier = Modifier.fillMaxWidth()) { Text(tr("Abmelden", "Log out")) }
        TextButton(onClick = { confirmDelete = true }) { Text(tr("Konto endgültig löschen", "Delete account permanently"), color = Danger) }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(tr("Konto löschen?", "Delete account?")) },
        text = { Text(tr("Alle deine Daten (Profil, Fortschritt, Ergebnisse) werden unwiderruflich gelöscht.", "All your data (profile, progress, results) will be permanently deleted.")) },
        confirmButton = {
            TextButton(onClick = {
                confirmDelete = false
                scope.launch { runCatching { Repo.deleteAccount() }; onLogout() }
            }) { Text(tr("Löschen", "Delete"), color = Danger) }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Abbrechen", "Cancel")) } }
    )
    certModule?.let { CertificateDialog(it) { certModule = null } }
}

// ------------------------------------------------------------------
// Zertifikat-Dialog: Nutzer tippt seinen vollen Namen, dann wird das PDF erzeugt
// ------------------------------------------------------------------
@Composable
fun CertificateDialog(m: Module, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(tr("Zertifikat erstellen", "Create certificate")) },
        text = {
            Column {
                Text(tr("Gib deinen vollständigen Namen ein – er wird so im PDF stehen und lässt sich später nicht ändern.",
                    "Enter your full name – it will appear exactly like this on the PDF."), fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it.take(60) }, singleLine = true, label = { Text(tr("Vor- und Nachname", "Full name")) })
                error?.let { Text(it, color = Danger, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(enabled = name.trim().length >= 3 && !busy, onClick = {
                scope.launch {
                    busy = true; error = null
                    try {
                        val score = runCatching { Repo.bestScores()[m.id] }.getOrNull() ?: 95
                        val id = Repo.issueCertificate(m.id, name.trim())
                        val file = Certificate.create(ctx, name.trim(), m.title, score, id)
                        Haptics.success(ctx)
                        Certificate.share(ctx, file)
                        onClose()
                    } catch (e: Exception) {
                        Haptics.error(ctx)
                        error = tr("Konnte nicht erstellt werden (Internet? Test ≥ 95 %?).", "Could not create (internet? test ≥ 95 %?).")
                    } finally { busy = false }
                }
            }) { Text(tr("PDF erstellen", "Create PDF")) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(tr("Abbrechen", "Cancel")) } }
    )
}
