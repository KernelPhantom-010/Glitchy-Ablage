package de.fabiobaensch.glitchy.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Datenschutz + Impressum aus assets/legal/. Die Texte sind VORLAGEN – bitte ausfüllen! */
@Composable
fun LegalScreen(onBack: () -> Unit, startTab: Int = 0) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(startTab) }
    val lang = if (isDe()) "de" else "en"
    val file = if (tab == 0) "legal/privacy_$lang.txt" else "legal/imprint_$lang.txt"
    val body = remember(file) { ctx.assets.open(file).bufferedReader().use { it.readText() } }

    ScreenScaffold(tr("Rechtliches", "Legal"), onBack) {
        TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
            Tab(tab == 0, { tab = 0 }, text = { Text(tr("Datenschutz", "Privacy")) })
            Tab(tab == 1, { tab = 1 }, text = { Text(tr("Impressum", "Imprint")) })
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(body, fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}
