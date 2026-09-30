package com.johngabie.johnpdf.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

interface UpdateCheck {
    val remoteVersion: Flow<RemoteVersion?>
    suspend fun checkForUpdate()
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
    private val repoOwner: String = "JohnGabie",
    private val repoName: String = "johnPDF",
    private val checkIntervalMs: Long = 24 * 60 * 60 * 1000, // 24 hours
) : UpdateCheck {
    override val remoteVersion: Flow<RemoteVersion?> = dataStore.data.map { prefs ->
        prefs[REMOTE_VERSION]?.let { parseRemoteVersion(it) }
    }

    override suspend fun checkForUpdate() {
        val now = System.currentTimeMillis()
        var shouldCheck = false

        dataStore.data.map { it[LAST_CHECK_TIME] ?: 0L }.collect { lastCheck ->
            shouldCheck = (now - lastCheck >= checkIntervalMs)
        }

        if (!shouldCheck) return

        try {
            val release = fetchLatestRelease()
            val versionCode = parseVersionCode(release.tagName)
            val remote = RemoteVersion(
                versionCode = versionCode,
                versionName = release.tagName.removePrefix("v"),
                downloadUrl = "https://github.com/$repoOwner/$repoName/releases/tag/${release.tagName}",
            )
            dataStore.edit {
                it[REMOTE_VERSION] = serializeRemoteVersion(remote)
                it[LAST_CHECK_TIME] = now
            }
        } catch (e: Exception) {
            // Silently fail; network error or parse error
        }
    }

    private suspend fun fetchLatestRelease(): GitHubRelease {
        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val connection = (url.openConnection() as HttpURLConnection)
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"

        try {
            if (connection.responseCode != 200) throw Exception("HTTP ${connection.responseCode}")
            val json = connection.inputStream.bufferedReader().readText()
            return Json.decodeFromString<GitHubRelease>(json)
        } finally {
            connection.disconnect()
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
    }
}
