package de.fabiobaensch.glitchy.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import de.fabiobaensch.glitchy.BuildConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Profile(
    val id: String,
    val username: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class LbRow(
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val score: Long
)

@Serializable private data class LessonRow(@SerialName("lesson_id") val lessonId: String)
@Serializable private data class TestIns(@SerialName("module_id") val moduleId: String, val score: Int)
@Serializable private data class TestRow(@SerialName("module_id") val moduleId: String, val score: Int)
@Serializable private data class ReportIns(val username: String, val reason: String? = null)

object Repo {
    private val db get() = Backend.client

    val uid: String? get() = db.auth.currentUserOrNull()?.id

    // ---------- Auth ----------
    suspend fun signIn(email: String, password: String) {
        db.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String, username: String) {
        db.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject { put("username", username) }
        }
    }

    /** Google über den Android Credential Manager -> ID-Token -> Supabase. */
    suspend fun google(activityContext: Context) {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
        db.auth.signInWith(IDToken) {
            idToken = token
            provider = Google
        }
    }

    suspend fun signOut() = db.auth.signOut()

    suspend fun deleteAccount() {
        db.postgrest.rpc("delete_my_account")
        runCatching { db.auth.signOut() }
    }

    // ---------- Profil ----------
    suspend fun usernameFree(name: String): Boolean =
        db.postgrest.rpc("username_free", buildJsonObject { put("p_name", name) }).decodeAs<Boolean>()

    suspend fun myProfile(): Profile? {
        val id = uid ?: return null
        return db.from("profiles").select { filter { eq("id", id) } }.decodeSingleOrNull<Profile>()
    }

    suspend fun setUsername(name: String) {
        val id = uid ?: return
        db.from("profiles").update({ set("username", name) }) { filter { eq("id", id) } }
    }

    suspend fun uploadAvatar(jpegBytes: ByteArray): String {
        val id = uid ?: error("not logged in")
        val path = "$id/avatar.jpg"
        val bucket = db.storage.from("avatars")
        bucket.upload(path, jpegBytes) { upsert = true }
        val url = bucket.publicUrl(path) + "?t=" + System.currentTimeMillis()
        db.from("profiles").update({ set("avatar_url", url) }) { filter { eq("id", id) } }
        return url
    }

    // ---------- Fortschritt ----------
    suspend fun loadDone(): Set<String> =
        db.from("lesson_progress").select().decodeList<LessonRow>().map { it.lessonId }.toSet()

    suspend fun markLessonDone(lessonId: String) {
        db.from("lesson_progress").insert(LessonRow(lessonId))
    }

    suspend fun saveTest(moduleId: String, score: Int) {
        db.from("test_results").insert(TestIns(moduleId, score))
    }

    suspend fun bestScores(): Map<String, Int> =
        db.from("test_results").select().decodeList<TestRow>()
            .groupBy { it.moduleId }.mapValues { (_, v) -> v.maxOf { it.score } }

    // ---------- Aktivität / Leaderboards ----------
    suspend fun logActivity(kind: String) {
        db.postgrest.rpc("log_activity", buildJsonObject { put("p_kind", kind) })
    }

    suspend fun leaderboard(kind: String, days: Int): List<LbRow> =
        db.postgrest.rpc("leaderboard", buildJsonObject {
            put("p_kind", kind)
            put("p_days", days)
        }).decodeList<LbRow>()

    suspend fun report(username: String) {
        db.from("reports").insert(ReportIns(username, "profile/avatar/username"))
    }

    // ---------- Zertifikate ----------
    suspend fun issueCertificate(moduleId: String, fullName: String): String =
        db.postgrest.rpc("issue_certificate", buildJsonObject {
            put("p_module", moduleId)
            put("p_name", fullName)
        }).decodeAs<String>()
}
