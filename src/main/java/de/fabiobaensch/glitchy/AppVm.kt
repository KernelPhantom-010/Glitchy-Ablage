package de.fabiobaensch.glitchy

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.fabiobaensch.glitchy.data.Catalog
import de.fabiobaensch.glitchy.data.Module
import de.fabiobaensch.glitchy.data.Profile
import de.fabiobaensch.glitchy.data.Repo
import kotlinx.coroutines.launch
import java.time.LocalDate

class AppVm(app: Application) : AndroidViewModel(app) {
    val catalog: List<Module> = Catalog.load(app)

    var done by mutableStateOf<Set<String>>(emptySet())
    var profile by mutableStateOf<Profile?>(null)
    var best by mutableStateOf<Map<String, Int>>(emptyMap())

    private var lastSolvedLogDay = -1L

    fun module(id: String) = catalog.find { it.id == id }

    fun percent(lessonIds: List<String>): Int =
        if (lessonIds.isEmpty()) 0 else lessonIds.count { it in done } * 100 / lessonIds.size

    fun moduleComplete(m: Module) = percent(m.lessons.map { it.id }) == 100
    fun certificateEligible(m: Module) = moduleComplete(m) && (best[m.id] ?: 0) >= 95

    suspend fun refreshNow() {
        runCatching { done = Repo.loadDone() }
        runCatching { profile = Repo.myProfile() }
        runCatching { best = Repo.bestScores() }
    }

    fun refresh() { viewModelScope.launch { refreshNow() } }

    fun lessonDone(id: String) {
        done = done + id
        viewModelScope.launch { runCatching { Repo.markLessonDone(id) } }
    }

    /** Mindestens eine Aufgabe gelöst -> zählt für das "aktiv"-Leaderboard (1x pro Tag senden). */
    fun logSolved() {
        val today = LocalDate.now().toEpochDay()
        if (lastSolvedLogDay == today) return
        lastSolvedLogDay = today
        viewModelScope.launch { runCatching { Repo.logActivity("solved") } }
    }

    fun logChallenge() {
        viewModelScope.launch { runCatching { Repo.logActivity("challenge") } }
    }

    fun recordBest(moduleId: String, score: Int) {
        best = best + (moduleId to maxOf(score, best[moduleId] ?: 0))
    }

    fun clear() {
        done = emptySet(); profile = null; best = emptyMap(); lastSolvedLogDay = -1L
    }
}
