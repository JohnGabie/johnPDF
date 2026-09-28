package com.johngabie.johnpdf.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RecentsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val store get() = File(tmp.root, "recents.json")
    private fun repo(max: Int = MAX_RECENTS) = RecentsRepository(store, Dispatchers.Unconfined, max)
    private fun item(path: String, at: Long, imported: Boolean = false) =
        RecentItem(name = File(path).name, origin = Origin.OTHER, path = path, openedAt = at, imported = imported)

    @Test fun add_orders_newest_first() = runTest {
        val r = repo()
        r.add(item("/a.pdf", 1)); r.add(item("/b.pdf", 2))
        assertEquals(listOf("/b.pdf", "/a.pdf"), r.items.value.map { it.path })
    }

    @Test fun adding_same_path_moves_to_top_without_duplicate() = runTest {
        val r = repo()
        r.add(item("/a.pdf", 1)); r.add(item("/b.pdf", 2)); r.add(item("/a.pdf", 3))
        assertEquals(listOf("/a.pdf", "/b.pdf"), r.items.value.map { it.path })
    }

    @Test fun cap_evicts_oldest_and_deletes_its_imported_copy() = runTest {
        val r = repo(max = 2)
        val oldCopy = tmp.newFile("old.pdf")
        r.add(item(oldCopy.path, 1, imported = true)); r.add(item("/b.pdf", 2)); r.add(item("/c.pdf", 3))
        assertEquals(listOf("/c.pdf", "/b.pdf"), r.items.value.map { it.path })
        assertFalse(oldCopy.exists())
    }

    @Test fun evicting_non_imported_keeps_user_file() = runTest {
        val r = repo(max = 1)
        val userFile = tmp.newFile("user.pdf")
        r.add(item(userFile.path, 1)); r.add(item("/b.pdf", 2))
        assertTrue(userFile.exists())
    }

    @Test fun remove_deletes_imported_copy() = runTest {
        val r = repo()
        val copy = tmp.newFile("copy.pdf")
        r.add(item(copy.path, 1, imported = true))
        r.remove(copy.path)
        assertEquals(emptyList<RecentItem>(), r.items.value)
        assertFalse(copy.exists())
    }

    @Test fun persists_across_instances() = runTest {
        repo().add(item("/a.pdf", 1))
        val reloaded = repo().also { it.load() }
        assertEquals(listOf("/a.pdf"), reloaded.items.value.map { it.path })
    }

    @Test fun corrupted_store_loads_as_empty() = runTest {
        store.writeText("{isto não é json")
        val r = repo().also { it.load() }
        assertEquals(emptyList<RecentItem>(), r.items.value)
        r.add(item("/a.pdf", 1))
        assertEquals(1, r.items.value.size)
    }
}
