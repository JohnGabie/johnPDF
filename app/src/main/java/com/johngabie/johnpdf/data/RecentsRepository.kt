package com.johngabie.johnpdf.data

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val MAX_RECENTS = 20

@Serializable
data class RecentItem(
    val name: String,
    val origin: Origin,
    val path: String,
    val openedAt: Long,
    /** true = cópia em filesDir/imports, apagada quando o item sai da lista. */
    val imported: Boolean,
)

class RecentsRepository(
    private val storeFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val maxItems: Int = MAX_RECENTS,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<RecentItem>>(emptyList())
    val items: StateFlow<List<RecentItem>> = _items.asStateFlow()

    suspend fun load() = mutex.withLock { _items.value = read() }

    suspend fun add(item: RecentItem) = mutex.withLock {
        val updated = (listOf(item) + read().filterNot { it.path == item.path })
            .sortedByDescending { it.openedAt }
        updated.drop(maxItems).forEach { deleteCopy(it) }
        write(updated.take(maxItems))
    }

    suspend fun remove(path: String) = mutex.withLock {
        val current = read()
        current.filter { it.path == path }.forEach { deleteCopy(it) }
        write(current.filterNot { it.path == path })
    }

    private suspend fun read(): List<RecentItem> = withContext(ioDispatcher) {
        if (!storeFile.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString<List<RecentItem>>(storeFile.readText()) }.getOrDefault(emptyList())
    }

    private suspend fun write(list: List<RecentItem>) {
        withContext(ioDispatcher) {
            storeFile.parentFile?.mkdirs()
            val tmp = File(storeFile.path + ".tmp")
            tmp.writeText(json.encodeToString(list))
            if (!tmp.renameTo(storeFile)) {
                storeFile.writeText(tmp.readText())
                tmp.delete()
            }
        }
        _items.value = list
    }

    private suspend fun deleteCopy(item: RecentItem) {
        if (item.imported) withContext(ioDispatcher) { File(item.path).delete() }
    }
}
