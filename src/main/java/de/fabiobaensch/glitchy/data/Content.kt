package de.fabiobaensch.glitchy.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Lerninhalte liegen als JSON in app/src/main/assets/content/.
 * Block-Typen:
 *  - "text":     Erklärtext
 *  - "practice": Eintipp-Aufgabe (prompt + answers; Leerzeichen und * werden beim Vergleich ignoriert)
 *  - "terminal": Terminal-Aufgabe (answers = Regex-Muster, output = simulierte Ausgabe)
 *  - "quiz":     Multiple Choice (prompt, options, correct = Index)
 */
@Serializable
data class Block(
    val type: String,
    val text: String = "",
    val textEn: String = "",
    val prompt: String = "",
    val promptEn: String = "",
    val answers: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val correct: Int = 0,
    val output: String = "",
    val hint: String = "",
    val hintEn: String = ""
) {
    private fun de() = Locale.getDefault().language == "de"
    fun textL() = if (!de() && textEn.isNotBlank()) textEn else text
    fun promptL() = if (!de() && promptEn.isNotBlank()) promptEn else prompt
    fun hintL() = if (!de() && hintEn.isNotBlank()) hintEn else hint
    val interactive get() = type != "text"

    private fun norm(s: String) = s.lowercase().replace(Regex("[\\s*]"), "")

    fun check(input: String): Boolean = when (type) {
        "practice" -> answers.any { norm(it) == norm(input) }
        "terminal" -> answers.any {
            runCatching { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(input.trim()) }.getOrDefault(false)
        }
        else -> false
    }
}

@Serializable
data class Lesson(val id: String, val title: String, val titleEn: String = "", val blocks: List<Block>) {
    fun titleL() = if (Locale.getDefault().language != "de" && titleEn.isNotBlank()) titleEn else title
    /** "p1.1" -> "1.1" */
    val number get() = id.dropWhile { it.isLetter() }
}

@Serializable
data class Section(val id: String, val title: String, val titleEn: String = "", val lessons: List<Lesson>) {
    fun titleL() = if (Locale.getDefault().language != "de" && titleEn.isNotBlank()) titleEn else title
}

@Serializable
data class Module(
    val id: String,
    val title: String,
    val titleEn: String = "",
    val hours: String = "20–30",
    val sections: List<Section>,
    val finalTest: List<Block> = emptyList()
) {
    fun titleL() = if (Locale.getDefault().language != "de" && titleEn.isNotBlank()) titleEn else title
    val lessons get() = sections.flatMap { it.lessons }
}

object Catalog {
    private val json = Json { ignoreUnknownKeys = true }

    fun load(ctx: Context): List<Module> =
        listOf("prog", "math").map { name ->
            val text = ctx.assets.open("content/$name.json").bufferedReader().use { it.readText() }
            json.decodeFromString<Module>(text)
        }
}
