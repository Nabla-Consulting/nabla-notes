package com.nabla.notes.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests for SettingsRepository folder stack persistence.
 *
 * DataStore needs its own TestScope that we cancel in teardown to avoid
 * UncompletedCoroutinesError from the internal background writer job.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

    // Dedicated scope for DataStore — cancelled in teardown.
    private val datastoreScope = TestScope(UnconfinedTestDispatcher())

    private fun buildRepo(): SettingsRepository {
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = datastoreScope,
            produceFile = { tmpFolder.newFile("prefs_${System.nanoTime()}.preferences_pb") }
        )
        return SettingsRepository(dataStore)
    }

    @After
    fun teardown() {
        datastoreScope.cancel()
    }

    @Test
    fun `saveLastFolderStack and loadLastFolderStack round-trip single entry`() = runTest {
        val repo = buildRepo()
        val stack = listOf(Pair("id1", "Projects"))
        repo.saveLastFolderStack(stack)
        assertEquals(stack, repo.loadLastFolderStack())
    }

    @Test
    fun `saveLastFolderStack and loadLastFolderStack round-trip multiple entries`() = runTest {
        val repo = buildRepo()
        val stack = listOf(Pair("abc123", "Projects"), Pair("def456", "2026"))
        repo.saveLastFolderStack(stack)
        assertEquals(stack, repo.loadLastFolderStack())
    }

    @Test
    fun `loadLastFolderStack returns empty list when nothing saved`() = runTest {
        val repo = buildRepo()
        assertTrue(repo.loadLastFolderStack().isEmpty())
    }

    @Test
    fun `saveLastFolderStack with empty list clears stored value`() = runTest {
        val repo = buildRepo()
        repo.saveLastFolderStack(listOf(Pair("id1", "Folder")))
        repo.saveLastFolderStack(emptyList())
        assertTrue(repo.loadLastFolderStack().isEmpty())
    }

    @Test
    fun `folder names with spaces survive round-trip`() = runTest {
        val repo = buildRepo()
        val stack = listOf(Pair("id1", "My Notes"), Pair("id2", "Work Projects"))
        repo.saveLastFolderStack(stack)
        assertEquals(stack, repo.loadLastFolderStack())
    }
}
