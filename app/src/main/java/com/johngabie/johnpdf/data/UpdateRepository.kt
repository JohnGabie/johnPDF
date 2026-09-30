package com.johngabie.johnpdf.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

interface UpdateCheck {
    val remoteVersion: Flow<RemoteVersion?>

    /** Version code of the running build, in the same encoding as [RemoteVersion.versionCode]. */
    val currentVersionCode: Int

    /**
     * Fetches the latest release and returns it, or null when no check ran or it failed.
     * Pass [force] to bypass the throttling interval (manual "check now").
     */
    suspend fun checkForUpdate(force: Boolean = false): RemoteVersion?
}

@Serializable
data class RemoteVersion(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val checksums: Map<String, String> = emptyMap(),
)

@Serializable
private data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("assets") val assets: List<Asset> = emptyList(),
) {
    @Serializable
    data class Asset(
        @SerialName("name") val name: String,
        @SerialName("browser_download_url") val browserDownloadUrl: String,
    )
}

class UpdateRepository(
    private val dataStore: DataStore<Preferences>,
    currentVersionName: String,
    private val repoOwner: String = "JohnGabie",
    private val repoName: String = "johnPDF",
    private val checkIntervalMs: Long = 24 * 60 * 60 * 1000, // 24 hours
) : UpdateCheck {
    override val remoteVersion: Flow<RemoteVersion?> = dataStore.data.map { prefs ->
        prefs[REMOTE_VERSION]?.let { parseRemoteVersion(it) }
    }

    override val currentVersionCode: Int = parseVersionCode(currentVersionName)

    override suspend fun checkForUpdate(force: Boolean): RemoteVersion? {
        val now = System.currentTimeMillis()
        // NOTE: must be first(), not collect() - dataStore.data never completes.
        val prefs = dataStore.data.first()
        val lastCheck = prefs[LAST_CHECK_TIME] ?: 0L

        if (!force && now - lastCheck < checkIntervalMs) {
            // Throttled: reuse whatever we already know.
            return prefs[REMOTE_VERSION]?.let { parseRemoteVersion(it) }
        }

        return try {
            val release = withTimeoutOrNull(15_000L) {
                withContext(Dispatchers.IO) { fetchLatestRelease() }
            }

            if (release == null) {
                // Timed out; record the attempt so we don't hammer the API.
                dataStore.edit { it[LAST_CHECK_TIME] = now }
                return null
            }

            val remote = RemoteVersion(
                versionCode = parseVersionCode(release.tagName),
                versionName = release.tagName.removePrefix("v"),
                downloadUrl = "https://github.com/$repoOwner/$repoName/releases/tag/${release.tagName}",
            )
            dataStore.edit {
                it[REMOTE_VERSION] = serializeRemoteVersion(remote)
                it[LAST_CHECK_TIME] = now
            }
            remote
        } catch (e: Exception) {
            // Network error or parse error. Record the attempt to avoid repeated checks.
            android.util.Log.w("UpdateRepository", "Update check failed", e)
            dataStore.edit { it[LAST_CHECK_TIME] = now }
            null
        }
    }

    private fun fetchLatestRelease(): GitHubRelease {
        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection)
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"

        try {
            val responseCode = connection.responseCode
            if (responseCode != 200) throw Exception("HTTP $responseCode")

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            // The release payload has many fields we don't model, so unknown keys must be ignored.
            return JSON.decodeFromString<GitHubRelease>(body)
        } catch (e: SocketTimeoutException) {
            throw Exception("Timeout ao verificar atualizações", e)
        } finally {
            try {
                connection.disconnect()
            } catch (e: Exception) {
                // Ignore disconnect errors
            }
        }
    }

    private fun parseVersionCode(tag: String): Int {
        val version = tag.removePrefix("v")
        val parts = version.split(".")
        if (parts.size < 3) return 0

        return try {
            val major = parts[0].toInt()
            val minor = parts[1].toInt()
            val patch = parts[2].takeWhile { it.isDigit() }.toInt()
            major * 10_000 + minor * 100 + patch
        } catch (e: Exception) {
            0
        }
    }

    private fun serializeRemoteVersion(remote: RemoteVersion): String {
        return Json.encodeToString(RemoteVersion.serializer(), remote)
    }

    private fun parseRemoteVersion(json: String): RemoteVersion? {
        return try {
            Json.decodeFromString<RemoteVersion>(json)
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        val REMOTE_VERSION = stringPreferencesKey("remote_version")
        val LAST_CHECK_TIME = longPreferencesKey("last_check_time")
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
