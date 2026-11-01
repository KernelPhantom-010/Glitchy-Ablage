package de.fabiobaensch.glitchy.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.fabiobaensch.glitchy.AppVm
import de.fabiobaensch.glitchy.data.Block
import de.fabiobaensch.glitchy.data.Repo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ------------------------------------------------------------------
// Lernen-Tab: Modul-Übersicht
// ------------------------------------------------------------------
@Composable
fun LearnTab(vm: AppVm, nav: NavController) {
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelMascot(64.dp)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(
                        tr("Hi ${vm.profile?.username ?: ""}!", "Hi ${vm.profile?.username ?: ""}!"),
                        fontSize = 24.sp, fontWeight = FontWeight.ExtraBold
                    )
                    Text(tr("Mach heute eine Aufgabe – bleib in den Rankings.", "Solve one task today – stay on the leaderboard."), color = Lilac, fontSize = 13.sp)
                }
            }
        }
        items(vm.catalog) { m ->
            val p = vm.percent(m.lessons.map { it.id })
            GlowCard(Modifier.clickable { Haptics.tick(nav.context); nav.navigate("module/${m.id}") }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m.titleL(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("$p %", color = Pink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Text("~${m.hours} ${tr("Stunden", "hours")}", color = Lilac, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                GradientBar(p)
                Spacer(Modifier.height(6.dp))
                m.sections.forEach { s ->
                    val sp = vm.percent(s.lessons.map { it.id })
                    Text("• ${s.titleL()}: $sp %", fontSize = 13.sp, color = MaterialColors.muted)
                }
            }
        }
    }
}

private object MaterialColors { val muted = Color(0xFFC9BDE8) }

// ------------------------------------------------------------------
// Modul-Detail
// ------------------------------------------------------------------
@Composable
fun ModuleScreen(vm: AppVm, nav: NavController, mid: String) {
    val m = vm.module(mid) ?: return
    var certDialog by remember { mutableStateOf(false) }
    val p = vm.percent(m.lessons.map { it.id })
    val best = vm.best[m.id]

    ScreenScaffold(m.titleL(), onBack = { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("$p % ${tr("abgeschlossen", "completed")}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(6.dp)); GradientBar(p)
            }
            items(m.sections) { s ->
                val sp = vm.percent(s.lessons.map { it.id })
                GlowCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.titleL(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("$sp %", color = Pink, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp)); GradientBar(sp); Spacer(Modifier.height(8.dp))
                    s.lessons.forEach { l ->
                        val d = l.id in vm.done
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { Haptics.tick(nav.context); nav.navigate("lesson/${m.id}/${l.id}") }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (d) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (d) Neon else Lilac)
                            Text("  ${l.number}  ${l.titleL()}", fontSize = 15.sp)
                        }
                    }
                }
            }
            item {
                GlowCard {
                    Text(tr("Abschlusstest", "Final test"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        tr("${m.finalTest.count { it.interactive }} Fragen · Bestes Ergebnis: ${best?.let { "$it %" } ?: "–"}",
                            "${m.finalTest.count { it.interactive }} questions · Best result: ${best?.let { "$it %" } ?: "–"}"),
                        color = Lilac, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { nav.navigate("test/${m.id}") }, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Test starten", "Start test"))
                    }
                    if (vm.certificateEligible(m)) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { certDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("🎓 Zertifikat (PDF) erstellen", "🎓 Create certificate (PDF)"))
                        }
                    } else {
                        Text(
                            tr("Zertifikat: Modul 100 % abschließen und Test mit mind. 95 % bestehen.",
                                "Certificate: complete 100 % of the module and score at least 95 % on the test."),
                            fontSize = 12.sp, color = Lilac, modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
    if (certDialog) CertificateDialog(m) { certDialog = false }
}

// ------------------------------------------------------------------
// Lektion
// ------------------------------------------------------------------
@Composable
fun LessonScreen(vm: AppVm, nav: NavController, mid: String, lid: String) {
    val m = vm.module(mid) ?: return
    val idx = m.lessons.indexOfFirst { it.id == lid }
    val l = m.lessons.getOrNull(idx) ?: return
    val ctx = LocalContext.current
    val solved = remember(lid) { mutableStateMapOf<Int, Boolean>() }
    val interactiveIdx = l.blocks.withIndex().filter { it.value.interactive }.map { it.index }
    val allSolved = interactiveIdx.all { solved[it] == true }
    val isDone = l.id in vm.done

    ScreenScaffold("${l.number}  ${l.titleL()}", onBack = { nav.popBackStack() }) {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(l.blocks) { i, b ->
                BlockView(b) { ok -> if (ok) { solved[i] = true; vm.logSolved() } }
            }
            item {
                GlowCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isDone,
                            enabled = !isDone && allSolved,
                            onCheckedChange = { if (it) { vm.lessonDone(l.id); Haptics.success(ctx) } }
                        )
                        Column {
                            Text("${l.number} ${l.titleL()} – ${tr("abgeschlossen", "completed")}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (!isDone && !allSolved)
                                Text(tr("Löse erst alle Aufgaben, dann kannst du abhaken.", "Solve all tasks first, then tick the box."), fontSize = 12.sp, color = Lilac)
                        }
                    }
                    if (isDone) {
                        m.lessons.getOrNull(idx + 1)?.let { next ->
                            TextButton(onClick = {
                                nav.navigate("lesson/${m.id}/${next.id}") { popUpTo("module/${m.id}") }
                            }) { Text(tr("Weiter zu ${next.number} →", "Next: ${next.number} →")) }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// Aufgaben-Blöcke
// ------------------------------------------------------------------
@Composable
fun BlockView(b: Block, hints: Boolean = true, onAnswered: (Boolean) -> Unit) {
    when (b.type) {
        "text" -> Text(b.textL(), fontSize = 16.sp, lineHeight = 24.sp)
        "quiz" -> QuizBlock(b, onAnswered)
        else -> InputBlock(b, hints, onAnswered)
    }
}

@Composable
private fun rememberShake(): Pair<Animatable<Float, AnimationVector1D>, suspend () -> Unit> {
    val a = remember { Animatable(0f) }
    val run: suspend () -> Unit = {
        repeat(3) { a.animateTo(12f, tween(40)); a.animateTo(-12f, tween(40)) }
        a.animateTo(0f, tween(40))
    }
    return a to run
}

@Composable
fun InputBlock(b: Block, hints: Boolean, onAnswered: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val term = b.type == "terminal"
    var input by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<Boolean?>(null) }
    var showHint by remember { mutableStateOf(false) }
    val (shake, doShake) = rememberShake()
    val border by animateColorAsState(
        when (state) { true -> Neon; false -> Danger; null -> Violet }, label = "border"
    )

    fun check() {
        val ok = b.check(input)
        state = ok
        onAnswered(ok)
        if (ok) Haptics.success(ctx) else { Haptics.error(ctx); scope.launch { doShake() } }
    }

    Column(
        Modifier.fillMaxWidth()
            .offset { IntOffset(shake.value.roundToInt(), 0) }
            .clip(RoundedCornerShape(16.dp))
            .background(if (term) Color(0xFF05020B) else CardBg)
            .padding(14.dp)
    ) {
        Text(
            (if (term) "▶ " else "✎ ") + b.promptL(),
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = input, onValueChange = { input = it; if (state == false) state = null },
            enabled = state != true, singleLine = true, modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = if (term) FontFamily.Monospace else FontFamily.Default, color = if (term) Neon else Color.White),
            leadingIcon = if (term) ({ Text("$", color = Neon, fontFamily = FontFamily.Monospace) }) else null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) check() }),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = border, unfocusedBorderColor = border)
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { check() }, enabled = input.isNotBlank() && state != true) { Text(tr("Prüfen", "Check")) }
            if (hints && b.hintL().isNotBlank() && state != true) {
                TextButton(onClick = { showHint = !showHint }) { Text(tr("Tipp", "Hint")) }
            }
            Spacer(Modifier.width(8.dp))
            when (state) {
                true -> Text(tr("✓ Richtig!", "✓ Correct!"), color = Neon, fontWeight = FontWeight.Bold)
                false -> Text(tr("✗ Noch nicht – versuch's nochmal", "✗ Not yet – try again"), color = Danger, fontSize = 13.sp)
                null -> {}
            }
        }
        if (showHint && hints) Text("💡 " + b.hintL(), color = Lilac, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        if (term && state == true && b.output.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(b.output, color = Neon, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

@Composable
fun QuizBlock(b: Block, onAnswered: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var picked by remember { mutableStateOf<Int?>(null) }
    var solved by remember { mutableStateOf(false) }
    val (shake, doShake) = rememberShake()

    Column(
        Modifier.fillMaxWidth().offset { IntOffset(shake.value.roundToInt(), 0) }
            .clip(RoundedCornerShape(16.dp)).background(CardBg).padding(14.dp)
    ) {
        Text("❓ " + b.promptL(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        b.options.forEachIndexed { i, o ->
            val color = when {
                solved && i == b.correct -> Neon
                picked == i && !solved -> Danger
                else -> Violet
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF241447))
                    .clickable(enabled = !solved) {
                        picked = i
                        val ok = i == b.correct
                        onAnswered(ok)
                        if (ok) { solved = true; Haptics.success(ctx) } else { Haptics.error(ctx); scope.launch { doShake() } }
                    }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(6.dp).height(22.dp).background(color, RoundedCornerShape(3.dp)))
                Text("  $o", fontSize = 15.sp)
            }
        }
        if (solved) Text(tr("✓ Richtig!", "✓ Correct!"), color = Neon, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    }
}

// ------------------------------------------------------------------
// Abschlusstest (ein Versuch pro Frage zählt)
// ------------------------------------------------------------------
@Composable
fun TestScreen(vm: AppVm, nav: NavController, mid: String) {
    val m = vm.module(mid) ?: return
    val qs = remember { m.finalTest.filter { it.interactive } }
    var idx by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var attempted by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val score = if (qs.isEmpty()) 0 else correct * 100 / qs.size

    LaunchedEffect(finished) {
        if (finished) {
            vm.recordBest(m.id, score)
            runCatching { Repo.saveTest(m.id, score) }
            vm.logSolved()
        }
    }

    ScreenScaffold(tr("Abschlusstest – ${m.titleL()}", "Final test – ${m.titleL()}"), onBack = { nav.popBackStack() }) {
        if (qs.isEmpty()) {
            Text(tr("Für dieses Modul gibt es noch keine Testfragen.", "No test questions for this module yet."), Modifier.padding(16.dp))
        } else if (!finished) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${idx + 1} / ${qs.size}", color = Lilac)
                GradientBar(idx * 100 / qs.size)
                key(idx) {
                    BlockView(qs[idx], hints = false) { ok ->
                        if (!attempted) { attempted = true; if (ok) correct++ }
                    }
                }
                Button(
                    onClick = {
                        attempted = false
                        if (idx + 1 >= qs.size) finished = true else idx++
                    },
                    enabled = attempted, modifier = Modifier.fillMaxWidth()
                ) { Text(if (idx + 1 >= qs.size) tr("Fertig", "Finish") else tr("Weiter", "Next")) }
                Text(tr("Nur der erste Versuch pro Frage zählt.", "Only your first attempt per question counts."), fontSize = 12.sp, color = Lilac)
            }
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                PixelMascot(110.dp)
                Text("$score %", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = if (score >= 95) Neon else Pink)
                Text("$correct / ${qs.size} ${tr("richtig", "correct")}")
                Spacer(Modifier.height(8.dp))
                Text(
                    if (score >= 95) tr("Mega! Wenn du das Modul zu 100 % abgeschlossen hast, kannst du jetzt dein Zertifikat erstellen.",
                        "Awesome! If you completed 100 % of the module you can now create your certificate.")
                    else tr("Für das Zertifikat brauchst du mind. 95 %. Versuch's nochmal!", "You need at least 95 % for the certificate. Try again!"),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { nav.popBackStack() }) { Text(tr("Zurück zum Modul", "Back to module")) }
            }
        }
    }
}
