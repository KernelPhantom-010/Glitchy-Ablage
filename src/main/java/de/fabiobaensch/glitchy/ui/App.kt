package de.fabiobaensch.glitchy.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.fabiobaensch.glitchy.AppVm
import de.fabiobaensch.glitchy.data.Backend
import de.fabiobaensch.glitchy.data.Repo
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun GlitchyApp(vm: AppVm = viewModel()) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ctx.getSharedPreferences("glitchy", 0) }
    var start by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { Backend.client.auth.awaitInitialization() }
        start = if (Repo.uid == null) "onboarding" else "home"
        if (start == "home") vm.refresh()
    }

    fun afterAuth() {
        scope.launch {
            vm.refreshNow()
            nav.navigate(if (vm.profile?.username == null) "username" else "home") { popUpTo(0) }
        }
    }

    fun loggedOut() {
        vm.clear()
        nav.navigate("onboarding") { popUpTo(0) }
    }

    val s = start
    if (s == null) {
        Box(Modifier.fillMaxSize().glitchBackground(), contentAlignment = Alignment.Center) { PixelMascot(120.dp) }
    } else {
        NavHost(
            navController = nav, startDestination = s,
            enterTransition = { slideInHorizontally(tween(300)) { it / 4 } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(200)) }
        ) {
            composable("onboarding") {
                Onboarding {

                    nav.navigate("auth") { popUpTo("onboarding") { inclusive = true } }
                }
            }
            composable("auth") { AuthScreen(onAuthed = { afterAuth() }, onLegal = { nav.navigate("legal") }) }
            composable("username") { UsernameScreen(onDone = { afterAuth() }) }
            composable("home") { Home(vm, nav, onLogout = { loggedOut() }) }
            composable("module/{mid}") { e -> ModuleScreen(vm, nav, e.arguments?.getString("mid") ?: "") }
            composable("lesson/{mid}/{lid}") { e ->
                LessonScreen(vm, nav, e.arguments?.getString("mid") ?: "", e.arguments?.getString("lid") ?: "")
            }
            composable("test/{mid}") { e -> TestScreen(vm, nav, e.arguments?.getString("mid") ?: "") }
            composable("legal") { LegalScreen(onBack = { nav.popBackStack() }) }
        }
    }
}

@Composable
private fun Home(vm: AppVm, nav: NavController, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(0) }
    val items = listOf(
        Icons.Default.School to tr("Lernen", "Learn"),
        Icons.Default.LocalFireDepartment to "Daily",
        Icons.Default.EmojiEvents to "Ranking",
        Icons.Default.Person to tr("Profil", "Profile")
    )
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.glitchBackground(),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E0721)) {
                items.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { if (tab != i) { tab = i; Haptics.tick(ctx) } },
                        icon = { Icon(icon, null) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Violet.copy(alpha = 0.45f))
                    )
                }
            }
        }
    ) { pad ->
        AnimatedContent(
            targetState = tab, modifier = Modifier.padding(pad),
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "tabs"
        ) { t ->
            when (t) {
                0 -> LearnTab(vm, nav)
                1 -> DailyTab(vm)
                2 -> LeaderboardTab(vm)
                else -> ProfileTab(vm, onLegal = { nav.navigate("legal") }, onLogout = onLogout)
            }
        }
    }
}
